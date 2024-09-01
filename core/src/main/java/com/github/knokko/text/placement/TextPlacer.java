package com.github.knokko.text.placement;

import com.github.knokko.text.SizedGlyph;
import com.github.knokko.text.font.FontData;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.freetype.FreeType.*;

public class TextPlacer {

	private final FontData fontData;
	private final ConcurrentHashMap<GlyphOffsetKey, GlyphOffset> glyphOffsets = new ConcurrentHashMap<>(); // TODO Throw old entries away
	private final ConcurrentSkipListSet<ByteBuffer> allocations = new ConcurrentSkipListSet<>((a, b) -> {
		if (a.capacity() > b.capacity()) return 1;
		if (a.capacity() < b.capacity()) return -1;
		return Long.compare(memAddress(a), memAddress(b));
	});

	public TextPlacer(FontData font) {
		this.fontData = font;
	}

	public Stream<PlacedGlyph> place(Stream<TextPlaceRequest> requests) {
		return requests.sorted().flatMap(request -> {
			double sizeFactor = ((request.text.length() + 1) * Math.log(request.text.length() + Math.E));
			int requiredSize = (int) (250 * sizeFactor);

			while (allocations.size() > 4) {
				ByteBuffer smallest = allocations.pollFirst();
				if (smallest != null) memFree(smallest);
			}

			ByteBuffer stackBuffer = allocations.pollLast();
			if (stackBuffer == null || stackBuffer.capacity() < requiredSize) {
				if (stackBuffer != null) allocations.add(stackBuffer);
				stackBuffer = memCalloc(requiredSize);
			}

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
				allocations.add(stackBuffer);
			}
		});
	}

	@SuppressWarnings("resource")
	private List<PlacedGlyph> placeFree(TextPlaceRequest request, MemoryStack stack) {
		var splitter = new TextSplitter(fontData);
		List<TextRun> runs = splitter.split(request.text, request.heightA, stack);
		List<PlacedGlyph> placements = new ArrayList<>();

		int cursorX = 0;
		int cursorY = 0;
		int previousRsbDelta = 0;

		runLoop:
		for (TextRun run : runs) {
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
						String context = "face=" + tempFace.ftFace + ", glyph=" + key.glyph + ", string=" + run.text();
						assertFtSuccess(FT_Load_Glyph(tempFace.ftFace, key.glyph, FT_LOAD_BITMAP_METRICS_ONLY), "FT_Load_Glyph", context);
						var glyphSlot = tempFace.ftFace.glyph();
						if (glyphSlot == null) throw new RuntimeException("Glyph slot should not be null right now");
						var result = new GlyphOffset(
								glyphSlot.bitmap_left(), glyphSlot.bitmap_top(),
								(int) glyphSlot.lsb_delta(), (int) glyphSlot.rsb_delta()
						);
						fontData.returnFace(tempFace);
						return result;
					});

					if (previousRsbDelta - glyphOffset.lsbDelta > 32) cursorX -= 64;
					else if (previousRsbDelta - glyphOffset.lsbDelta < -31) cursorX += 64;

					previousRsbDelta = glyphOffset.rsbDelta;

					int scale = currentFace.scale;
					placements.add(new PlacedGlyph(
							new SizedGlyph(glyph, run.faceIndex(), currentFace.fontSize, scale),
							cursorX / 64 + scale * (position.x_offset() + glyphOffset.bitmapLeft),
							cursorY / 64 + scale * (position.y_offset() - glyphOffset.bitmapTop),
							request, charIndex
					));

					cursorX += scale * position.x_advance();
					cursorY += scale * position.y_advance();

					if (cursorX > 64 * request.getWidth() && splitter.wasBaseLeftToRight) break runLoop;
				}
			}

			fontData.returnFace(currentFace);
		}

		if (!splitter.wasBaseLeftToRight) {
			int shift = request.getWidth() - cursorX / 64;
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
		for (var buffer : allocations) memFree(buffer);
		allocations.clear();
	}

	record GlyphOffsetKey(int heightA, int fontIndex, int glyph) {}
	record GlyphOffset(int bitmapLeft, int bitmapTop, int lsbDelta, int rsbDelta) {}
}
