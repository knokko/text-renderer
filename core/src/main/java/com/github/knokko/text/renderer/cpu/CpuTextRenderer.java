package com.github.knokko.text.renderer.cpu;

import com.github.knokko.text.bitmap.FreeTypeGlyphRasterizer;
import com.github.knokko.text.font.FontData;
import com.github.knokko.text.bitmap.BitmapGlyphsBuffer;
import com.github.knokko.text.placement.TextPlaceRequest;
import com.github.knokko.text.placement.TextPlacer;

import java.nio.ByteBuffer;
import java.util.Collection;

import static org.lwjgl.system.MemoryUtil.*;

public abstract class CpuTextRenderer {

	private final FreeTypeGlyphRasterizer rasterizer;
	private final TextPlacer placer;
	private final ByteBuffer byteBuffer;
	private final BitmapGlyphsBuffer glyphsBuffer;

	public CpuTextRenderer(FontData font, int glyphBufferCapacity) {
		this.rasterizer = new FreeTypeGlyphRasterizer(font);
		this.placer = new TextPlacer(font);
		this.byteBuffer = memAlloc(glyphBufferCapacity);
		this.glyphsBuffer = new BitmapGlyphsBuffer(memAddress(byteBuffer), glyphBufferCapacity);
	}

	public abstract void setPixel(int x, int y, int value);

	public void render(Collection<TextPlaceRequest> requests) {
		var placedGlyphs = placer.place(requests.stream());
		glyphsBuffer.startFrame();
		var glyphQuads = glyphsBuffer.bufferGlyphs(rasterizer, placedGlyphs);
		glyphQuads.forEach(quad -> {
			for (int offsetY = 0; offsetY < quad.getHeight(); offsetY++) {
				for (int offsetX = 0; offsetX < quad.getActualWidth(); offsetX++) {

					int imageX = offsetX + quad.minX;
					int imageY = offsetY + quad.minY;
					int bufferIndex = quad.bufferIndex + offsetX / quad.scale + quad.bufferOffsetX + (offsetY / quad.scale) * quad.sectionWidth;
					setPixel(imageX, imageY, byteBuffer.get(bufferIndex) & 0xFF);
				}
			}
		});
	}

	public void destroy() {
		placer.destroy();
		memFree(byteBuffer);
	}
}
