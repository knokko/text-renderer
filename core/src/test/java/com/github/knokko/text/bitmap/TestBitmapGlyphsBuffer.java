package com.github.knokko.text.bitmap;

import com.github.knokko.text.SizedGlyph;
import com.github.knokko.text.bitmap.BitmapGlyphsBuffer;
import com.github.knokko.text.bitmap.GlyphRasterizer;
import com.github.knokko.text.placement.PlacedGlyph;
import com.github.knokko.text.placement.TextPlaceRequest;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.system.MemoryUtil.*;

public class TestBitmapGlyphsBuffer {

	private static class DummyRasterizer implements GlyphRasterizer {

		private ByteBuffer buffer;
		private int size;

		@Override
		public void set(int glyph, int faceIndex, int size) {
			int capacity = 2 * size * size;
			buffer = BufferUtils.createByteBuffer(capacity);
			for (int counter = 0; counter < capacity; counter++) {
				buffer.put((byte) (counter + 1));
			}
			buffer.position(0);
			this.size = size;
		}

		@Override
		public int getBufferWidth() {
			return size;
		}

		@Override
		public int getBufferHeight() {
			return 2 * size;
		}

		@Override
		public ByteBuffer getBuffer() {
			return buffer;
		}
	}

	@Test
	public void testSingleCharacter() {
		var byteBuffer = memCalloc(1000);
		var glyphsBuffer = new BitmapGlyphsBuffer(memAddress(byteBuffer), byteBuffer.capacity(), 60);
		glyphsBuffer.startFrame();

		int size = 10;
		var result = glyphsBuffer.getSections(new DummyRasterizer(), new SizedGlyph(1234, 0, size));

		int totalArea = 0;
		for (var section : result) {
			totalArea += section.width() * section.height();
		}
		assertEquals(2 * size * size, totalArea);

		assertTrue(result.size() >= 4, "Size (" + result.size() + ") must be at least 4");
		assertTrue(result.size() <= 6, "Size (" + result.size() + ") should be at most 6");

		for (var section : result) {
			for (int x = 0; x < section.width(); x++) {
				for (int y = 0; y < section.height(); y++) {
					int glyphX = x + section.offsetX();
					int glyphY = y + section.offsetY();
					byte glyphValue = (byte) (1 + glyphX + glyphY * size);

					int bufferIndex = section.bufferIndex() + x + y * section.width();
					byte bufferValue = byteBuffer.get(bufferIndex);

					assertEquals(glyphValue, bufferValue);
				}
			}
		}

		for (int index = 60 * result.size(); index < byteBuffer.capacity(); index++) {
			assertEquals(0, byteBuffer.get(index));
		}
	}

	@Test
	public void testEfficientMemoryUsage() {
		var placeRequest = new TextPlaceRequest("hello", 12, 34, 56, 78, null);

		var glyph1 = new SizedGlyph(12, 0, 15);
		var glyph2 = new SizedGlyph(13, 0, 15);

		int requiredSize = 2 * glyph1.size * glyph1.size + 2 * glyph2.size * glyph2.size;

		int size = requiredSize + requiredSize / 4;
		long address = nmemAlloc(size);
		var glyphsBuffer = new BitmapGlyphsBuffer(address, size);
		var rasterizer = new DummyRasterizer();

		var dummyGlyphs = new ArrayList<PlacedGlyph>();
		dummyGlyphs.add(new PlacedGlyph(glyph1, 123, 456, placeRequest, 0));
		dummyGlyphs.add(new PlacedGlyph(glyph2, 43, 75, placeRequest, 1));
		dummyGlyphs.add(new PlacedGlyph(glyph1, 6, 1, placeRequest, 2));

		int initialUsedSpace = glyphsBuffer.getUsedSpace();
		assertEquals(size - initialUsedSpace, glyphsBuffer.countAvailableSpace());
		assertTrue(initialUsedSpace < 120, "initial used space is not smaller than slot size: " + initialUsedSpace);
		glyphsBuffer.startFrame();
		glyphsBuffer.bufferGlyphs(rasterizer, dummyGlyphs);

		assertTrue(glyphsBuffer.getUsedSpace() > 800);

		glyphsBuffer.startFrame();
		assertEquals(size - initialUsedSpace, glyphsBuffer.countAvailableSpace());

		nmemFree(address);
	}
}
