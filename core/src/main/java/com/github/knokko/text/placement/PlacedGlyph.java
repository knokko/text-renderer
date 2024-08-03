package com.github.knokko.text.placement;

import com.github.knokko.text.SizedGlyph;

import java.util.Objects;

public class PlacedGlyph {

	public final SizedGlyph glyph;
	public final int minX, minY;
	public final TextPlaceRequest request;
	public final int charIndex;

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
