package com.github.knokko.text.placement;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.harfbuzz.hb_glyph_info_t;

import java.nio.ByteBuffer;
import java.text.Bidi;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.util.harfbuzz.HarfBuzz.*;

class TextSplitter {

	private final long hbBuffer;
	private final long[] hbFonts;

	boolean wasBaseLeftToRight;

	TextSplitter(long hbBuffer, long[] hbFonts) {
		this.hbBuffer = hbBuffer;
		this.hbFonts = hbFonts;
	}

	List<TextRun> split(String originalText, MemoryStack stack) {
		Bidi bidi = new Bidi(originalText, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
		wasBaseLeftToRight = bidi.baseIsLeftToRight();
		List<TextRun> runs = new ArrayList<>();

		ByteBuffer originalStringBuffer = stack.UTF16(originalText);

		for (int bidiRun = 0; bidiRun < bidi.getRunCount(); bidiRun++) {
			int runStart = bidi.getRunStart(bidiRun);
			int runLimit = bidi.getRunLimit(bidiRun);
			if (runStart == runLimit) continue;

			List<TextRun> newRuns = splitForRightFace(originalText, originalStringBuffer, runStart, runLimit, 0);
			if (bidi.getRunLevel(bidiRun) % 2 == 1) Collections.reverse(newRuns);

			if (wasBaseLeftToRight) runs.addAll(merge(newRuns));
			else runs.addAll(0, merge(newRuns));
		}

		return runs;
	}

	private List<TextRun> merge(List<TextRun> original) {
		List<TextRun> merged = new ArrayList<>();

		for (TextRun run : original) {
			if (merged.isEmpty()) {
				merged.add(run);
				continue;
			}

			TextRun last = merged.get(merged.size() - 1);
			if (last.faceIndex() == run.faceIndex()) {
				merged.remove(merged.size() - 1);
				merged.add(new TextRun(last.text() + run.text(), run.faceIndex(), last.offset()));
			} else merged.add(run);
		}

		return merged;
	}

	private List<TextRun> splitForRightFace(
			String originalString, ByteBuffer originalStringBuffer,
			int offset, int limit, int faceIndex
	) {
		if (limit <= offset) return Collections.emptyList();

		hb_buffer_reset(hbBuffer);
		hb_buffer_add_utf16(hbBuffer, originalStringBuffer, offset, limit - offset);
		hb_buffer_guess_segment_properties(hbBuffer);
		hb_buffer_set_cluster_level(hbBuffer, HB_BUFFER_CLUSTER_LEVEL_MONOTONE_CHARACTERS);
		hb_shape(hbFonts[faceIndex], hbBuffer, null);
		var glyphInfo = hb_buffer_get_glyph_infos(hbBuffer);
		List<Substring> substrings = computeSubstrings(originalString, offset, limit, glyphInfo, faceIndex);

		List<TextRun> runs = new ArrayList<>(substrings.size());
		for (Substring substring : substrings) {
			String smallString = originalString.substring(substring.startIndex(), substring.limit());

			if (substring.succeeded()) runs.add(new TextRun(smallString, faceIndex, substring.startIndex()));
			else if (faceIndex + 1 < hbFonts.length) runs.addAll(splitForRightFace(
					originalString, originalStringBuffer, substring.startIndex(), substring.limit(), faceIndex + 1
			));
			else runs.add(new TextRun(smallString, 0, substring.startIndex()));
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
