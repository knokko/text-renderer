package com.github.knokko.text.bitmap;

import com.github.knokko.text.SizedGlyph;
import com.github.knokko.text.placement.PlacedGlyph;

import java.nio.ByteBuffer;
import java.util.*;

import static org.lwjgl.system.MemoryUtil.memByteBuffer;

/**
 * The <i>BitmapGlyphsBuffer</i> implements stage 2 of the text rendering pipeline: given a list of (placed) glyphs, it
 * ensures that each of the glyphs is rasterized, and stored somewhere in the buffer.
 * <p>
 *    To use this class, you need to create an instance, and use the <i>bufferGlyphs</i> and/or <i>getSections</i>
 *    method (the former is recommended). Furthermore, it is important to call the <i>startFrame</i> method before or
 *    after each frame, to indicate that old glyphs can potentially be erased when this buffer needs to reclaim space.
 * </p>
 */
public class BitmapGlyphsBuffer {

	private final Map<SizedGlyph, BufferedBitmapGlyph> glyphMap = new HashMap<>();
	private final SortedSet<BufferedBitmapGlyph> glyphSet = new TreeSet<>();
	private final List<Integer> bufferSlots = new ArrayList<>();
	private final ByteBuffer buffer;
	private final int slotSize;

	private long currentFrame;

	/**
	 * Constructs a new <i>BitmapGlyphsBuffer</i> that will store the rasterized glyphs in a 'buffer' starting at
	 * memory address <i>address</i>, with a size of <i>size</i> bytes.
	 * @param address The start memory address of the glyph buffer
	 * @param size The size of the glyph buffer, in bytes
	 * @param slotSize The 'slot size', in bytes. All rasterized glyphs will be split in chunks of at most
	 *                 <i>slotSize</i> bytes, which simplifies memory allocation. Using a large slot size will cause
	 *                 a relatively large amount of memory to be wasted for small glyphs, whereas a small slot size
	 *                 will increase the overhead and number of triangles to render.<br>
	 *                 Note that it is beneficial to choose a <i>slotSize</i> that is divisible by a lot of numbers,
	 *                 since this allows a more efficient packing of glyphs into sections. For instance, the default
	 *                 slot size is 120 (bytes), which is divisible by quite some numbers.
	 */
	public BitmapGlyphsBuffer(long address, int size, int slotSize) {
		this.buffer = memByteBuffer(address, size);
		this.slotSize = slotSize;

		int numSlots = size / slotSize;
		for (int index = numSlots - 1; index >= 0; index--) bufferSlots.add(index);
	}

	/**
	 * Constructs a new <i>BitmapGlyphsBuffer</i> that will store the rasterized glyphs in a 'buffer' starting at
	 * memory address <i>address</i>, with a size of <i>size</i> bytes.
	 * @param address The start memory address of the glyph buffer
	 * @param size The size of the glyph buffer, in bytes
	 */
	public BitmapGlyphsBuffer(long address, int size) {
		this(address, size, 120);
	}

	/**
	 * Starts the next frame. Calling this method allows this glyph buffer to delete glyphs that were rasterized
	 * <b>before</b> this method call. Old glyphs will be deleted if this buffer needs space for other glyphs.
	 */
	public void startFrame() {
		currentFrame += 1;
	}

	/**
	 * Rasterizes the given list of placed glyphs (the output of stage 1 of the text rendering pipeline), and stores
	 * them in this glyph buffer. This method will return a list of <i>GlyphQuad</i>s, which is the output of stage 2.
	 * @param rasterizer The rasterizer that should render the glyphs
	 * @param placedGlyphs The glyphs to be rasterized
	 * @return The list of corresponding <i>GlyphQuad</i>s
	 */
	public List<GlyphQuad> bufferGlyphs(GlyphRasterizer rasterizer, List<PlacedGlyph> placedGlyphs) {
		var glyphQuads = new ArrayList<GlyphQuad>(placedGlyphs.size());
		for (var placedGlyph : placedGlyphs) {
			int scale = placedGlyph.glyph.scale;
			var sections = getSections(rasterizer, placedGlyph.glyph);

			for (var section : sections) {
				int desiredMinX = placedGlyph.minX + scale * section.offsetX();
				int desiredMinY = placedGlyph.minY + scale * section.offsetY();
				int desiredMaxX = desiredMinX + scale * section.width() - 1;
				int desiredMaxY = desiredMinY + scale * section.height() - 1;
				int minX = Math.max(placedGlyph.request.minX, desiredMinX);
				int maxX = Math.min(placedGlyph.request.maxX, desiredMaxX);

				while ((1 + maxX - minX) % scale != 0) maxX -= 1;

				int minY = Math.max(placedGlyph.request.minY, desiredMinY);
				int maxY = Math.min(placedGlyph.request.maxY, desiredMaxY);

				while ((1 + maxY - minY) % scale != 0) maxY -= 1;

				glyphQuads.add(new GlyphQuad(
						section.bufferIndex(), minX, minY, maxX, maxY, scale, section.width(),
						minX - desiredMinX + section.width() * (minY - desiredMinY),
						placedGlyph.charIndex, placedGlyph.request.userData
				));
			}
		}

		return glyphQuads;
	}

	/**
	 * Rasterizes a single glyph, and stores the rasterized fragments into this buffer. The sections in which the glyph
	 * is stored, will be returned
	 * @param rasterizer The rasterizer that should be used to rasterize the glyph
	 * @param glyph The glyph to be rasterized
	 * @return The sections in this buffer where the glyph fragments are stored
	 */
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
								// TODO Create proper exception class for this
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
			if (bufferedGlyph.lastUsed != currentFrame) {
				if (!glyphSet.remove(bufferedGlyph)) throw new IllegalStateException("Glyph was not in set");
				bufferedGlyph.lastUsed = currentFrame;
				if (!glyphSet.add(bufferedGlyph))
					throw new IllegalStateException("Glyph should just have been removed");
			}
		}

		return bufferedGlyph.sections;
	}

	/**
	 * Gets the amount of bytes that this glyph buffer is currently using. This method is meant for monitoring
	 * purposes.
	 */
	public int getUsedSpace() {
		return buffer.capacity() - slotSize * bufferSlots.size();
	}

	/**
	 * Gets the amount of unused bytes in this glyph buffer, plus the amount of bytes that this glyph buffer can
	 * reclaim from old glyphs. This method is meant for monitoring purposes.
	 */
	public int countAvailableSpace() {
		int freeSlots = bufferSlots.size();
		int availableSlots = 0;
		for (var glyph : glyphSet) {
			if (glyph.lastUsed < currentFrame) availableSlots += glyph.sections.size();
		}
		return slotSize * (freeSlots + availableSlots);
	}
}
