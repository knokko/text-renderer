package com.github.knokko.text.placement;

import com.github.knokko.text.SizedGlyph;

import java.util.Objects;

public class PlacedGlyph {

	public final SizedGlyph glyph;
	public int minX, minY;
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
	public boolean equals(Object other) {
		if (other instanceof PlacedGlyph) {
			PlacedGlyph pg = (PlacedGlyph) other;
			return this.glyph.equals(pg.glyph) && this.minX == pg.minX && this.minY == pg.minY &&
					this.request == pg.request && this.charIndex == pg.charIndex;
		} else return false;
	}

	@Override
	public String toString() {
		return "PlacedGlyph(" + minX + ", " + minY + ", glyph=" + glyph + ")";
	}
}
