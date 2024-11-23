package com.github.knokko.text.bitmap;

import com.github.knokko.text.SizedGlyph;

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
	 * Rasterizes the given glyph with the given size, and optional user data. After this method returns, you
	 * can use <i>getBufferWidth</i>, <i>getBufferHeight</i> to retrieve the dimensions of the glyph, and
	 * <i>getBuffer</i> to get the rasterized glyph
	 */
	void set(SizedGlyph glyph, Object userData);

	/**
	 * Creates a key for the given {@code userData}. Given a {@link SizedGlyph} {@code g} and two objects {@code a} and
	 * {@code b}, {@code getUserData(g, a)} should be equal to {@code getUserData(g, b)} if and only if
	 * {@code set(g, a)} would have the same effect as {@code set(g, b)}.
	 *
	 * <p>
	 *     If this rasterizer ignores all user data, it can always return the empty string (which would satisfy the
	 *     contract above).
	 * </p>
	 * @param userData The request user data
	 * @return The key
	 */
	String getUserDataKey(Object userData);

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
