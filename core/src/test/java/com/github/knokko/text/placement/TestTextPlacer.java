package com.github.knokko.text.placement;

import com.github.knokko.text.TextInstance;
import com.github.knokko.text.font.ClasspathFontsSource;
import com.github.knokko.text.font.UnicodeFonts;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestTextPlacer {

	@Test
	public void testWrongFontRegressionItalic() {
		var instance = new TextInstance();
		var font = instance.createFont(new ClasspathFontsSource(
				"fonts/unicode-freeserif.ttf", "fonts/unicode-quivira.ttf"
		));
		var placer = new TextPlacer(font);

		List<TextPlaceRequest> requests = new ArrayList<>();
		String oldItalicText = "𐌀 𐌁 𐌂 𐌃 𐌄 𐌅 𐌆 𐌇 𐌈 𐌉 𐌊 𐌋 𐌌 𐌍 𐌎 𐌏 𐌐 𐌑 𐌒 𐌓 𐌔 𐌕 𐌖 𐌗 𐌘 𐌙 𐌚 𐌛 𐌜 𐌝 𐌞 𐌠 𐌡 𐌢 𐌣";
		requests.add(new TextPlaceRequest(oldItalicText, 0, 5, 5000, 45, true, null));

		// Note that unicode-quivira supports old italic characters, whereas unicode-freeserif does not.
		// So unicode-freeserif should render all whitespaces (because it is the first font), but it should
		// fall back to unicode-quivira to render the actual symbols
		var result = placer.place(requests);

		// There are 34 whitespaces, which should be rendered with face 0
		assertEquals(34, result.stream().filter(placedGlyph -> placedGlyph.glyph.faceIndex == 0).count());

		// There are 35 old italic characters, which should be rendered with face 1
		assertEquals(35, result.stream().filter(placedGlyph -> placedGlyph.glyph.faceIndex == 1).count());

		assertEquals(34 + 35, result.size());

		for (var placedGlyph : result) {
			int glyphChar = oldItalicText.codePointAt(placedGlyph.charIndex);
			if (placedGlyph.glyph.faceIndex == 0) assertEquals(' ', glyphChar);
			else assertTrue(Character.isSupplementaryCodePoint(glyphChar));
		}

		placer.destroy();
		font.destroy();
		instance.destroy();
	}

	@Test
	public void testWrongFontRegressionSyriac() {
		var instance = new TextInstance();
		var font = instance.createFont(new ClasspathFontsSource("fonts/thaana.ttf", "fonts/syriac.otf"));
		var placer = new TextPlacer(font);

		List<TextPlaceRequest> requests = new ArrayList<>();
		String syriacText = "܀ ܁ ܂ ܃ ܄ ܅ ܆ ܇ ܈ ܉ ܊ ܋ ܌ ܍ ܏ ܐ ܑ ܒ ܓ ܔ ܕ ܖ ܗ ܘ ܙ ܚ ܛ ܜ ܝ ܞ ܟ ܠ ܡ ܢ ܣ";
		requests.add(new TextPlaceRequest(syriacText, 0, 5, 5000, 45, true, null));

		// Since the thaana font doesn't support the syriac characters, the fallback syriac font must be used to
		// render the syriac characters, whereas the primary thaana font renders the whitespaces.
		var result = placer.place(requests);

		// There are 34 whitespaces, of which 33 should be rendered with thaana.
		// The last one is rendered with syriac because it's unsafe to break.
		assertEquals(33, result.stream().filter(placedGlyph -> placedGlyph.glyph.faceIndex == 0).count());

		// The 35 syriac characters should be rendered with syriac, as well as the one whitespace character that is
		// unsafe to break.
		assertEquals(36, result.stream().filter(placedGlyph -> placedGlyph.glyph.faceIndex == 1).count());

		assertEquals(33 + 36, result.size());

		// The entire text should be right-to-left
		for (int index = 1; index < result.size(); index++) {
			assertTrue(result.get(index - 1).charIndex > result.get(index).charIndex);
		}

		for (var placedGlyph : result) {
			int placedChar = syriacText.codePointAt(placedGlyph.charIndex);
			if (placedGlyph.glyph.faceIndex == 0) assertEquals(' ', placedChar);
			else assertNotEquals(Character.DIRECTIONALITY_LEFT_TO_RIGHT, Character.getDirectionality(placedChar));
		}

		placer.destroy();
		font.destroy();
		instance.destroy();
	}

	@Test
	public void testWrongFontRegressionTagalog() {
		var instance = new TextInstance();
		var font = instance.createFont(new ClasspathFontsSource(
				"fonts/unicode-freeserif.ttf",
				"fonts/unicode-quivira.ttf"
		));
		var placer = new TextPlacer(font);

		String tagalogText = "ᜀ ᜁ ᜂ ᜃ ᜄ ᜅ ᜆ ᜇ ᜈ ᜉ ᜊ ᜋ ᜌ ᜎ ᜏ ᜐ ᜑ ᜒ ᜓ";

		List<TextPlaceRequest> requests = new ArrayList<>();
		requests.add(new TextPlaceRequest(tagalogText, 10, 10, 500, 40, true, null));

		var result = placer.place(requests);
		assertEquals(37, result.size());

		// The last 5 glyphs must NOT be broken up because they are part of the same cluster
		for (int index = 32; index < 37; index++) {
			assertEquals(1, result.get(index).glyph.faceIndex);
		}

		placer.destroy();
		font.destroy();
		instance.destroy();
	}

	@Test
	public void regressionHiInsideArabic() {
		var instance = new TextInstance();
		var font = instance.createFont(new ClasspathFontsSource("fonts/unicode-polyglott.ttf"));
		var placer = new TextPlacer(font);

		List<TextPlaceRequest> requests = new ArrayList<>();
		requests.add(new TextPlaceRequest("ؤلاششششششش" + "  hi  " + "يييييييثب", 10, 50, 490, 80, true, null));

		var result = placer.place(requests);
		assertEquals(24, result.size());

		assertEquals(24, result.get(0).charIndex);
		assertEquals(1382, result.get(0).glyph.id);
		assertEquals(23, result.get(1).charIndex);
		assertEquals(4604, result.get(1).glyph.id);
		assertEquals(22, result.get(2).charIndex);
		assertEquals(4692, result.get(2).glyph.id);
		assertEquals(21, result.get(3).charIndex);
		assertEquals(4692, result.get(3).glyph.id);

		assertEquals(16, result.get(8).charIndex);
		assertEquals(4691, result.get(8).glyph.id);
		assertEquals(15, result.get(9).charIndex);
		assertEquals(3, result.get(9).glyph.id);
		assertEquals(14, result.get(10).charIndex);
		assertEquals(3, result.get(10).glyph.id);

		assertEquals(12, result.get(11).charIndex);
		assertEquals(75, result.get(11).glyph.id);
		assertEquals(13, result.get(12).charIndex);
		assertEquals(76, result.get(12).glyph.id);

		assertEquals(11, result.get(13).charIndex);
		assertEquals(3, result.get(13).glyph.id);
		assertEquals(10, result.get(14).charIndex);
		assertEquals(3, result.get(14).glyph.id);

		assertEquals(9, result.get(15).charIndex);
		assertEquals(1394, result.get(15).glyph.id);
		assertEquals(8, result.get(16).charIndex);
		assertEquals(4632, result.get(16).glyph.id);

		assertEquals(3, result.get(21).charIndex);
		assertEquals(4631, result.get(21).glyph.id);
		assertEquals(1, result.get(22).charIndex);
		assertEquals(4699, result.get(22).glyph.id);
		assertEquals(0, result.get(23).charIndex);
		assertEquals(1378, result.get(23).glyph.id);

		placer.destroy();
		font.destroy();
		instance.destroy();
	}

	@Test
	public void testOffsetRegressionGujarati() {
		var instance = new TextInstance();
		var font = instance.createFont(UnicodeFonts.SOURCE);
		var placer = new TextPlacer(font);

		String gujaratiText = "૬ ૭ ૮ ૯";

		List<TextPlaceRequest> requests = new ArrayList<>();
		requests.add(new TextPlaceRequest(gujaratiText, 10, 10, 500, 40, true, null));

		var result = placer.place(requests);
		for (var placed : result) {
			assertNotEquals(0, placed.glyph.id);
		}

		placer.destroy();
		font.destroy();
		instance.destroy();
	}
}
