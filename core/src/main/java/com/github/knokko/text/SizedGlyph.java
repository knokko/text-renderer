package com.github.knokko.text;

public class SizedGlyph {

	public final int id;
	public final int faceIndex;
	public final int size;

	public SizedGlyph(int id, int faceIndex, int size) {
		this.id = id;
		this.faceIndex = faceIndex;
		this.size = size;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof SizedGlyph) {
			SizedGlyph otherGlyph = (SizedGlyph) other;
			return this.id == otherGlyph.id && this.faceIndex == otherGlyph.faceIndex && this.size == otherGlyph.size;
		} else return false;
	}

	@Override
	public int hashCode() {
		return id ^ faceIndex ^ size;
	}

	@Override
	public String toString() {
		return "SizedGlyph(" + id + "[" + faceIndex + "] * " + size + ")";
	}
}
