package com.github.knokko.text.font;

import com.github.knokko.text.TextInstance;

import java.util.*;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.freetype.FreeType.FT_LOAD_BITMAP_METRICS_ONLY;
import static org.lwjgl.util.freetype.FreeType.FT_Load_Char;

public class FontData {

	private final TextInstance textInstance;
	private final FreeTypeFaceSource[] faceSources;
	private final HeightSearcher[] heightSearchers;
	private int maxHeight = 100;
	private final Map<TextFaceKey, TextFaceList> faceCache = new HashMap<>();

	private long totalBorrowCounter;
	private long openFaceCounter;
	private long openBorrowCounter;

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
			this.heightSearchers[faceIndex] = new HeightSearcher(50_000, size -> {
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

	public void setMaxHeight(int maxHeight) {
		if (!faceCache.isEmpty()) throw new IllegalStateException("You must call this method BEFORE using this font");
		this.maxHeight = maxHeight;
	}

	public int getNumFaces() {
		return faceSources.length;
	}

	public TextFace borrowFaceWithHeightA(int faceIndex, int height) {
		int originalHeight = height;
		int heightScale = 1;
		while (height > maxHeight) {
			heightScale += 1;
			height = originalHeight / heightScale;
		}

		int desiredRawHeight = height * 64;

		int size = heightSearchers[faceIndex].search(desiredRawHeight, height * 64, 3, 640 * height);

		return borrowFaceWithSize(faceIndex, size, heightScale);
	}

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

	public void returnFace(TextFace face) {
		synchronized (faceCache) {
			var faceList = faceCache.get(face.key);
			faceList.faces.add(face);
			faceList.borrowCounter -= 1;
			openBorrowCounter -= 1;
		}
	}

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
