package com.github.knokko.text.font;

import com.github.knokko.text.TextInstance;

import java.util.*;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.freetype.FreeType.FT_LOAD_BITMAP_METRICS_ONLY;
import static org.lwjgl.util.freetype.FreeType.FT_Load_Char;

/**
 * Represents an ordered list of at least 1 font, where the first font is the primary/preferred font, and all other
 * fonts are <i>fallback</i> fonts that will be used when none of the previous fonts is able to render a glyph. You
 * should create 1 instance of {@link FontData} for each font that you want to use. This class is thread-safe.
 */
public class FontData {

	private final TextInstance textInstance;
	private final FreeTypeFaceSource[] faceSources;
	private final HeightSearcher[] heightSearchers;
	private int maxHeight = 100;
	private final Map<TextFaceKey, TextFaceList> faceCache = new HashMap<>();

	private long totalBorrowCounter;
	private long openFaceCounter;
	private long openBorrowCounter;

	/**
	 * Constructs a new instance of {@link FontData}.
	 * @param textInstance The {@link TextInstance}
	 * @param fonts The ordered list of {@link FontSource}s, where the first source is the primary (preferred) font.
	 *              All fonts to which the font sources point, will be loaded.
	 */
	public FontData(TextInstance textInstance, FontSource... fonts) {
		this.textInstance = textInstance;

		FreeTypeFaceSource[][] sources = new FreeTypeFaceSource[fonts.length][];
		int numFreeTypeFonts = 0;
		for (int index = 0; index < fonts.length; index++) {
			sources[index] = fonts[index].loadData();
			numFreeTypeFonts += sources[index].length;
		}

		this.faceSources = new FreeTypeFaceSource[numFreeTypeFonts];
		int index = 0;
		for (var fontSources : sources) {
			System.arraycopy(fontSources, 0, this.faceSources, index, fontSources.length);
			index += fontSources.length;
		}

		this.heightSearchers = new HeightSearcher[faceSources.length];
		for (index = 0; index < faceSources.length; index++) {
			int faceIndex = index;
			this.heightSearchers[faceIndex] = new HeightSearcher(100, size -> {
				var face = this.borrowFaceWithSize(faceIndex, size, 1);

				assertFtSuccess(FT_Load_Char(
						face.ftFace, 'A', FT_LOAD_BITMAP_METRICS_ONLY
				), "Load_Char", "A size");
				int height = Math.toIntExact(Objects.requireNonNull(face.ftFace.glyph()).metrics().height());

				this.returnFace(face);
				return height;
			});
		}
	}

	/**
	 * Sets the maximum height of this {@link FontData}. Whenever a
	 * {@link com.github.knokko.text.placement.TextPlaceRequest} asks for a higher {@code heightA}, the glyph will be
	 * <i>upscaled</i> to artificially get that height, sparing memory. This method must be called <b>before</b> using
	 * this <i>FontData</i>
	 */
	public void setMaxHeight(int maxHeight) {
		if (!faceCache.isEmpty()) throw new IllegalStateException("You must call this method BEFORE using this font");
		this.maxHeight = maxHeight;
	}

	/**
	 * @return The number of fonts/faces in this <i>FontData</i>
	 */
	public int getNumFaces() {
		return faceSources.length;
	}

	/**
	 * Borrows the face/font with index {@code faceIndex} from this {@link FontData}, with the given {@code height}.
	 * Note that this method is intended for internal use, so you should probably not need to call this yourself. But
	 * if you do, make sure that you also call {@link #returnFace(TextFace)}.
	 *
	 * @param faceIndex The index of the face/font to borrow, must be at least 0 and smaller than {@link #getNumFaces()}
	 * @param height The desired height of the (uppercase) 'A' character, in pixels
	 * @param minScale The minimum {@link com.github.knokko.text.SizedGlyph#scale}
	 * @return The borrowed font/face
	 */
	public TextFace borrowFaceWithHeightA(int faceIndex, int height, int minScale) {
		int originalHeight = height;
		int heightScale = 1;
		while (height > maxHeight || heightScale < minScale) {
			heightScale += 1;
			height = originalHeight / heightScale;
		}

		int desiredRawHeight = height * 64;

		int size = heightSearchers[faceIndex].search(desiredRawHeight, height * 64, 3, 640 * height);

		return borrowFaceWithSize(faceIndex, size, heightScale);
	}

	/**
	 * Borrows the face/font with index {@code faceIndex} from this {@link FontData}, with the given {@code size}
	 * and {@code scale}.
	 * Note that this method is intended for internal use, so you should probably not need to call this yourself. But
	 * if you do, make sure that you also call {@link #returnFace(TextFace)}.
	 *
	 * @param faceIndex The index of the face/font to borrow, must be at least 0 and smaller than {@link #getNumFaces()}
	 * @param size The raw size of the font to be borrowed, which will be passed to
	 * {@link org.lwjgl.util.freetype.FreeType#FT_Set_Char_Size}.
	 * @return The borrowed font/face
	 */
	public TextFace borrowFaceWithSize(int faceIndex, int size, int scale) {
		// Performance measurements: creating a FT_Face takes 10 to 40 microseconds, and allocates 10 to 30 KB
		// Resizing an existing FT_Face takes 1 to 15 microseconds
		synchronized (faceCache) {
			totalBorrowCounter += 1;

			var key = new TextFaceKey(faceIndex, size, scale);
			var faceList = faceCache.computeIfAbsent(key, k -> new TextFaceList());
			faceList.lastUsed = totalBorrowCounter;
			faceList.borrowCounter += 1;
			openBorrowCounter += 1;

			if (openBorrowCounter > 50) throw new Error("Uh ooh");

			if (!faceList.faces.isEmpty()) {
				return faceList.faces.remove(faceList.faces.size() - 1);
			}
			try (var stack = stackPush()) {
				var ftFace = textInstance.createFreeTypeFace(faceSources[faceIndex], stack);
				openFaceCounter += 1;

				if (openFaceCounter > 100) {
					long oldestLastUsed = totalBorrowCounter;

					for (var value : faceCache.values()) {
						if (value.lastUsed < oldestLastUsed && !value.faces.isEmpty()) oldestLastUsed = value.lastUsed;
					}

					for (var value : faceCache.values()) {
						if (value.lastUsed == oldestLastUsed) {
							openFaceCounter -= value.faces.size();
							for (var face : value.faces) face.destroy();
							value.faces.clear();
						}
					}

					faceCache.values().removeIf(value -> value.faces.isEmpty() && value.borrowCounter == 0);
				}
				return new TextFace(ftFace, size, scale, key);
			}
		}
	}

	/**
	 * Returns a font/face that was previously borrowed using {@link #borrowFaceWithHeightA} or
	 * {@link #borrowFaceWithSize}.
	 * @param face The borrowed face/font
	 */
	public void returnFace(TextFace face) {
		synchronized (faceCache) {
			var faceList = faceCache.get(face.key);
			faceList.faces.add(face);
			faceList.borrowCounter -= 1;
			openBorrowCounter -= 1;
		}
	}

	/**
	 * Destroys this {@link FontData}. You should use this once you no longer need it.
	 */
	public void destroy() {
		synchronized (textInstance) {
			for (var faceList : faceCache.values()) {
				for (var face : faceList.faces) face.destroy();
			}
			faceCache.clear();
		}
		for (var source : faceSources) source.destroy();
	}

	record TextFaceKey(int faceIndex, int size, int heightScale) {}

	static class TextFaceList {

		final List<TextFace> faces = new ArrayList<>();
		long lastUsed = 0;
		long borrowCounter = 0;

		@Override
		public String toString() {
			return "TextFaceList(lastUsed=" + lastUsed + ",borrowCounter=" + borrowCounter + "," + faces + ")";
		}
	}
}
