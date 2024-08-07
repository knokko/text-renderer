package com.github.knokko.text.bitmap;

import com.github.knokko.text.SizedGlyph;
import com.github.knokko.text.placement.PlacedGlyph;
import com.github.knokko.text.placement.TextPlaceRequest;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.libc.LibCStdlib.nmalloc;

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
		var placeRequest = new TextPlaceRequest(
				"hello", 12, 34, 56, 78, true, null
		);

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

	@Test
	public void testEnforceBoundsY() {
		int bufferSize = 1000;
		long bufferAddress = nmalloc(bufferSize);
		var glyphs = new BitmapGlyphsBuffer(bufferAddress, bufferSize);

		var placeRequest = new TextPlaceRequest("h", 5, 6, 20, 35, true, null);

		var placedGlyphs = new ArrayList<PlacedGlyph>();
		placedGlyphs.add(new PlacedGlyph(new SizedGlyph(123, 0, 20), 2, 1, placeRequest, 0));

		var quads = glyphs.bufferGlyphs(new DummyRasterizer(), placedGlyphs);

		byte[][] resultMap = new byte[16][30];

		for (var quad : quads) {
			for (int offsetY = 0; offsetY < quad.getHeight(); offsetY++) {
				for (int offsetX = 0; offsetX < quad.getActualWidth(); offsetX++) {

					int imageX = offsetX + quad.minX;
					int imageY = offsetY + quad.minY;
					int bufferIndex = quad.bufferIndex + offsetX + quad.bufferOffsetX + offsetY * quad.sectionWidth;

					resultMap[imageX - placeRequest.minX][imageY - placeRequest.minY] = memGetByte(bufferAddress + bufferIndex);
				}
			}
		}

		assertEquals(104, resultMap[0][0]);
		assertEquals(119, resultMap[15][0]);

		assertEquals(124, resultMap[0][1]);
		assertEquals((byte) 139, resultMap[15][1]);

		assertEquals((byte) 144, resultMap[0][2]);
		assertEquals((byte) 159, resultMap[15][2]);

		assertEquals((byte) 684, resultMap[0][29]);
		assertEquals((byte) 699, resultMap[15][29]);
	}

	@Test
	public void testDoNotEnforceBoundsY() {
		int bufferSize = 1000;
		long bufferAddress = nmalloc(bufferSize);
		var glyphs = new BitmapGlyphsBuffer(bufferAddress, bufferSize);

		var placeRequest = new TextPlaceRequest("h", 5, 6, 20, 35, false, null);

		var placedGlyphs = new ArrayList<PlacedGlyph>();
		placedGlyphs.add(new PlacedGlyph(new SizedGlyph(123, 0, 20), 2, 1, placeRequest, 0));

		var quads = glyphs.bufferGlyphs(new DummyRasterizer(), placedGlyphs);

		byte[][] resultMap = new byte[16][40];

		for (var quad : quads) {
			for (int offsetY = 0; offsetY < quad.getHeight(); offsetY++) {
				for (int offsetX = 0; offsetX < quad.getActualWidth(); offsetX++) {

					int imageX = offsetX + quad.minX;
					int imageY = offsetY + quad.minY;
					int bufferIndex = quad.bufferIndex + offsetX + quad.bufferOffsetX + offsetY * quad.sectionWidth;

					resultMap[imageX - placeRequest.minX][imageY - 1] = memGetByte(bufferAddress + bufferIndex);
				}
			}
		}

		assertEquals(4, resultMap[0][0]);
		assertEquals(19, resultMap[15][0]);

		assertEquals(24, resultMap[0][1]);
		assertEquals(39, resultMap[15][1]);

		assertEquals(44, resultMap[0][2]);
		assertEquals(59, resultMap[15][2]);

		assertEquals((byte) 584, resultMap[0][29]);
		assertEquals((byte) 599, resultMap[15][29]);

		assertEquals((byte) 784, resultMap[0][39]);
		assertEquals((byte) 799, resultMap[15][39]);
	}
}
