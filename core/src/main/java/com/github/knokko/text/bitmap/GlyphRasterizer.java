package com.github.knokko.text.bitmap;

import java.nio.ByteBuffer;

public interface GlyphRasterizer {

	void set(int glyph, int faceIndex, int size);

	int getBufferWidth();

	int getBufferHeight();

	ByteBuffer getBuffer();
}
