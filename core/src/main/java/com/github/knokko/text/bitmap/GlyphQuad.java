package com.github.knokko.text.bitmap;

public class GlyphQuad {

	public final int bufferIndex;
	public final int minX, minY, maxX, maxY;
	public final int sectionWidth, bufferOffsetX;
	public final int charIndex;
	public final Object userData;

	public GlyphQuad(
			int bufferIndex, int minX, int minY, int maxX, int maxY,
			int sectionWidth, int bufferOffsetX, int charIndex, Object userData
	) {
		this.bufferIndex = bufferIndex;
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		this.sectionWidth = sectionWidth;
		this.bufferOffsetX = bufferOffsetX;
		this.charIndex = charIndex;
		this.userData = userData;
	}

	@Override
	public String toString() {
		return "GlyphQuad(" + minX + "," + minY + "," + maxX + "," + maxY + ")";
	}

	public int getActualWidth() {
		return 1 + maxX - minX;
	}

	public int getHeight() {
		return 1 + maxY - minY;
	}
}
