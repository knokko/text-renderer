package com.github.knokko.text.bitmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record BitmapGlyphSection(int bufferIndex, int offsetX, int offsetY, int width, int height) {

	@SuppressWarnings("SuspiciousNameCombination")
	public static List<BitmapGlyphSection> coverRectangle(
			int slotSize, int width, int height,
			BufferSlotAllocator allocator
	) {
		if (width < 0 || height < 0) throw new IllegalArgumentException();
		if (width == 0 || height == 0) return Collections.emptyList();

		var rowWise = coverRectangleRowWise(slotSize, width, height);
		var columnWise = coverRectangleRowWise(slotSize, height, width);

		int resultSize = Math.min(rowWise.size(), columnWise.size());
		List<BitmapGlyphSection> result = new ArrayList<>(resultSize);

		for (int index = 0; index < resultSize; index++) {

			BitmapGlyphSection dummy;
			if (rowWise.size() <= columnWise.size()) {
				dummy = rowWise.get(index);
			} else {
				BitmapGlyphSection section = columnWise.get(index);
				dummy = new BitmapGlyphSection(0, section.offsetY, section.offsetX, section.height, section.width);
			}

			int bufferIndex = allocator.allocateIndex(dummy.offsetX, dummy.offsetY, dummy.width, dummy.height);
			if (bufferIndex == -1) continue;
			result.add(new BitmapGlyphSection(bufferIndex, dummy.offsetX, dummy.offsetY, dummy.width, dummy.height));
		}

		return result;
	}

	private static List<BitmapGlyphSection> coverRectangleRowWise(int slotSize, int width, int height) {
		int desiredSlotWidth = (int) Math.sqrt(slotSize);
		int desiredNumColumns = Math.max(1, width / desiredSlotWidth);
		int slotWidth = width / desiredNumColumns;
		if (slotWidth * desiredNumColumns < width) slotWidth += 1;
		int slotHeight = slotSize / slotWidth;

		List<BitmapGlyphSection> sections = new ArrayList<>((1 + width / slotWidth) * (1 + height / slotHeight));

		int x;
		int y = 0;
		while (y < height) {
			slotHeight = Math.min(slotHeight, height - y);
			x = 0;
			while (x < width) {
				int currentWidth = Math.min(Math.max(slotWidth, slotSize / slotHeight), width - x);
				sections.add(new BitmapGlyphSection(0, x, y, currentWidth, slotHeight));
				x += currentWidth;
			}
			y += slotHeight;
		}

		return sections;
	}

	@Override
	public String toString() {
		return "BGSection(index=" + bufferIndex + ", rect=(" + offsetX + ", " + offsetY + ", " + width + ", " + height + "))";
	}
}
