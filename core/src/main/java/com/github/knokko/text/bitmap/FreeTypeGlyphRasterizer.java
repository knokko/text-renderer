package com.github.knokko.text.bitmap;

import com.github.knokko.text.font.FontData;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_GlyphSlot;

import java.nio.ByteBuffer;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.lwjgl.util.freetype.FreeType.FT_LOAD_RENDER;
import static org.lwjgl.util.freetype.FreeType.FT_Load_Glyph;

public class FreeTypeGlyphRasterizer implements GlyphRasterizer {

	private final FontData font;

	private int width, height;
	private ByteBuffer buffer = BufferUtils.createByteBuffer(10_000 * 10_000); // TODO Do this smarter

	public FreeTypeGlyphRasterizer(FontData font) {
		this.font = font;
	}

	@Override
	public void set(int glyph, int faceIndex, int size) {
		String context = "FreeTypeGlyphRasterizer.set(" + glyph + ", " + faceIndex + ", " + size + ")";
		var face = font.borrowFaceWithSize(faceIndex, size, 1);
		assertFtSuccess(FT_Load_Glyph(face.ftFace, glyph, FT_LOAD_RENDER), "Load_Glyph", context);

		FT_GlyphSlot slot = face.ftFace.glyph();
		if (slot == null) throw new Error("Glyph slot must not be null at this point");

		@SuppressWarnings("resource") FT_Bitmap bitmap = slot.bitmap();
		this.width = bitmap.width();
		this.height = bitmap.rows();

		if (this.width > 0 && this.height > 0) {
			var bufferView = bitmap.buffer(this.width * this.height);
			this.buffer.position(0);
			this.buffer.limit(this.buffer.capacity());
			this.buffer.put(bufferView);
			this.buffer.position(0);
			this.buffer.limit(this.width * this.height);
		} else this.buffer.limit(0);

		font.returnFace(face);
	}

	@Override
	public int getBufferWidth() {
		return width;
	}

	@Override
	public int getBufferHeight() {
		return height;
	}

	@Override
	public ByteBuffer getBuffer() {
		return buffer;
	}
}
