package com.github.knokko.text.bitmap;

import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class TestBitmapGlyphSection {

	private static class DummySlotAllocator implements BufferSlotAllocator {

		private int next;

		@Override
		public int allocateIndex(int offsetX, int offsetY, int width, int height) {
			return next++;
		}
	}

	private void assertValid(Collection<BitmapGlyphSection> rectangles, int slotSize, int totalWidth, int totalHeight) {
		for (var rectangle : rectangles) {
			assertTrue(rectangle.offsetX() >= 0, "got " + rectangle.offsetX());
			assertTrue(rectangle.offsetY() >= 0, "got " + rectangle.offsetY());
			assertTrue(
					rectangle.offsetX() + rectangle.width() <= totalWidth,
					"got " + rectangle.offsetX() + " + " + rectangle.width()
			);
			assertTrue(
					rectangle.offsetY() + rectangle.height() <= totalHeight,
					"got " + rectangle.offsetY() + " + " + rectangle.height()
			);
			assertTrue(
					rectangle.width() * rectangle.height() <= slotSize,
					"got " + rectangle.width() + " x " + rectangle.height()
			);
		}

		for (int testX = 0; testX < totalWidth; testX++) {
			int x = testX;
			for (int testY = 0; testY < totalHeight; testY++) {
				int y = testY;
				long numRectangles = rectangles.stream().filter(
						rectangle -> rectangle.offsetX() <= x && rectangle.offsetY() <= y &&
								rectangle.offsetX() + rectangle.width() > x &&
								rectangle.offsetY() + rectangle.height() > y
				).count();
				if (numRectangles != 1) {
					fail("point (" + x + ", " + y + ") is covered by " + numRectangles + " rectangles: " + rectangles);
				}
			}
		}
	}

	@Test
	public void testCoverRectangle15x30() {
		var rectangles = BitmapGlyphSection.coverRectangle(120, 15, 30, new DummySlotAllocator());
		assertEquals(4, rectangles.size());
		assertValid(rectangles, 120, 15, 30);
	}

	@Test
	public void testCoverSmallRectangles() {
		int slotSize = 120;

		for (int width = 1; width < 40; width++) {
			for (int height = 1; height < 40; height++) {
				int area = width * height;
				int targetSize = 1 + Math.max(area, slotSize) / slotSize;

				var rectangles = BitmapGlyphSection.coverRectangle(slotSize, width, height, new DummySlotAllocator());
				assertValid(rectangles, slotSize, width, height);
				assertTrue(
						rectangles.size() <= 1 + targetSize,
						"dimensions are " + width + " x " + height + " so target is " + targetSize +
								", but got " + rectangles.size() + ": " + rectangles);
			}
		}
	}
}
