package com.github.knokko.text.font;

import org.lwjgl.util.freetype.FT_Face;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.lwjgl.util.freetype.FreeType.*;
import static org.lwjgl.util.harfbuzz.HarfBuzz.*;

/**
 * This class is meant for internal use only. It wraps a FreeType face and some HarfBuzz objects.
 */
public class TextFace {

	public final FT_Face ftFace;
	public final int fontSize, scale;
	public final long hbFont;
	public final long hbBuffer;
	final FontData.TextFaceKey key;

	TextFace(FT_Face ftFace, int size, int scale, FontData.TextFaceKey key) {
		this.ftFace = ftFace;
		this.hbBuffer = hb_buffer_create();
		this.key = key;

		if (size <= 0) throw new IllegalArgumentException("Size (" + size + ") must be positive");

		assertFtSuccess(FT_Set_Char_Size(
				ftFace, size, 0, 0, 0
		), "Set_Char_Size", "TextFace.setSize(" + size + ")");

		this.fontSize = size;
		this.scale = scale;

		this.hbFont = hb_ft_font_create_referenced(this.ftFace.address());
		hb_ft_font_set_funcs(this.hbFont);
	}

	@Override
	public String toString() {
		return "TextFace(size = " + fontSize + ", scale = " + scale + ")";
	}

	void destroy() {
		assertFtSuccess(FT_Done_Face(ftFace), "Done_Face", "TextFace.destroy");
		hb_buffer_destroy(hbBuffer);
	}
}
