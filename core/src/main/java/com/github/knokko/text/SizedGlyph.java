package com.github.knokko.text;

public class SizedGlyph {

	public final int id;
	public final int faceIndex;
	public final int size;
	public final int scale;

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
