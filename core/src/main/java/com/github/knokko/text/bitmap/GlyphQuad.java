package com.github.knokko.text.bitmap;

import com.github.knokko.text.placement.TextPlaceRequest;

/**
 * The output of stage 2 of the text rendering pipeline (and the input of stage 3), is a list of <i>GlyphQuad</i>s. A
 * <i>GlyphQuad</i> is a quad with a position that should render( a part of) a glyph. This class contains the
 * coordinates of the quad, and the index into the glyphs buffer that contains the rasterized glyph data.
 */
public class GlyphQuad {

	/**
	 * The first index into the glyphs buffer that contains the rasterized glyph section to be drawn in this quad,
	 * in bytes.
	 */
	public final int bufferIndex;

	/**
	 * The coordinates of the quad to be rendered, in pixels.
	 */
	public final int minX, minY, maxX, maxY;

	/**
	 * The scale at which the rasterized glyph should be rendered, which is usually 1.
	 * <ul>
	 *     <li>When the scale is 1, the glyphs buffer contains 1 byte (value) per pixel in the quad.</li>
	 *     <li>When the scale is N, the glyphs buffer contains 1 byte (value) per N by N pixel block in this quad.</li>
	 * </ul>
	 */
	public final int scale;

	/**
	 * The width of the rasterized glyph section, in pixels
	 */
	public final int sectionWidth;

	/**
	 * The index into the {@link TextPlaceRequest#text} of the character that is (partially) being rendered by this
	 * quad. The exact character can be retrieved by invoking the <i>codePointAt</i> method of the text.
	 */
	public final int charIndex;

	/**
	 * The corresponding user data that has been propagated from stage 1
	 */
	public final Object userData;

	/**
	 * Constructs a new <i>GlyphQuad</i>. Note that you should usually not use this constructor yourself. Instead, let
	 * the <i>BitmapGlyphsBuffer</i> do it.
	 * @param bufferIndex {@link #bufferIndex}
	 * @param minX {@link #minX}
	 * @param minY {@link #minY}
	 * @param maxX {@link #maxX}
	 * @param maxY {@link #maxY}
	 * @param scale {@link #scale}
	 * @param sectionWidth {@link #sectionWidth}
	 * @param charIndex {@link #charIndex}
	 * @param userData {@link #userData}
	 */
	public GlyphQuad(
			int bufferIndex, int minX, int minY, int maxX, int maxY, int scale,
			int sectionWidth, int charIndex, Object userData
	) {
		this.bufferIndex = bufferIndex;
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		this.scale = scale;
		this.sectionWidth = sectionWidth;
		this.charIndex = charIndex;
		this.userData = userData;
	}

	@Override
	public String toString() {
		return "GlyphQuad(" + minX + "," + minY + "," + maxX + "," + maxY + ")";
	}

	/**
	 * @return the width of the quad to be rendered, in pixels
	 */
	public int getWidth() {
		return 1 + maxX - minX;
	}

	/**
	 * @return the height of the quad to be rendered, in pixels
	 */
	public int getHeight() {
		return 1 + maxY - minY;
	}
}
