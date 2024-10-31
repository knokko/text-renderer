package com.github.knokko.text;

/**
 * A simple tuple consisting of a glyph ID, font/face index, size, and scale
 */
public class SizedGlyph {

	/**
	 * The glyph ID of this glyph, which should be propagated to FreeType
	 */
	public final int id;

	/**
	 * The font/face index of this glyph. This is usually 0, but it can be larger if your <i>FontData</i> has
	 * fallback fonts, and the first font is not capable of rendering this glyph.
	 */
	public final int faceIndex;

	/**
	 * The size at which this glyph should be rendered. This value should be passed to <i>FT_Set_Char_Size</i>
	 */
	public final int size;

	/**
	 * The scale at which this glyph should be rendered, not to be confused with the <i>size</i>. The scale is usually
	 * 1, but it may be larger when the text to be rendered is very large.
	 * <ul>
	 *     <li>
	 *         When the scale is 1, the rendered area of this glyph should be equally large as the size of the
	 *         rasterized glyph.
	 *     </li>
	 *     <li>
	 *         When the scale is N, the rendered area of this glyph should be N times as wide and N times as high
	 *         as the rasterized glyph: the glyph will be upscaled. This is useful to spare memory when the
	 *         text/glyph is very large.
	 *     </li>
	 * </ul>
	 */
	public final int scale;

	/**
	 * Constructs a new <i>SizedGlyph</i>. This constructor is intended for internal use.
	 */
	public SizedGlyph(int id, int faceIndex, int size, int scale) {
		this.id = id;
		this.faceIndex = faceIndex;
		this.size = size;
		this.scale = scale;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof SizedGlyph) {
			SizedGlyph otherGlyph = (SizedGlyph) other;
			return this.id == otherGlyph.id && this.faceIndex == otherGlyph.faceIndex &&
					this.size == otherGlyph.size && this.scale == otherGlyph.scale;
		} else return false;
	}

	@Override
	public int hashCode() {
		return id ^ faceIndex ^ size ^ scale;
	}

	@Override
	public String toString() {
		return "SizedGlyph(" + id + "[" + faceIndex + "] * " + size + " * " + scale + ")";
	}
}
