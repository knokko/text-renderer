package com.github.knokko.text.bitmap;

import com.github.knokko.text.SizedGlyph;
import com.github.knokko.text.placement.PlacedGlyph;
import com.github.knokko.text.placement.TextPlaceRequest;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.libc.LibCStdlib.nmalloc;

public class TestBitmapGlyphsBuffer {

	private static class DummyRasterizer implements GlyphRasterizer {

		private ByteBuffer buffer;
		private int size;

		@Override
		public void set(SizedGlyph glyph, Object userData) {
			int capacity = 2 * glyph.size * glyph.size;
			buffer = BufferUtils.createByteBuffer(capacity);
			for (int counter = 0; counter < capacity; counter++) {
				buffer.put((byte) (counter + 1));
			}
			buffer.position(0);
			this.size = glyph.size;
		}

		@Override
		public String getUserDataKey(Object userData) {
			return "";
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

		@Override
		public void destroy() {}
	}

	@Test
	public void testSingleCharacter() {
		var byteBuffer = memCalloc(1000);
		var glyphsBuffer = new BitmapGlyphsBuffer(memAddress(byteBuffer), byteBuffer.capacity(), 60);
		glyphsBuffer.startFrame();

		int size = 10;
		var result = glyphsBuffer.getSections(new DummyRasterizer(), new SizedGlyph(1234, 0, size, 2), null);

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
				"hello", 12, 34, 56, 78, 20, 10, 1, null
		);

		var glyph1 = new SizedGlyph(12, 0, 15, 3);
		var glyph2 = new SizedGlyph(13, 0, 15, 4);

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

		var quadCount = glyphsBuffer.bufferGlyphs(rasterizer, dummyGlyphs).size();
		assertTrue(quadCount > 2, "Expected " + quadCount + " to be larger than 2");

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

		var placeRequest = new TextPlaceRequest("h", 5, 6, 20, 35, 20, 15, 1, null);

		var placedGlyphs = new ArrayList<PlacedGlyph>();
		placedGlyphs.add(new PlacedGlyph(new SizedGlyph(123, 0, 20, 1), 2, 1, placeRequest, 0));

		var quads = glyphs.bufferGlyphs(new DummyRasterizer(), placedGlyphs);

		byte[][] resultMap = new byte[16][30];

		quads.forEach(quad -> {
			for (int offsetY = 0; offsetY < quad.getHeight(); offsetY++) {
				for (int offsetX = 0; offsetX < quad.getWidth(); offsetX++) {

					int imageX = offsetX + quad.minX;
					int imageY = offsetY + quad.minY;
					int bufferIndex = quad.bufferIndex + offsetX + offsetY * quad.sectionWidth;

					resultMap[imageX - placeRequest.minX][imageY - placeRequest.minY] = memGetByte(bufferAddress + bufferIndex);
				}
			}
		});

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

		var placeRequest = new TextPlaceRequest("h", 5, -6, 20, 55, 20, 15, 1, null);

		var placedGlyphs = new ArrayList<PlacedGlyph>();
		placedGlyphs.add(new PlacedGlyph(new SizedGlyph(123, 0, 20, 1), 2, 1, placeRequest, 0));

		var quads = glyphs.bufferGlyphs(new DummyRasterizer(), placedGlyphs);

		byte[][] resultMap = new byte[16][40];

		quads.forEach(quad -> {
			for (int offsetY = 0; offsetY < quad.getHeight(); offsetY++) {
				for (int offsetX = 0; offsetX < quad.getWidth(); offsetX++) {

					int imageX = offsetX + quad.minX;
					int imageY = offsetY + quad.minY;
					int bufferIndex = quad.bufferIndex + offsetX + offsetY * quad.sectionWidth;

					resultMap[imageX - placeRequest.minX][imageY - 1] = memGetByte(bufferAddress + bufferIndex);
				}
			}
		});

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

	@Test
	public void testScaledBoundY() {
		int bufferSize = 1000;
		long bufferAddress = nmalloc(bufferSize);
		var glyphs = new BitmapGlyphsBuffer(bufferAddress, bufferSize);

		var placeRequest = new TextPlaceRequest("h", 1, 4, 3, 7, 5, 2, 1, null);

		var placedGlyphs = new ArrayList<PlacedGlyph>();
		placedGlyphs.add(new PlacedGlyph(new SizedGlyph(123, 0, 5, 2), 0, 0, placeRequest, 0));

		var quads = glyphs.bufferGlyphs(new DummyRasterizer(), placedGlyphs);
		assertEquals(1, quads.size());

		var quad = quads.get(0);
		assertEquals(1, quad.minX);
		assertEquals(4, quad.minY);
		assertEquals(2, quad.maxX); // maxX is 2 instead of 3 because width must be a multiple of scale
		assertEquals(7, quad.maxY);
		assertEquals(placeRequest.minX + placeRequest.minY * quad.sectionWidth, quad.bufferIndex);
		assertEquals(2, quad.scale);
		assertEquals(5, quad.sectionWidth);
	}

	@Test
	public void testReuseGlyphsWhenUserDataIsIgnored() {
		int bufferSize = 1000;
		long bufferAddress = nmalloc(bufferSize);
		var glyphs = new BitmapGlyphsBuffer(bufferAddress, bufferSize);

		var placedGlyphs = new ArrayList<PlacedGlyph>();
		for (int userData = 0; userData < 1000; userData++) {
			var placeRequest = new TextPlaceRequest("h", 1, 4, 3, 7, 5, 2, 1, userData);
			placedGlyphs.add(new PlacedGlyph(new SizedGlyph(123, 0, 5, 2), 0, 0, placeRequest, 0));
		}

		var quads = glyphs.bufferGlyphs(new DummyRasterizer(), placedGlyphs);
		assertEquals(1000, quads.size());

		int firstBufferIndex = quads.get(0).bufferIndex;
		for (var quad : quads) assertEquals(firstBufferIndex, quad.bufferIndex);
	}

	@Test
	public void testDontReuseGlyphsWhenUserDataIsUsed() {
		int bufferSize = 1000 * 1000;
		long bufferAddress = nmalloc(bufferSize);
		var glyphs = new BitmapGlyphsBuffer(bufferAddress, bufferSize, 900);

		var rasterizer = new GlyphRasterizer() {

			private final ByteBuffer buffer = BufferUtils.createByteBuffer(900);

			@Override
			public void set(SizedGlyph glyph, Object userData) {
				for (int counter = 0; counter < buffer.capacity(); counter++) buffer.put((byte) counter);
				buffer.position(0);
			}

			@Override
			public String getUserDataKey(Object userData) {
				return userData.toString();
			}

			@Override
			public int getBufferWidth() {
				return 30;
			}

			@Override
			public int getBufferHeight() {
				return 30;
			}

			@Override
			public ByteBuffer getBuffer() {
				return buffer;
			}

			@Override
			public void destroy() {}
		};

		var placedGlyphs = new ArrayList<PlacedGlyph>();
		for (int userData = 0; userData < 1000; userData++) {
			var placeRequest = new TextPlaceRequest("h", 1, 4, 3, 7, 5, 2, 1, userData);
			placedGlyphs.add(new PlacedGlyph(new SizedGlyph(123, 0, 5, 2), 0, 0, placeRequest, 0));
		}

		var quads = glyphs.bufferGlyphs(rasterizer, placedGlyphs);
		assertEquals(1000, quads.size());

		Set<Integer> bufferIndices = new HashSet<>();
		for (var quad : quads) bufferIndices.add(quad.bufferIndex);
		assertEquals(1000, bufferIndices.size());
	}
}
