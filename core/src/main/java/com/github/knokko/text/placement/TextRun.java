package com.github.knokko.text.placement;

import org.lwjgl.util.harfbuzz.hb_glyph_info_t;
import org.lwjgl.util.harfbuzz.hb_glyph_position_t;

public record TextRun(
		String text, int faceIndex, int offset,
		hb_glyph_info_t.Buffer glyphInfos,
		hb_glyph_position_t.Buffer glyphPositions
) {

	@Override
	public String toString() {
		return "TextRun(" + text + ", face=" + faceIndex + ", offset=" + offset + ", info-count=" + (glyphInfos != null ? glyphInfos.capacity() : 0) + ")";
	}
}
