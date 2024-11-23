package com.github.knokko.text.bitmap;

import com.github.knokko.text.SizedGlyph;

import java.util.List;

class BufferedBitmapGlyph implements Comparable<BufferedBitmapGlyph> {

	final SizedGlyph glyph;
	final String userData;
	long lastUsed;
	final List<BitmapGlyphSection> sections;

	BufferedBitmapGlyph(SizedGlyph glyph, String userData, List<BitmapGlyphSection> sections, long currentFrame) {
		this.glyph = glyph;
		this.userData = userData;
		this.sections = sections;
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
		if (this.glyph.scale > other.glyph.scale) return 1;
		if (this.glyph.scale < other.glyph.scale) return -1;
		return this.userData.compareTo(other.userData);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof BufferedBitmapGlyph) {
			BufferedBitmapGlyph bbg = (BufferedBitmapGlyph) other;
			return this.glyph.equals(bbg.glyph) && this.userData.equals(bbg.userData);
		} else return false;
	}

	@Override
	public int hashCode() {
		return glyph.hashCode() ^ userData.hashCode();
	}
}
