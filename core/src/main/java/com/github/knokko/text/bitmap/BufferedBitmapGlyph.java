package com.github.knokko.text.bitmap;

import java.util.Collections;
import java.util.List;

class BufferedBitmapGlyph {

	long lastUsed;
	final List<BitmapGlyphSection> sections;

	BufferedBitmapGlyph(List<BitmapGlyphSection> sections) {
		this.sections = Collections.unmodifiableList(sections);
	}

	@Override
	public String toString() {
		return sections.size() + " sections last used " + lastUsed;
	}
}
