package com.github.knokko.text.placement;

import com.github.knokko.text.font.FontData;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.harfbuzz.hb_glyph_info_t;
import org.lwjgl.util.harfbuzz.hb_glyph_position_t;

import java.nio.ByteBuffer;
import java.text.Bidi;
import java.util.*;

import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.util.harfbuzz.HarfBuzz.*;

class TextSplitter {

	private final FontData fontData;
	private final long hbBuffer;

	boolean wasBaseLeftToRight;

	TextSplitter(FontData fontData, long hbBuffer) {
		this.fontData = fontData;
		this.hbBuffer = hbBuffer;
	}

	List<TextRun> split(String originalText, int height, MemoryStack stack) {
		Bidi bidi = new Bidi(originalText, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
		wasBaseLeftToRight = bidi.baseIsLeftToRight();
		List<TextRun> runs = new ArrayList<>();

		ByteBuffer originalStringBuffer = stack.UTF16(originalText);

		for (int bidiRun = 0; bidiRun < bidi.getRunCount(); bidiRun++) {
			int runStart = bidi.getRunStart(bidiRun);
			int runLimit = bidi.getRunLimit(bidiRun);
			if (runStart == runLimit) continue;

			List<TextRun> newRuns = splitForRightFace(originalText, originalStringBuffer, height, runStart, runLimit, 0, stack);
			if (bidi.getRunLevel(bidiRun) % 2 == 1) Collections.reverse(newRuns);

			if (wasBaseLeftToRight) runs.addAll(merge(newRuns, stack));
			else runs.addAll(0, merge(newRuns, stack));
		}

		return runs;
	}

	@SuppressWarnings("resource")
	private List<TextRun> merge(List<TextRun> original, MemoryStack stack) {
		List<TextRun> merged = new ArrayList<>();

		for (TextRun run : original) {
			if (merged.isEmpty()) {
				merged.add(run);
				continue;
			}

			TextRun last = merged.get(merged.size() - 1);
			if (last.faceIndex() == run.faceIndex()) {
				merged.remove(merged.size() - 1);
				if (last.glyphInfos() == null || last.glyphPositions() == null) {
					merged.add(new TextRun(last.text() + run.text(), run.faceIndex(), last.offset(), run.glyphInfos(), run.glyphPositions()));
				} else {
					int oldSize = last.glyphInfos().limit();
					int newSize = run.glyphInfos().limit();
					int totalSize = oldSize + newSize;

					hb_glyph_info_t.Buffer mergedInfo;
					hb_glyph_position_t.Buffer mergedPositions;
					if (totalSize > last.glyphInfos().capacity()) {
						mergedInfo = hb_glyph_info_t.calloc(totalSize * 2, stack);
						mergedInfo.limit(totalSize);
						mergedPositions = hb_glyph_position_t.calloc(totalSize * 2, stack);
						mergedPositions.limit(totalSize);
						memCopy(last.glyphInfos().address(), mergedInfo.address(), (long) oldSize * hb_glyph_info_t.SIZEOF);
						memCopy(last.glyphPositions().address(), mergedPositions.address(), (long) oldSize * hb_glyph_position_t.SIZEOF);
					} else {
						mergedInfo = last.glyphInfos().limit(totalSize);
						mergedPositions = last.glyphPositions().limit(totalSize);
					}

					memCopy(run.glyphInfos().address(), mergedInfo.address(oldSize), (long) newSize * hb_glyph_info_t.SIZEOF);
					memCopy(run.glyphPositions().address(), mergedPositions.address(oldSize), (long) newSize * hb_glyph_position_t.SIZEOF);

					merged.add(new TextRun(last.text() + run.text(), run.faceIndex(), last.offset(), mergedInfo, mergedPositions));
				}
			} else merged.add(run);
		}

		return merged;
	}

	private TextRun extractGlyphIntoTextRun(
			String smallString,
			Substring substring,
			hb_glyph_info_t.Buffer originalInfo,
			hb_glyph_position_t.Buffer originalPositions,
			MemoryStack stack
	) {
		int startIndex = 0;
		while (startIndex < originalInfo.limit() && (originalInfo.get(startIndex).cluster() < substring.startIndex || originalInfo.get(startIndex).cluster() >= substring.limit)) {
			startIndex += 1;
		}

		int limit = startIndex;
		while (limit < originalInfo.limit() && originalInfo.get(limit).cluster() < substring.limit && originalInfo.get(limit).cluster() >= substring.startIndex) {
			limit += 1;
		}

		int resultSize = limit - startIndex;
		if (resultSize == 0) return new TextRun(smallString, substring.faceIndex, substring.startIndex, null, null);

		var resultInfo = hb_glyph_info_t.calloc(resultSize, stack);
		memCopy(originalInfo.address(startIndex), resultInfo.address(), (long) resultInfo.capacity() * hb_glyph_info_t.SIZEOF);
		resultInfo.forEach(info -> info.cluster(info.cluster() - substring.startIndex));

		var resultPositions = hb_glyph_position_t.calloc(resultSize, stack);
		memCopy(originalPositions.address(startIndex), resultPositions.address(), (long) resultPositions.capacity() * hb_glyph_position_t.SIZEOF);

		return new TextRun(smallString, substring.faceIndex, substring.startIndex, resultInfo, resultPositions);
	}

	private void updateGlyphInfoAndPositions(
			ByteBuffer originalStringBuffer, int offset, int limit, long hbFont
	) {
		hb_buffer_reset(hbBuffer); // TODO Maybe make hbBuffer part of TextFace
		hb_buffer_add_utf16(hbBuffer, originalStringBuffer, offset, limit - offset);
		hb_buffer_guess_segment_properties(hbBuffer);
		hb_buffer_set_cluster_level(hbBuffer, HB_BUFFER_CLUSTER_LEVEL_MONOTONE_CHARACTERS);
		hb_shape(hbFont, hbBuffer, null);
	}

	private List<TextRun> splitForRightFace(
			String originalString, ByteBuffer originalStringBuffer,
			int height, int offset, int limit, int faceIndex, MemoryStack stack
	) {
		if (limit <= offset) return Collections.emptyList();

		var face = fontData.borrowFaceWithHeightA(faceIndex, height);
		updateGlyphInfoAndPositions(originalStringBuffer, offset, limit, face.hbFont);
		var glyphInfo = Objects.requireNonNull(hb_buffer_get_glyph_infos(hbBuffer));
		fontData.returnFace(face);

		List<Substring> substrings = computeSubstrings(originalString, offset, limit, glyphInfo, faceIndex);
		List<TextRun> runs = new ArrayList<>(substrings.size());
		if (substrings.size() == 1 && substrings.get(0).succeeded) {
			var glyphPositions = Objects.requireNonNull(hb_buffer_get_glyph_positions(hbBuffer));

			String smallString = originalString.substring(substrings.get(0).startIndex(), substrings.get(0).limit());
			runs.add(extractGlyphIntoTextRun(smallString, substrings.get(0), glyphInfo, glyphPositions, stack));
			return runs;
		}

		for (Substring substring : substrings) {

			if (substring.succeeded) {
				runs.addAll(splitForRightFace(
						originalString, originalStringBuffer, height,
						substring.startIndex, substring.limit, faceIndex, stack
				));
			} else {
				if (faceIndex + 1 < fontData.getNumFaces()) {
					runs.addAll(splitForRightFace(
							originalString, originalStringBuffer,
							height, substring.startIndex(), substring.limit(),
							faceIndex + 1, stack
					));
				} else {
					face = fontData.borrowFaceWithHeightA(0, height);
					updateGlyphInfoAndPositions(originalStringBuffer, substring.startIndex, substring.limit, face.hbFont);
					var newGlyphInfo = Objects.requireNonNull(hb_buffer_get_glyph_infos(hbBuffer));
					var glyphPositions = Objects.requireNonNull(hb_buffer_get_glyph_positions(hbBuffer));
					fontData.returnFace(face);

					String smallString = originalString.substring(substring.startIndex(), substring.limit());
					runs.add(extractGlyphIntoTextRun(
							smallString, new Substring(substring.startIndex, substring.limit, 0, false),
							newGlyphInfo, glyphPositions, stack
					));
				}
			}
		}
		return runs;
	}

	private List<Substring> computeSubstrings(
			String originalString, int offset, int limit,
			hb_glyph_info_t.Buffer glyphs, int faceIndex
	) {
		if (glyphs == null) throw new RuntimeException("Glyph info is null");

		boolean[] supported = new boolean[limit - offset];
		if (supported.length == 0) return Collections.emptyList();
		Arrays.fill(supported, true);

		for (int index = 0; index < glyphs.limit(); index++) {
			var glyph = glyphs.get(index);
			int cluster = glyph.cluster() - offset;

			// Not sure why harfbuzz does this, but let's just ignore it
			if (cluster >= supported.length) continue;

			if (glyph.codepoint() == 0) {
				supported[cluster] = false;
				int codepoint = originalString.codePointAt(offset + cluster);
				if (Character.isSupplementaryCodePoint(codepoint)) {
					supported[cluster + 1] = false;
				}
				if ((glyph.mask() & HB_GLYPH_FLAG_UNSAFE_TO_BREAK) != 0) {
					supported[cluster - 1] = false;
				}
			}
		}

		int startIndex = 0;
		boolean wasSupported = supported[0];
		List<Substring> substrings = new ArrayList<>();

		for (int index = 0; index < supported.length; index++) {
			if (supported[index] == wasSupported) continue;

			substrings.add(new Substring(startIndex + offset, index + offset, faceIndex, wasSupported));
			startIndex = index;
			wasSupported = !wasSupported;
		}

		substrings.add(new Substring(startIndex + offset, supported.length + offset, faceIndex, wasSupported));
		return substrings;
	}

	private record Substring(int startIndex, int limit, int faceIndex, boolean succeeded) {}
}
