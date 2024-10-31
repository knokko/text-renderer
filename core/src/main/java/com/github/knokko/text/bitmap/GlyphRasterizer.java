package com.github.knokko.text.bitmap;

import java.nio.ByteBuffer;

/**
 * Implementations of <i>GlyphRasterizer</i> <b>rasterize</b> glyphs: given a glyph, font, and size, they create a
 * <i>ByteBuffer</i> containing the intensities of all the pixels. Furthermore, it determines the width and height of
 * each glyph.
 * <p>
 *     The most important implementation of this interface is <i>FreeTypeGlyphRasterizer</i>, which is probably the
 *     only implementation you will ever need. This library does not expose any other implementation.
 * </p>
 */
public interface GlyphRasterizer {

	/**
	 * Rasterizes the given glyph with the given size, and the given face/font index. After this method returns, you
	 * can use <i>getBufferWidth</i>, <i>getBufferHeight</i> to retrieve the dimensions of the glyph, and
	 * <i>getBuffer</i> to get the rasterized glyph
	 */
	void set(int glyph, int faceIndex, int size);

	/**
	 * Gets the width of the last glyph that was rasterized, in pixels
	 */
	int getBufferWidth();

	/**
	 * Gets the height of the last glyph that was rasterized, in pixels
	 */
	int getBufferHeight();

	/**
	 * Gets the rasterized glyph data. The intensity value (byte) of the pixel at (x, y) is stored at index
	 * {@code x + y * getBufferWidth()}. Note that the buffer data may be overwritten during the next call to
	 * {@link #set}
	 */
	ByteBuffer getBuffer();

	/**
	 * Destroys this rasterizer. You are supposed to call this method when you no longer need this rasterizer.
	 */
	void destroy();
}
