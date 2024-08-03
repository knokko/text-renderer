package com.github.knokko.text.placement;

import com.github.knokko.text.SizedGlyph;
import com.github.knokko.text.TextFont;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Size;

import java.text.Bidi;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.freetype.FreeType.FT_Load_Glyph;
import static org.lwjgl.util.harfbuzz.HarfBuzz.*;
import static org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_get_glyph_positions;

public class TextPlacer {

	private final TextFont font;
	private final long hbBuffer;

	public TextPlacer(TextFont font) {
		this.font = font;
		this.hbBuffer = hb_buffer_create();
	}

	public List<PlacedGlyph> place(Collection<TextPlaceRequest> requestCollection) {
		TextPlaceRequest[] requests = requestCollection.toArray(new TextPlaceRequest[0]);
		Arrays.sort(requests);

		List<PlacedGlyph> placements = new ArrayList<>();

		int lastHeight = 0;
		long hbFont = 0;
		for (var request : requests) {
			int height = request.getHeight();
			if (height != lastHeight) {
				font.setHeight(height);
				lastHeight = height;
				if (hbFont != 0L) hb_font_destroy(hbFont);
				hbFont = hb_ft_font_create_referenced(font.getFreeTypeFace().address());
				hb_ft_font_set_funcs(hbFont);
			}

			try (var stack = stackPush()) {
				var freePlacements = placeFree(hbFont, request, stack);
				for (var placement : freePlacements) {
					placements.add(new PlacedGlyph(
							placement.glyph,
							request.minX + placement.minX,
							request.minY + placement.minY,
							placement.request,
							placement.charIndex
					));
				}
			}
		}

		if (hbFont != 0L) hb_font_destroy(hbFont);
		return placements;
	}

	@SuppressWarnings("resource")
	private List<PlacedGlyph> placeFree(long hbFont, TextPlaceRequest request, MemoryStack stack) {
		Bidi bidi = new Bidi(request.text, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
		List<PlacedGlyph> placements = new ArrayList<>();

		FT_Size faceSize = font.getFreeTypeFace().size();
		if (faceSize == null) throw new RuntimeException("Face size must not be null right now");
		int ascent = Math.toIntExact(faceSize.metrics().ascender() / 64);

		int cursorX = 0;
		int cursorY = 0;

		bidiLoop:
		for (int bidiRun = 0; bidiRun < bidi.getRunCount(); bidiRun++) {
			String runString = request.text.substring(bidi.getRunStart(bidiRun), bidi.getRunLimit(bidiRun));
			String context = "TextPlacer.placeRun(" + runString + ")";

			hb_buffer_reset(hbBuffer);

			hb_buffer_add_utf16(hbBuffer, stack.UTF16(runString), 0, -1);

			hb_buffer_guess_segment_properties(hbBuffer);
			hb_shape(hbFont, hbBuffer, null);

			var glyphInfo = hb_buffer_get_glyph_infos(hbBuffer);
			var glyphPositions = hb_buffer_get_glyph_positions(hbBuffer);
			if (glyphInfo == null || glyphPositions == null)
				throw new RuntimeException("Glyph info/positions are null");

			for (int glyphIndex = 0; glyphIndex < glyphPositions.limit(); glyphIndex++) {
				var position = glyphPositions.get(glyphIndex);
				var info = glyphInfo.get(glyphIndex);

				int charIndex = info.cluster() + bidi.getRunStart(bidiRun);
				int glyph = info.codepoint();

				assertFtSuccess(FT_Load_Glyph(font.getFreeTypeFace(), glyph, 0), "FT_Load_Glyph", context);
				var glyphSlot = font.getFreeTypeFace().glyph();
				if (glyphSlot == null) throw new RuntimeException("Glyph slot should not be null right now");

				placements.add(new PlacedGlyph(
						new SizedGlyph(glyph, font.getSize()),
						cursorX + position.x_offset() + glyphSlot.bitmap_left(),
						cursorY + position.y_offset() - glyphSlot.bitmap_top() + ascent,
						request, charIndex
				));

				cursorX += position.x_advance() / 64;
				cursorY += position.y_advance() / 64;

				if (cursorX > request.getWidth() && bidi.baseIsLeftToRight()) break bidiLoop;
			}
		}

		if (!bidi.baseIsLeftToRight()) {
			int shift = request.getWidth() - cursorX;
			placements = placements.stream().map(placement -> new PlacedGlyph(
					placement.glyph, placement.minX + shift,
					placement.minY, placement.request, placement.charIndex
			)).collect(Collectors.toList());

			int cutIndex;
			for (cutIndex = placements.size() - 1; cutIndex >= 0; cutIndex--) {
				if (placements.get(cutIndex).minX < 0) break;
			}

			if (cutIndex > 0) placements = placements.subList(cutIndex, placements.size());
		}

		return placements;
	}

	public void destroy() {
		hb_buffer_destroy(hbBuffer);
	}
}
