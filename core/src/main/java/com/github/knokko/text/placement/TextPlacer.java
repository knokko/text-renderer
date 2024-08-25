package com.github.knokko.text.placement;

import com.github.knokko.text.SizedGlyph;
import com.github.knokko.text.font.TextFont;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Size;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.freetype.FreeType.FT_Load_Glyph;
import static org.lwjgl.util.harfbuzz.HarfBuzz.*;
import static org.lwjgl.util.harfbuzz.HarfBuzz.hb_buffer_get_glyph_positions;

public class TextPlacer {

	private final TextFont font;
	private final long hbBuffer;

	private final long[] hbFonts;
	private final TextSplitter splitter;

	private final Map<GlyphOffsetKey, GlyphOffset> glyphOffsets = new HashMap<>(); // TODO Throw old entries away
	private int currentHeight;

	public TextPlacer(TextFont font) {
		this.font = font;
		this.hbBuffer = hb_buffer_create();
		this.hbFonts = new long[font.getFreeTypeFaces().length];
		this.splitter = new TextSplitter(hbBuffer, hbFonts);
	}

	private void setHeight(int height) {
		if (height == currentHeight) return;

		font.setHeight(height);

		for (int faceIndex = 0; faceIndex < hbFonts.length; faceIndex++) {
			if (hbFonts[faceIndex] != 0L) hb_font_destroy(hbFonts[faceIndex]);
			hbFonts[faceIndex] = hb_ft_font_create_referenced(font.getFreeTypeFaces()[faceIndex].address());
			hb_ft_font_set_funcs(hbFonts[faceIndex]);
		}

		currentHeight = height;
	}

	public Stream<PlacedGlyph> place(Stream<TextPlaceRequest> requests) {
		currentHeight = -1;

		// TODO Parallel stream?
		return requests.sorted().flatMap(request -> {
			try (var stack = stackPush()) {
				return placeFree(request, stack).stream().map(placement -> new PlacedGlyph(
						placement.glyph,
						request.minX + placement.minX,
						request.minY + placement.minY,
						placement.request,
						placement.charIndex
				));
			}
		});
	}

	@SuppressWarnings("resource")
	private List<PlacedGlyph> placeFree(TextPlaceRequest request, MemoryStack stack) {
		this.setHeight(request.getHeight());
		List<TextRun> runs = splitter.split(request.text, stack);
		List<PlacedGlyph> placements = new ArrayList<>();

		int cursorX = 0;
		int cursorY = 0;

		int maxAscent = 0;

		for (TextRun run : runs) {
			FT_Size faceSize = font.getFreeTypeFaces()[run.faceIndex()].size();
			if (faceSize == null) throw new RuntimeException("Face size must not be null right now");
			int ascent = Math.toIntExact(faceSize.metrics().ascender() / 64);
			maxAscent = Math.max(maxAscent, ascent);
		}

		runLoop:
		for (TextRun run : runs) {
			String context = "TextPlacer.placeFree(run = " + run.text() + ")";

			hb_buffer_reset(hbBuffer);
			hb_buffer_add_utf16(hbBuffer, stack.UTF16(run.text()), 0, -1);
			hb_buffer_guess_segment_properties(hbBuffer);
			hb_buffer_set_cluster_level(hbBuffer, HB_BUFFER_CLUSTER_LEVEL_MONOTONE_CHARACTERS);
			hb_shape(hbFonts[run.faceIndex()], hbBuffer, null);

			var glyphInfo = hb_buffer_get_glyph_infos(hbBuffer);
			var glyphPositions = hb_buffer_get_glyph_positions(hbBuffer);
			if (glyphInfo == null || glyphPositions == null)
				throw new RuntimeException("Glyph info/positions are null");

			for (int glyphIndex = 0; glyphIndex < glyphPositions.limit(); glyphIndex++) {
				var position = glyphPositions.get(glyphIndex);
				var info = glyphInfo.get(glyphIndex);

				int charIndex = info.cluster() + run.offset();
				if (info.cluster() >= run.text().length()) continue;

				int glyph = info.codepoint();
				var glyphOffset = glyphOffsets.computeIfAbsent(new GlyphOffsetKey(request.getHeight(), run.faceIndex(), glyph), key -> {
					assertFtSuccess(FT_Load_Glyph(font.getFreeTypeFaces()[run.faceIndex()], glyph, 0), "FT_Load_Glyph", context);
					var glyphSlot = font.getFreeTypeFaces()[run.faceIndex()].glyph();
					if (glyphSlot == null) throw new RuntimeException("Glyph slot should not be null right now");
					return new GlyphOffset(glyphSlot.bitmap_left(), glyphSlot.bitmap_top());
				});

				int scale = font.getScale();
				placements.add(new PlacedGlyph(
						new SizedGlyph(glyph, run.faceIndex(), font.getSize(false), scale),
						cursorX + scale * (position.x_offset() + glyphOffset.bitmapLeft),
						cursorY + scale * (position.y_offset() - glyphOffset.bitmapTop + maxAscent),
						request, charIndex
				));

				cursorX += scale * position.x_advance() / 64;
				cursorY += scale * position.y_advance() / 64;

				if (cursorX > request.getWidth() && splitter.wasBaseLeftToRight) break runLoop;
			}
		}

		if (!splitter.wasBaseLeftToRight) {
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
		for (int faceIndex = 0; faceIndex < hbFonts.length; faceIndex++) {
			if (hbFonts[faceIndex] != 0L) hb_font_destroy(hbFonts[faceIndex]);
			hbFonts[faceIndex] = 0L;
		}
	}

	record GlyphOffsetKey(int height, int fontIndex, int glyph) {}
	record GlyphOffset(int bitmapLeft, int bitmapTop) {}
}
