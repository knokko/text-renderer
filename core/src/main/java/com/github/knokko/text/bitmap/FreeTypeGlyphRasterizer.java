package com.github.knokko.text.bitmap;

import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;

import java.nio.ByteBuffer;
import java.util.function.IntConsumer;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.lwjgl.util.freetype.FreeType.FT_LOAD_RENDER;
import static org.lwjgl.util.freetype.FreeType.FT_Load_Glyph;

public class FreeTypeGlyphRasterizer implements GlyphRasterizer {

	private final FT_Face freeTypeFace;
	private final IntConsumer changeSize;

	private int width, height;
	private ByteBuffer buffer;

	public FreeTypeGlyphRasterizer(FT_Face freeTypeFace, IntConsumer changeSize) {
		this.freeTypeFace = freeTypeFace;
		this.changeSize = changeSize;
	}

	@Override
	public void set(int glyph, int size) {
		String context = "FreeTypeGlyphRasterizer.set(" + glyph + ", " + size + ")";
		changeSize.accept(size);
		assertFtSuccess(FT_Load_Glyph(freeTypeFace, glyph, FT_LOAD_RENDER), "Load_Glyph", context);

		FT_GlyphSlot slot = freeTypeFace.glyph();
		if (slot == null) throw new Error("Glyph slot must not be null at this point");

		FT_Bitmap bitmap = slot.bitmap();
		this.width = bitmap.width();
		this.height = bitmap.rows();
		this.buffer = bitmap.buffer(this.width * this.height);
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
