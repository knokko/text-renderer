package com.github.knokko.text;

public class SizedGlyph {

	public final int id;
	public final int size;

	public SizedGlyph(int id, int size) {
		this.id = id;
		this.size = size;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof SizedGlyph) {
			SizedGlyph otherGlyph = (SizedGlyph) other;
			return this.id == otherGlyph.id && this.size == otherGlyph.size;
		} else return false;
	}

	@Override
	public int hashCode() {
		return id ^ size;
	}

	@Override
	public String toString() {
		return "SizedGlyph(" + id + " * " + size + ")";
	}
}
