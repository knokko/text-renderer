package com.github.knokko.text.bitmap;

import com.github.knokko.text.SizedGlyph;
import com.github.knokko.text.placement.PlacedGlyph;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Stream;

import static org.lwjgl.system.MemoryUtil.memByteBuffer;

public class BitmapGlyphsBuffer {

	private final Map<SizedGlyph, BufferedBitmapGlyph> glyphMap = new HashMap<>();
	private final SortedSet<BufferedBitmapGlyph> glyphSet = new TreeSet<>();
	private final List<Integer> bufferSlots = new ArrayList<>();
	private final ByteBuffer buffer;
	private final int slotSize;

	private long currentFrame;

	public BitmapGlyphsBuffer(long address, int size, int slotSize) {
		this.buffer = memByteBuffer(address, size);
		this.slotSize = slotSize;

		int numSlots = size / slotSize;
		for (int index = numSlots - 1; index >= 0; index--) bufferSlots.add(index);
	}

	public BitmapGlyphsBuffer(long address, int size) {
		this(address, size, 120);
	}

	public void startFrame() {
		currentFrame += 1;
	}

	public Stream<GlyphQuad> bufferGlyphs(GlyphRasterizer rasterizer, Stream<PlacedGlyph> placedGlyphs) {
		return placedGlyphs.flatMap(placedGlyph -> {
			int scale = placedGlyph.glyph.scale;
			var sections = getSections(rasterizer, placedGlyph.glyph);
			return sections.stream().map(section -> {
				int desiredMinX = placedGlyph.minX + scale * section.offsetX();
				int desiredMinY = placedGlyph.minY + scale * section.offsetY();
				int desiredMaxX = desiredMinX + scale * section.width() - 1;
				int desiredMaxY = desiredMinY + scale * section.height() - 1;
				int minX = Math.max(placedGlyph.request.minX, desiredMinX);
				int maxX = Math.min(placedGlyph.request.maxX, desiredMaxX);

				while ((1 + maxX - minX) % scale != 0) maxX -= 1;

				int minY, maxY;
				if (placedGlyph.request.enforceBoundsY) {
					minY = Math.max(placedGlyph.request.minY, desiredMinY);
					maxY = Math.min(placedGlyph.request.maxY, desiredMaxY);
				} else {
					minY = desiredMinY;
					maxY = desiredMaxY;
				}

				while ((1 + maxY - minY) % scale != 0) maxY -= 1;

				return new GlyphQuad(
						section.bufferIndex(), minX, minY, maxX, maxY, scale, section.width(),
						minX - desiredMinX + section.width() * (minY - desiredMinY),
						placedGlyph.charIndex, placedGlyph.request.userData
				);
			});
		});
	}

	public List<BitmapGlyphSection> getSections(GlyphRasterizer rasterizer, SizedGlyph glyph) {
		var bufferedGlyph = glyphMap.get(glyph);

		if (bufferedGlyph == null) {

			rasterizer.set(glyph.id, glyph.faceIndex, glyph.size);
			ByteBuffer bitmap = rasterizer.getBuffer();
			List<BitmapGlyphSection> sections = BitmapGlyphSection.coverRectangle(
					slotSize, rasterizer.getBufferWidth(), rasterizer.getBufferHeight(), (x, y, width, height) -> {
						boolean hasNonZero = false;
						for (int offsetY = 0; offsetY < height; offsetY++) {
							for (int offsetX = 0; offsetX < width; offsetX++) {
								if (bitmap.get(x + offsetX + (y + offsetY) * rasterizer.getBufferWidth()) != 0) {
									hasNonZero = true;
									break;
								}
							}
						}

						if (hasNonZero) {
							while (bufferSlots.isEmpty()) {
								var oldestGlyph = glyphSet.first();
								if (oldestGlyph.lastUsed < currentFrame) {
									if (!glyphSet.remove(oldestGlyph)) {
										throw new IllegalStateException("Failed to remove oldest glyph");
									}
									if (glyphMap.remove(oldestGlyph.glyph) != oldestGlyph) {
										throw new IllegalStateException("Unexpected oldest glyph was removed");
									}
									for (var section : oldestGlyph.sections) {
										bufferSlots.add(section.bufferIndex() / slotSize);
									}
								} else throw new RuntimeException("Not enough slots/capacity available");
							}
							return slotSize * bufferSlots.remove(bufferSlots.size() - 1);
						} else return -1;
					}
			);

			for (BitmapGlyphSection section : sections) {
				int baseIndex = section.bufferIndex();
				for (int bufferY = 0; bufferY < section.height(); bufferY++) {
					buffer.put(
							baseIndex + bufferY * section.width(), bitmap,
							section.offsetX() + (bufferY + section.offsetY()) * rasterizer.getBufferWidth(), section.width()
					);
				}
			}

			bufferedGlyph = new BufferedBitmapGlyph(glyph, sections, currentFrame);
			if (glyphMap.put(glyph, bufferedGlyph) != null) throw new RuntimeException("Didn't expect existing element");
			if (!glyphSet.add(bufferedGlyph)) throw new RuntimeException("Didn't expect existing element in glyph set");
		} else {
			if (!glyphSet.remove(bufferedGlyph)) throw new IllegalStateException("Glyph was not in set");
			bufferedGlyph.lastUsed = currentFrame;
			if (!glyphSet.add(bufferedGlyph)) throw new IllegalStateException("Glyph should just have been removed");
		}

		return bufferedGlyph.sections;
	}

	public int getUsedSpace() {
		return buffer.capacity() - slotSize * bufferSlots.size();
	}

	public int countAvailableSpace() {
		int freeSlots = bufferSlots.size();
		int availableSlots = 0;
		for (var glyph : glyphSet) {
			if (glyph.lastUsed < currentFrame) availableSlots += glyph.sections.size();
		}
		return slotSize * (freeSlots + availableSlots);
	}
}
