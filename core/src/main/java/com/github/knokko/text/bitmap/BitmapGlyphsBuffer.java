package com.github.knokko.text.bitmap;

import com.github.knokko.text.SizedGlyph;
import com.github.knokko.text.placement.PlacedGlyph;

import java.nio.ByteBuffer;
import java.util.*;

import static org.lwjgl.system.MemoryUtil.memByteBuffer;

public class BitmapGlyphsBuffer {

	private final Map<SizedGlyph, BufferedBitmapGlyph> glyphMap = new HashMap<>();
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

	public List<GlyphQuad> bufferGlyphs(GlyphRasterizer rasterizer, Collection<PlacedGlyph> placedGlyphs) {
		List<GlyphQuad> quads = new ArrayList<>();

		for (var placedGlyph : placedGlyphs) {
			var sections = getSections(rasterizer, placedGlyph.glyph);
			for (var section : sections) {
				int desiredMinX = placedGlyph.minX + section.offsetX();
				int minX = Math.max(placedGlyph.request.minX, desiredMinX);
				quads.add(new GlyphQuad(
						section.bufferIndex(), minX, placedGlyph.minY + section.offsetY(),
						Math.min(placedGlyph.request.maxX, placedGlyph.minX + section.offsetX() + section.width() - 1),
						placedGlyph.minY + section.offsetY() + section.height() - 1,
						section.width(), minX - desiredMinX, placedGlyph.charIndex, placedGlyph.request.userData
				));
			}
		}

		return quads;
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
							// TODO Evict older entries
							if (bufferSlots.isEmpty()) throw new RuntimeException("Not enough slots available");
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

			bufferedGlyph = new BufferedBitmapGlyph(sections);
			glyphMap.put(glyph, bufferedGlyph);
		}

		bufferedGlyph.lastUsed = currentFrame;
		return bufferedGlyph.sections;
	}

	public int getUsedSpace() {
		return buffer.capacity() - slotSize * bufferSlots.size();
	}

	public int countAvailableSpace() {
		int freeSlots = bufferSlots.size();
		int availableSlots = 0;
		for (var glyph : glyphMap.values()) {
			if (glyph.lastUsed < currentFrame) availableSlots += glyph.sections.size();
		}
		return slotSize * (freeSlots + availableSlots);
	}
}
