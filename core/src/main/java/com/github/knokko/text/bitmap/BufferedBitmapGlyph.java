package com.github.knokko.text.bitmap;

import com.github.knokko.text.SizedGlyph;

import java.util.Collections;
import java.util.List;

class BufferedBitmapGlyph implements Comparable<BufferedBitmapGlyph> {

	final SizedGlyph glyph;
	long lastUsed;
	final List<BitmapGlyphSection> sections;

	BufferedBitmapGlyph(SizedGlyph glyph, List<BitmapGlyphSection> sections, long currentFrame) {
		this.glyph = glyph;
		this.sections = Collections.unmodifiableList(sections);
		this.lastUsed = currentFrame;
	}

	@Override
	public String toString() {
		return sections.size() + " sections last used " + lastUsed;
	}

	@Override
	public int compareTo(BufferedBitmapGlyph other) {
		if (this.lastUsed > other.lastUsed) return 1;
		if (this.lastUsed < other.lastUsed) return -1;
		if (this.glyph.id > other.glyph.id) return 1;
		if (this.glyph.id < other.glyph.id) return -1;
		if (this.glyph.faceIndex > other.glyph.faceIndex) return 1;
		if (this.glyph.faceIndex < other.glyph.faceIndex) return -1;
		if (this.glyph.size > other.glyph.size) return 1;
		if (this.glyph.size < other.glyph.size) return -1;
		return Integer.compare(this.glyph.scale, other.glyph.scale);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof BufferedBitmapGlyph && this.glyph.equals(((BufferedBitmapGlyph) other).glyph);
	}

	@Override
	public int hashCode() {
		return glyph.hashCode();
	}
}
