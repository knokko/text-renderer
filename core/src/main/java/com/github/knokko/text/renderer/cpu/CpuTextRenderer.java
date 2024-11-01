package com.github.knokko.text.renderer.cpu;

import com.github.knokko.text.bitmap.FreeTypeGlyphRasterizer;
import com.github.knokko.text.font.FontData;
import com.github.knokko.text.bitmap.BitmapGlyphsBuffer;
import com.github.knokko.text.placement.TextPlaceRequest;
import com.github.knokko.text.placement.TextPlacer;

import java.nio.ByteBuffer;
import java.util.Collection;

import static org.lwjgl.system.MemoryUtil.*;

/**
 * An abstract class for implementations of stage 3 of the text rendering pipeline. This class does most of the work,
 * and tells subclasses which pixels should get which intensity. Subclasses only need to implement the
 * {@link #setPixel} method. The only implementation provided by this library is {@link BufferedImageTextRenderer}.
 * <p>
 *     Note that this class is intended to be used as reference/example since the java standard library is perfectly
 *     capable of text rendering.
 * </p>
 */
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

	/**
	 * Sets the value of the pixel at {@code (x, y)} to {@code value}
	 * @param x The x-coordinate of the pixel to be changed
	 * @param y The y-coordinate of the pixel to be changed
	 * @param value The new value of the pixel, in the range [0, 255]
	 */
	public abstract void setPixel(int x, int y, int value);

	/**
	 * Renders the given requests
	 */
	public void render(Collection<TextPlaceRequest> requests) {
		var placedGlyphs = placer.place(requests);
		glyphsBuffer.startFrame();
		var glyphQuads = glyphsBuffer.bufferGlyphs(rasterizer, placedGlyphs);
		glyphQuads.forEach(quad -> {
			for (int offsetY = 0; offsetY < quad.getHeight(); offsetY++) {
				for (int offsetX = 0; offsetX < quad.getWidth(); offsetX++) {

					int imageX = offsetX + quad.minX;
					int imageY = offsetY + quad.minY;
					int bufferIndex = quad.bufferIndex + offsetX / quad.scale + (offsetY / quad.scale) * quad.sectionWidth;
					setPixel(imageX, imageY, byteBuffer.get(bufferIndex) & 0xFF);
				}
			}
		});
	}

	/**
	 * Destroys this renderer. You should call this once you no longer need it.
	 */
	public void destroy() {
		placer.destroy();
		memFree(byteBuffer);
		rasterizer.destroy();
	}
}
