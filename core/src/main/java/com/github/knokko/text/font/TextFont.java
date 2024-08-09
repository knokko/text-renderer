package com.github.knokko.text.font;

import com.github.knokko.text.bitmap.FreeTypeGlyphRasterizer;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_Size;

import java.nio.ByteBuffer;
import java.util.Objects;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.util.freetype.FreeType.*;

public class TextFont {

	private final FT_Face[] ftFaces;
	private final ByteBuffer[] fontBuffers;
	private final double[] relativeFontSizes;
	private final HeightSearcher heightSearcher = new HeightSearcher(5000, size -> {
		setSize(size);
		return getRawHeight(true);
	});
	private int maxHeight = 10_000;

	private int currentUnscaledFontSize;
	private int currentScaledFontSize;
	private int currentScale, currentHeightScale;
	public final FreeTypeGlyphRasterizer rasterizer;

	public TextFont(FT_Face[] ftFaces, ByteBuffer[] fontBuffers) {
		this.ftFaces = Objects.requireNonNull(ftFaces);
		this.fontBuffers = Objects.requireNonNull(fontBuffers);
		this.relativeFontSizes = new double[ftFaces.length];
		this.rasterizer = new FreeTypeGlyphRasterizer(ftFaces, this::setSize);
		this.estimateFontSizes();
	}

	public TextFont setMaxHeight(int newMaxHeight) {
		this.maxHeight = newMaxHeight;
		return this;
	}

	private void estimateFontSizes() {
		if (ftFaces.length == 0) throw new IllegalArgumentException("Fonts must have at least 1 face");
		if (ftFaces.length == 1) {
			relativeFontSizes[0] = 1.0;
			return;
		}

		int testSize = 20;

		long[] rawHeights = new long[ftFaces.length];
		long maxHeight = 0;
		for (int index = 0; index < ftFaces.length; index++) {
			var face = ftFaces[index];
			assertFtSuccess(FT_Set_Char_Size(
					face, 0, testSize * 64L, 0, 5 * testSize
			), "Set_Char_Size", "estimateFontSizes");
			assertFtSuccess(FT_Set_Pixel_Sizes(
					face, 0, testSize
			), "Set_Pixel_Sizes", "estimateFontSizes");

			FT_Load_Char(face, 'A', 0);
			var glyph = face.glyph();
			if (glyph == null) {
				throw new UnsupportedOperationException("Face " + index + " doesn't support the A character?");
			}
			rawHeights[index] = glyph.metrics().height();

			maxHeight = Math.max(maxHeight, rawHeights[index]);
		}

		for (int index = 0; index < ftFaces.length; index++) {
			relativeFontSizes[index] = (double) rawHeights[index] / (double) maxHeight;
		}
	}

	public FT_Face[] getFreeTypeFaces() {
		return ftFaces;
	}

	void setSize(int size) {
		if (currentScaledFontSize == size) return;
		if (size <= 0) throw new IllegalArgumentException("Size (" + size + ") must be positive");

		currentScale = 1;
		currentUnscaledFontSize = size;

		String context = "TextFont.setSize(" + size + ")";

		scaleLoop:
		while (true) {
			for (int index = 0; index < ftFaces.length; index++) {
				var face = ftFaces[index];
				int localSize = Math.toIntExact(Math.round(currentUnscaledFontSize / relativeFontSizes[index]));

				int charSizeResult = FT_Set_Char_Size(
						face, 0, localSize * 64L, 0, 5 * localSize
				);
				if (charSizeResult == FT_Err_Invalid_Pixel_Size) {
					currentScale += 1;
					currentUnscaledFontSize = size / currentScale;
					if (size % currentScale != 0) currentUnscaledFontSize += 1;
					continue scaleLoop;
				}

				assertFtSuccess(charSizeResult, "Set_Char_Size", context);
				assertFtSuccess(FT_Set_Pixel_Sizes(
						face, 0, localSize
				), "Set_Pixel_Sizes", context);
			}
			break;
		}

		currentScaledFontSize = size;
	}

	public int getSize(boolean scaled) {
		if (scaled) return currentScaledFontSize;
		else return currentUnscaledFontSize;
	}

	public int getScale() {
		return currentScale * currentHeightScale;
	}

	private int getRawHeight(boolean scaled) {
		long maxAscent = 0;
		long minDescent = 0;
		long maxHeight = 0;

		for (var face : ftFaces) {
			@SuppressWarnings("resource") FT_Size size = face.size();
			if (size == null) continue;

			maxAscent = Math.max(maxAscent, size.metrics().ascender());
			minDescent = Math.min(minDescent, size.metrics().descender());
			maxHeight = Math.max(maxHeight, size.metrics().ascender() - size.metrics().descender());
		}

		if (scaled) maxHeight *= currentScale;
		return Math.toIntExact(maxHeight);
	}

	/**
	 * @return The vertical distance between two lines of text, in pixels
	 */
	public int getHeight(boolean scaled) {
		return getRawHeight(scaled) / 64;
	}

	/**
	 * Sets the font height (the vertical distance between two lines of text), in pixels
	 */
	public void setHeight(int height) {
		int originalHeight = height;
		currentHeightScale = 1;
		while (height > maxHeight) {
			currentHeightScale += 1;
			height = originalHeight / currentHeightScale;
		}

		int desiredRawHeight = height * 64;
		int size = heightSearcher.search(desiredRawHeight, height, 3, 10 * height);
		setSize(size);
	}

	public void destroy() {
		for (FT_Face face : ftFaces) FT_Done_Face(face);
		for (ByteBuffer buffer : fontBuffers) memFree(buffer);
	}
}
