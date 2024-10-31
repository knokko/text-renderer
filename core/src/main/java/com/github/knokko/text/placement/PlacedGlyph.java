package com.github.knokko.text.placement;

import com.github.knokko.text.SizedGlyph;

import java.util.Objects;

/**
 * The output of stage 1 (and input of stage 2) of the text rendering pipeline is a list of <i>PlacedGlyph</i>s. Each
 * placed glyph has a position, size, glyph, as well as some metadata.
 */
public class PlacedGlyph {

	/**
	 * The glyph, size, and font index
	 */
	public final SizedGlyph glyph;

	/**
	 * The position at which the glyph should be placed. It should be rendered between (minX, minY) and
	 * (minX + glyphWidth, minY + glyphHeight). Note that this class does <b>not</b> store the <i>glyphWidth</i>
	 * and <i>glyphHeight</i>.
	 */
	public int minX, minY;

	/**
	 * The <i>TextPlaceRequest</i> for which this glyph was placed. This information is normally not needed to render
	 * the glyph, but it can be used to propagate data from stage 1 to stage 3, which some users may need.
	 */
	public final TextPlaceRequest request;

	/**
	 * The index of the character for which this glyph was placed. You can get the original codepoint by using
	 * <i>request.text.codePointAt(charIndex)</i>. This information is usually not required, but it could be useful in
	 * stage 3 for some renderers.
	 */
	public final int charIndex;

	/**
	 * Constructs a new <i>PlacedGlyph</i>. This constructor is intended for internal usage only.
	 */
	public PlacedGlyph(SizedGlyph glyph, int minX, int minY, TextPlaceRequest request, int charIndex) {
		this.glyph = Objects.requireNonNull(glyph);
		this.minX = minX;
		this.minY = minY;
		this.request = request;
		this.charIndex = charIndex;
	}

	@Override
	public String toString() {
		return "PlacedGlyph(" + minX + ", " + minY + ", glyph=" + glyph + ")";
	}
}
