package com.github.knokko.text.placement;

import com.github.knokko.text.SizedGlyph;
import com.github.knokko.text.font.FontData;
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

	private final FontData fontData;
	private final long hbBuffer;

	private final TextSplitter splitter;

	private final Map<GlyphOffsetKey, GlyphOffset> glyphOffsets = new HashMap<>(); // TODO Throw old entries away

	public TextPlacer(FontData font) {
		this.fontData = font;
		this.hbBuffer = hb_buffer_create();
		this.splitter = new TextSplitter(fontData, hbBuffer);
	}

	public Stream<PlacedGlyph> place(Stream<TextPlaceRequest> requests) {
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
		List<TextRun> runs = splitter.split(request.text, stack);
		List<PlacedGlyph> placements = new ArrayList<>();

		int cursorX = 0;
		int cursorY = 0;

		int maxAscent = 0;

		for (TextRun run : runs) {
			// TODO Cache this stuff
			var tempFace = fontData.borrowFaceWithHeight(run.faceIndex(), request.getHeight());
			FT_Size faceSize = tempFace.ftFace.size();
			if (faceSize == null) throw new RuntimeException("Face size must not be null right now");
			int ascent = Math.toIntExact(faceSize.metrics().ascender() / 64);
			maxAscent = Math.max(maxAscent, ascent);
			fontData.returnFace(tempFace);
		}

		runLoop:
		for (TextRun run : runs) {
			String context = "TextPlacer.placeFree(run = " + run.text() + ")";
			var currentFace = fontData.borrowFaceWithHeight(run.faceIndex(), request.getHeight());

			hb_buffer_reset(hbBuffer);
			hb_buffer_add_utf16(hbBuffer, stack.UTF16(run.text()), 0, -1);
			hb_buffer_guess_segment_properties(hbBuffer);
			hb_buffer_set_cluster_level(hbBuffer, HB_BUFFER_CLUSTER_LEVEL_MONOTONE_CHARACTERS);
			hb_shape(currentFace.hbFont, hbBuffer, null);

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
					var tempFace = fontData.borrowFaceWithHeight(key.fontIndex, key.height);
					assertFtSuccess(FT_Load_Glyph(tempFace.ftFace, glyph, 0), "FT_Load_Glyph", context);
					var glyphSlot = tempFace.ftFace.glyph();
					if (glyphSlot == null) throw new RuntimeException("Glyph slot should not be null right now");
					var result = new GlyphOffset(glyphSlot.bitmap_left(), glyphSlot.bitmap_top());
					fontData.returnFace(tempFace);
					return result;
				});

				int scale = currentFace.getScale();
				placements.add(new PlacedGlyph(
						new SizedGlyph(glyph, run.faceIndex(), currentFace.getSize(false), scale),
						cursorX + scale * (position.x_offset() + glyphOffset.bitmapLeft),
						cursorY + scale * (position.y_offset() - glyphOffset.bitmapTop + maxAscent),
						request, charIndex
				));

				cursorX += scale * position.x_advance() / 64;
				cursorY += scale * position.y_advance() / 64;

				if (cursorX > request.getWidth() && splitter.wasBaseLeftToRight) break runLoop;
			}

			fontData.returnFace(currentFace);
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
	}

	record GlyphOffsetKey(int height, int fontIndex, int glyph) {}
	record GlyphOffset(int bitmapLeft, int bitmapTop) {}
}
