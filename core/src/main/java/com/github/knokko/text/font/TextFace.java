package com.github.knokko.text.font;

import org.lwjgl.util.freetype.FT_Face;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.lwjgl.util.freetype.FreeType.*;
import static org.lwjgl.util.harfbuzz.HarfBuzz.*;

public class TextFace {

	public final FT_Face ftFace;
	private final int scaledFontSize, unscaledFontSize, scale, heightScale;
	public final long hbFont;
	public final long hbBuffer;
	final FontData.TextFaceKey key;

	TextFace(FT_Face ftFace, int size, int heightScale, FontData.TextFaceKey key) {
		this.ftFace = ftFace;
		this.hbBuffer = hb_buffer_create();
		this.key = key;

		if (size <= 0) throw new IllegalArgumentException("Size (" + size + ") must be positive");

		int currentScale = 1;
		int currentUnscaledFontSize = size;

		String context = "TextFace.setSize(" + size + ")";

		while (true) {
			int charSizeResult = FT_Set_Char_Size(
					ftFace, 0, currentUnscaledFontSize * 64L, 0, 5 * currentUnscaledFontSize
			);
			// TODO Handle very small sizes by downscaling
			if (charSizeResult == FT_Err_Invalid_Pixel_Size) {
				currentScale += 1;
				currentUnscaledFontSize = size / currentScale;
				if (size % currentScale != 0) currentUnscaledFontSize += 1;
				continue;
			}

			assertFtSuccess(charSizeResult, "Set_Char_Size", context);
			assertFtSuccess(FT_Set_Pixel_Sizes(
					ftFace, 0, currentUnscaledFontSize
			), "Set_Pixel_Sizes", context);
			break;
		}

		this.scaledFontSize = size;
		this.unscaledFontSize = currentUnscaledFontSize;
		this.scale = currentScale;
		this.heightScale = heightScale;

		this.hbFont = hb_ft_font_create_referenced(this.ftFace.address());
		hb_ft_font_set_funcs(this.hbFont);
	}

	public int getSize(boolean scaled) {
		if (scaled) return scaledFontSize;
		else return unscaledFontSize;
	}

	public int getScale() {
		return scale * heightScale;
	}

	void destroy() {
		assertFtSuccess(FT_Done_Face(ftFace), "Done_Face", "TextFace.destroy");
		hb_buffer_destroy(hbBuffer);
	}
}
