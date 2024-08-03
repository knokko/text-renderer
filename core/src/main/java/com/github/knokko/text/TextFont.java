package com.github.knokko.text;

import com.github.knokko.text.bitmap.FreeTypeGlyphRasterizer;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_Size;

import java.nio.ByteBuffer;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.util.freetype.FreeType.*;

public class TextFont {

	private final FT_Face ftFace;
	private final ByteBuffer fontBuffer;
	public final FreeTypeGlyphRasterizer rasterizer;
	private int currentSize;

	private final HeightSearcher heightSearcher = new HeightSearcher(5000, size -> {
		setSize(size, false);
		return getRawHeight();
	});

	public TextFont(FT_Face ftFace, ByteBuffer fontBuffer) {
		this.ftFace = ftFace;
		this.fontBuffer = fontBuffer;
		this.rasterizer = new FreeTypeGlyphRasterizer(ftFace, this::setSize);
	}

	public FT_Face getFreeTypeFace() {
		return ftFace;
	}

	/**
	 * Sets the font 'size', in pixels
	 */
	public void setSize(int size) {
		setSize(size, true);
	}

	private void setSize(int size, boolean shouldUpdatePlacer) {
		if (size <= 0) throw new IllegalArgumentException("Size (" + size + ") must be positive");

		String context = "TextFont.setSize(" + size + ")";
		assertFtSuccess(FT_Set_Char_Size(
				ftFace, 0, size * 64L, 0, 5 * size
		), "Set_Char_Size", context);
		assertFtSuccess(FT_Set_Pixel_Sizes(
				ftFace, 0, size
		), "Set_Pixel_Sizes", context);

		this.currentSize = size;
	}

	public int getSize() {
		return currentSize;
	}

	public int getRawHeight() {
		@SuppressWarnings("resource")
		FT_Size size = ftFace.size();
		if (size == null) return 0;

		return Math.toIntExact(size.metrics().height());
	}

	/**
	 * @return The vertical distance between two lines of text, in pixels
	 */
	public int getHeight() {
		return getRawHeight() / 64;
	}

	/**
	 * Sets the font height (the vertical distance between two lines of text), in pixels
	 */
	public void setHeight(int height) {
		int desiredRawHeight = height * 64;
		int size = heightSearcher.search(desiredRawHeight, height, 3, 10 * height);
		setSize(size);
	}

	public void destroy() {
		FT_Done_Face(ftFace);
		if (fontBuffer != null) memFree(fontBuffer);
	}
}
