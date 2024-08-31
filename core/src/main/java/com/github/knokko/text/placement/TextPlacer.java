package com.github.knokko.text.placement;

import com.github.knokko.text.SizedGlyph;
import com.github.knokko.text.font.FontData;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.lwjgl.system.MemoryUtil.memCalloc;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.util.freetype.FreeType.FT_Load_Glyph;
import static org.lwjgl.util.harfbuzz.HarfBuzz.*;

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
			double sizeFactor = ((request.text.length() + 1) * Math.log(request.text.length() + Math.E));
			var stackBuffer = memCalloc((int) (250 * sizeFactor));
			try {
				var stack = MemoryStack.create(stackBuffer);
				return placeFree(request, stack).stream().map(placement -> new PlacedGlyph(
						placement.glyph,
						request.minX + placement.minX,
						request.baseY + placement.minY,
						placement.request,
						placement.charIndex
				));
			} finally {
				memFree(stackBuffer);
			}
		});
	}

	@SuppressWarnings("resource")
	private List<PlacedGlyph> placeFree(TextPlaceRequest request, MemoryStack stack) {
		List<TextRun> runs = splitter.split(request.text, request.heightA, stack);
		List<PlacedGlyph> placements = new ArrayList<>();

		int cursorX = 0;
		int cursorY = 0;

		runLoop:
		for (TextRun run : runs) {
			String context = "TextPlacer.placeFree(run = " + run.text() + ")";
			var currentFace = fontData.borrowFaceWithHeightA(run.faceIndex(), request.heightA);

			if (run.glyphInfos() != null && run.glyphPositions() != null) {
				for (int glyphIndex = 0; glyphIndex < run.glyphPositions().limit(); glyphIndex++) {
					var position = run.glyphPositions().get(glyphIndex);
					var info = run.glyphInfos().get(glyphIndex);

					int charIndex = info.cluster() + run.offset();
					if (info.cluster() >= run.text().length()) continue;

					int glyph = info.codepoint();
					var glyphOffset = glyphOffsets.computeIfAbsent(new GlyphOffsetKey(request.heightA, run.faceIndex(), glyph), key -> {
						var tempFace = fontData.borrowFaceWithHeightA(key.fontIndex, key.heightA);
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
							cursorY + scale * (position.y_offset() - glyphOffset.bitmapTop),
							request, charIndex
					));

					cursorX += scale * position.x_advance() / 64;
					cursorY += scale * position.y_advance() / 64;

					if (cursorX > request.getWidth() && splitter.wasBaseLeftToRight) break runLoop;
				}
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

	record GlyphOffsetKey(int heightA, int fontIndex, int glyph) {}
	record GlyphOffset(int bitmapLeft, int bitmapTop) {}
}
