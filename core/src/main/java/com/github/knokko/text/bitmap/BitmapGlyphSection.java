package com.github.knokko.text.bitmap;

import java.util.ArrayList;
import java.util.List;

public class BitmapGlyphSection {

	public static List<BitmapGlyphSection> coverRectangle(
			int slotSize, int width, int height,
			BufferSlotAllocator allocator
	) {
		// TODO Select slot dimensions in a more clever way
		int slotWidth = (int) Math.sqrt(slotSize);
		int slotHeight = slotSize / slotWidth;

		List<BitmapGlyphSection> sections = new ArrayList<>((1 + width / slotWidth) * (1 + height / slotHeight));

		// TODO Optimize algorithm around the edges
		int x;
		int y = 0;
		while (y < height) {
			slotHeight = Math.min(slotHeight, height - y);
			x = 0;
			while (x < width) {
				int currentWidth = Math.min(slotWidth, width - x);
				int bufferIndex = allocator.allocateIndex(x, y, currentWidth, slotHeight);
				if (bufferIndex >= 0) {
					sections.add(new BitmapGlyphSection(bufferIndex, x, y, currentWidth, slotHeight));
				}
				x += currentWidth;
			}
			y += slotHeight;
		}

		return sections;
	}

	public final int bufferIndex;
	public final int offsetX, offsetY;
	public final int width, height;

	public BitmapGlyphSection(int bufferIndex, int offsetX, int offsetY, int width, int height) {
		this.bufferIndex = bufferIndex;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.width = width;
		this.height = height;
	}

	@Override
	public String toString() {
		return "BGSection(index=" + bufferIndex + ", rect=(" + offsetX + ", " + offsetY + ", " + width + ", " + height + "))";
	}
}
