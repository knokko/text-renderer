package com.github.knokko.text.renderer.cpu;

import com.github.knokko.text.TextInstance;
import com.github.knokko.text.font.ClasspathFontsSource;
import com.github.knokko.text.font.FontData;
import com.github.knokko.text.font.UnicodeFonts;
import com.github.knokko.text.placement.TextAlignment;
import com.github.knokko.text.placement.TextPlaceRequest;
import com.github.knokko.text.util.UnicodeLines;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.knokko.text.util.ImageChecks.assertImageEquals;

public class TestBufferedImageRenderer {

	@Test
	public void testPartialHebrew() {
		var instance = new TextInstance();
		var font = new FontData(instance, new ClasspathFontsSource("fonts/unicode-polyglott.ttf"));
		var renderer = new BufferedImageTextRenderer(
				new BufferedImage(500, 130, BufferedImage.TYPE_INT_RGB),
				font, 10_000
		);

		List<TextPlaceRequest> requests = new ArrayList<>();
		requests.add(new TextPlaceRequest(
				"hello world", 10, 10, 200, 40,
				34, 18, 1, TextAlignment.DEFAULT, null
		));
		requests.add(new TextPlaceRequest(
				"אלט", 210, 10, 300, 40,
				34, 18, 1, TextAlignment.DEFAULT, null
		));
		requests.add(new TextPlaceRequest(
				"ؤلاششششششش" + "  hi  " + "يييييييثب", 10, 50, 490, 80,
				74, 18, 1, TextAlignment.DEFAULT, null
		));
		requests.add(new TextPlaceRequest(
				"(Only) 1 word (אלט) is Hebrew", 10, 90, 490, 120,
				114, 18, 1, TextAlignment.DEFAULT, null
		));
		renderer.render(requests);

		assertImageEquals(
				"expected-english-hebrew-mix.png",
				renderer.image,
				"actual-english-hebrew-mix.png",
				true
		);

		renderer.destroy();
		font.destroy();
		instance.destroy();
	}

	private List<TextPlaceRequest> shift(List<TextPlaceRequest> original, int index) {
		var old = original.get(index);
		return Collections.singletonList(new TextPlaceRequest(
				old.text, 1000 + old.minX, old.minY, 1000 + old.maxX, old.maxY,
				old.baseY, old.heightA, old.minScale, old.alignment, old.userData
		));
	}

	private void addUnicodeRequest(List<TextPlaceRequest> unicodeRequests, String text) {
		int offsetY = 100 * unicodeRequests.size();
		unicodeRequests.add(new TextPlaceRequest(
				text, 10, offsetY + 10, 1000, offsetY + 90,
				offsetY + 60, 30, 1, TextAlignment.DEFAULT, null
		));
	}

	@Test
	@SuppressWarnings({"UnnecessaryUnicodeEscape", "SpellCheckingInspection"})
	public void testLargeAscentsAndDescents() {
		var instance = new TextInstance();
		var unicodeFont = new FontData(instance, UnicodeFonts.SOURCE);
		var freeserifFont = new FontData(instance, new ClasspathFontsSource("fonts/unicode-freeserif.ttf"));
		var quiviraFont = new FontData(instance, new ClasspathFontsSource("fonts/unicode-quivira.ttf"));
		var polyglottFont = new FontData(instance, new ClasspathFontsSource("fonts/unicode-polyglott.ttf"));
		var cjkFont = new FontData(instance, new ClasspathFontsSource("fonts/cjk.ttf"));
		var thaanaFont = new FontData(instance, new ClasspathFontsSource("fonts/thaana.ttf"));
		var syriacFont = new FontData(instance, new ClasspathFontsSource("fonts/syriac.otf"));
		var gujaratiFont = new FontData(instance, new ClasspathFontsSource("fonts/gujarati.ttf"));
		var oriyaFont = new FontData(instance, new ClasspathFontsSource("fonts/oriya.ttf"));
		var teluguFont = new FontData(instance, new ClasspathFontsSource("fonts/telugu.otf"));
		var kannadaFont = new FontData(instance, new ClasspathFontsSource("fonts/kannada.ttf"));
		var tibetanFont = new FontData(instance, new ClasspathFontsSource("fonts/tibetan.ttf"));
		var myanmarFont = new FontData(instance, new ClasspathFontsSource("fonts/myanmar.ttf"));
		var hangulFont = new FontData(instance, new ClasspathFontsSource("fonts/hangul.ttf"));
		var mongolianFont = new FontData(instance, new ClasspathFontsSource("fonts/mongolian.ttf"));
		var yiFont = new FontData(instance, new ClasspathFontsSource("fonts/yi.ttf"));
		var marksFont = new FontData(instance, new ClasspathFontsSource("fonts/marks.ttf"));

		BufferedImage targetImage = new BufferedImage(2000, 3000, BufferedImage.TYPE_INT_RGB);
		for (int y = 10; y < targetImage.getHeight(); y += 100) {
			for (int x = 0; x < targetImage.getWidth(); x++) {
				targetImage.setRGB(x, y, Color.RED.getRGB());
			}
		}
		for (int y = 90; y < targetImage.getHeight(); y += 100) {
			for (int x = 0; x < targetImage.getWidth(); x++) {
				targetImage.setRGB(x, y, Color.RED.getRGB());
			}
		}

		int glyphBufferCapacity = 75_000;

		List<TextPlaceRequest> unicodeRequests = new ArrayList<>();
		addUnicodeRequest(unicodeRequests, "aaa\u090Daaa\u0650aaa\u064Faaa");
		addUnicodeRequest(unicodeRequests, "aaa\u23B0aaa\u1713aaa\u1699aaa");
		addUnicodeRequest(unicodeRequests, "aaa\u06B3aaa\u065Caaa\u08D8aaa");
		addUnicodeRequest(unicodeRequests, "aaa\u3031aaa\u302Aaaa\u3031aaa");
		addUnicodeRequest(unicodeRequests, "aaa\u07B1aaa\u07A8aaa\u07B1aaa");
		addUnicodeRequest(unicodeRequests, "aaa\u071Baaa\u0737aaa\u070Faaa");
		addUnicodeRequest(unicodeRequests, "aaa\u0A90aaa\u0ABCaaa\u0A81aaa");
		addUnicodeRequest(unicodeRequests, "aaa\u0B14aaa\u0B3Caaa\u0B01aaa");
		addUnicodeRequest(unicodeRequests, "aaa\u0C58aaa\u1CDDaaa\u1CDAaaa");
		addUnicodeRequest(unicodeRequests, "aaa\u0CC8aaa\u0CBCaaa\u0CBFaaa");
		addUnicodeRequest(unicodeRequests, "aaa" + Character.toString(988802) + "aaa\uE298aaa\uE2BFaaa");
		addUnicodeRequest(unicodeRequests, "aaa\uA9E8aaa\u108Daaa\uA9E8aaa");
		addUnicodeRequest(unicodeRequests, "aaa\uC61Baaa\u1160aaa\u111Aaaa");
		addUnicodeRequest(unicodeRequests, "aaa\u1840aaa\u18A9aaa\u1886aaa");
		addUnicodeRequest(unicodeRequests, "aaa\uA30Daaa\uA4A2aaa\uA022aaa");
		addUnicodeRequest(unicodeRequests, "aaa\u2E3Daaa\u1AB5aaa\u1DD8aaa");

		new BufferedImageTextRenderer(targetImage, unicodeFont, glyphBufferCapacity).render(unicodeRequests);

		new BufferedImageTextRenderer(targetImage, freeserifFont, glyphBufferCapacity).render(shift(unicodeRequests, 0));
		new BufferedImageTextRenderer(targetImage, quiviraFont, glyphBufferCapacity).render(shift(unicodeRequests, 1));
		new BufferedImageTextRenderer(targetImage, polyglottFont, glyphBufferCapacity).render(shift(unicodeRequests, 2));
		new BufferedImageTextRenderer(targetImage, cjkFont, glyphBufferCapacity).render(shift(unicodeRequests, 3));
		new BufferedImageTextRenderer(targetImage, thaanaFont, glyphBufferCapacity).render(shift(unicodeRequests, 4));
		new BufferedImageTextRenderer(targetImage, syriacFont, glyphBufferCapacity).render(shift(unicodeRequests, 5));
		new BufferedImageTextRenderer(targetImage, gujaratiFont, glyphBufferCapacity).render(shift(unicodeRequests, 6));
		new BufferedImageTextRenderer(targetImage, oriyaFont, glyphBufferCapacity).render(shift(unicodeRequests, 7));
		new BufferedImageTextRenderer(targetImage, teluguFont, glyphBufferCapacity).render(shift(unicodeRequests, 8));
		new BufferedImageTextRenderer(targetImage, kannadaFont, glyphBufferCapacity).render(shift(unicodeRequests, 9));
		new BufferedImageTextRenderer(targetImage, tibetanFont, glyphBufferCapacity).render(shift(unicodeRequests, 10));
		new BufferedImageTextRenderer(targetImage, myanmarFont, glyphBufferCapacity).render(shift(unicodeRequests, 11));
		new BufferedImageTextRenderer(targetImage, hangulFont, glyphBufferCapacity).render(shift(unicodeRequests, 12));
		new BufferedImageTextRenderer(targetImage, mongolianFont, glyphBufferCapacity).render(shift(unicodeRequests, 13));
		new BufferedImageTextRenderer(targetImage, yiFont, glyphBufferCapacity).render(shift(unicodeRequests, 14));
		new BufferedImageTextRenderer(targetImage, marksFont, glyphBufferCapacity).render(shift(unicodeRequests, 15));

		assertImageEquals(
				"expected-ascents-and-descents.png",
				targetImage,
				"actual-ascents-and-descents.png",
				true
		);

		unicodeFont.destroy();
		freeserifFont.destroy();
		quiviraFont.destroy();
		polyglottFont.destroy();
		cjkFont.destroy();
		thaanaFont.destroy();
		syriacFont.destroy();
		gujaratiFont.destroy();
		oriyaFont.destroy();
		teluguFont.destroy();
		kannadaFont.destroy();
		tibetanFont.destroy();
		myanmarFont.destroy();
		hangulFont.destroy();
		mongolianFont.destroy();
		yiFont.destroy();
		marksFont.destroy();
		instance.destroy();
	}

	@Test
	public void unicodeTestCase() {
		var instance = new TextInstance();
		var font = new FontData(instance, UnicodeFonts.SOURCE);
		var renderer = new BufferedImageTextRenderer(
				new BufferedImage(3500, 9700, BufferedImage.TYPE_INT_RGB),
				font, 3_000_000
		);

		List<TextPlaceRequest> requests = new ArrayList<>();
		int minY = 5;
		for (String line : UnicodeLines.get()) {
			int maxY = minY + 40;
			requests.add(new TextPlaceRequest(
					line, 0, minY, renderer.image.getWidth(), maxY,
					minY + 20, 15, 1, TextAlignment.DEFAULT, null
			));
			minY = maxY;
		}

		renderer.render(requests);

		assertImageEquals(
				"expected-unicode-test-result.png",
				renderer.image,
				"actual-unicode-test-result.png",
				true
		);

		renderer.destroy();
		font.destroy();
		instance.destroy();
	}

	@Test
	public void unicodeTestCaseInMultipleFrames() {
		var instance = new TextInstance();
		var font = new FontData(instance, UnicodeFonts.SOURCE);
		var renderer = new BufferedImageTextRenderer(
				new BufferedImage(3500, 9700, BufferedImage.TYPE_INT_RGB),
				font, 90_000
		);

		List<TextPlaceRequest> requests = new ArrayList<>(1);
		int minY = 5;
		for (String line : UnicodeLines.get()) {
			int maxY = minY + 40;
			requests.add(new TextPlaceRequest(
					line, 0, minY, renderer.image.getWidth(), maxY,
					minY + 20, 15, 1, TextAlignment.DEFAULT, null
			));
			renderer.render(requests);
			requests.clear();
			minY = maxY;
		}

		assertImageEquals(
				"expected-unicode-test-result.png", renderer.image,
				"actual-unicode-test-result-multiple-frames.png", true
		);

		renderer.destroy();
		font.destroy();
		instance.destroy();
	}

	@Test
	public void testVeryLargeText() {
		var instance = new TextInstance();
		var font = new FontData(instance, UnicodeFonts.SOURCE);
		font.setMaxHeight(300);
		var renderer = new BufferedImageTextRenderer(
				new BufferedImage(11000, 3000, BufferedImage.TYPE_INT_RGB),
				font, 1_000_000
		);

		var requests = new ArrayList<TextPlaceRequest>();
		int minX = 0;
		int minY = 0;
		for (int height = 1500; height < 5000; height += 900) {
			requests.add(new TextPlaceRequest(
					"Big", minX, minY, minX + height, minY + height,
					minY + height / 2, height * 35 / 100, 1, TextAlignment.DEFAULT, null
			));
			// noinspection SuspiciousNameCombination
			minX += height;
			if (minX > renderer.image.getWidth()) {
				minX = 0;
				minY += height;
			}
		}

		renderer.render(requests);

		assertImageEquals(
				"expected-very-large-text.png", renderer.image,
				"actual-very-large-text.png", true
		);

		renderer.destroy();
		font.destroy();
		instance.destroy();
	}

	@Test
	public void setMaxHeight() {
		var instance = new TextInstance();
		var font = new FontData(instance, UnicodeFonts.SOURCE);
		font.setMaxHeight(38);
		var renderer = new BufferedImageTextRenderer(
				new BufferedImage(4000, 300, BufferedImage.TYPE_INT_RGB),
				font, 50_000
		);

		var requests = new ArrayList<TextPlaceRequest>();
		int minX = 0;
		int minY = 0;
		for (int height = 20; height < 500; height += 30) {
			requests.add(new TextPlaceRequest(
					"Big", minX, minY, minX + height, minY + height,
					minY + (int) (height * 0.48), height * 38 / 100, 1, TextAlignment.DEFAULT, null
			));
			// noinspection SuspiciousNameCombination
			minX += height;
			if (minX > renderer.image.getWidth()) {
				minX = 0;
				minY += height;
			}
		}

		renderer.render(requests);

		assertImageEquals(
				"expected-large-text.png", renderer.image,
				"actual-large-text.png", true
		);

		renderer.destroy();
		font.destroy();
		instance.destroy();
	}

	@Test
	public void testVerySmallText() {
		var instance = new TextInstance();
		var font = new FontData(instance, new ClasspathFontsSource("fonts/unicode-polyglott.ttf"));
		var renderer = new BufferedImageTextRenderer(
				new BufferedImage(50, 10, BufferedImage.TYPE_INT_RGB),
				font, 1_000
		);

		List<TextPlaceRequest> requests = new ArrayList<>();
		requests.add(new TextPlaceRequest(
				"hello", 0, 0, 10, 9, 7, 0, 1, TextAlignment.DEFAULT, null
		));
		requests.add(new TextPlaceRequest(
				"hello", 10, 0, 20, 9, 7, 1, 1, TextAlignment.DEFAULT, null
		));
		requests.add(new TextPlaceRequest(
				"hello", 20, 0, 40, 9, 7, 2, 1, TextAlignment.DEFAULT, null
		));
		renderer.render(requests);

		assertImageEquals(
				"expected-very-small-text.png",
				renderer.image,
				"actual-very-small-text.png",
				true
		);

		renderer.destroy();
		font.destroy();
		instance.destroy();
	}

	@Test
	public void testMinScale() {
		var instance = new TextInstance();
		var font = new FontData(instance, new ClasspathFontsSource("fonts/unicode-polyglott.ttf"));
		var renderer = new BufferedImageTextRenderer(
				new BufferedImage(180, 40, BufferedImage.TYPE_INT_RGB),
				font, 10_000
		);

		List<TextPlaceRequest> requests = new ArrayList<>();
		requests.add(new TextPlaceRequest(
				"hi", 0, 0, 60, 39, 37, 35, 1, TextAlignment.DEFAULT, null
		));
		requests.add(new TextPlaceRequest(
				"hi", 60, 0, 120, 39, 37, 35, 2, TextAlignment.DEFAULT, null
		));
		requests.add(new TextPlaceRequest(
				"hi", 120, 0, 180, 39, 37, 35, 3, TextAlignment.DEFAULT, null
		));
		renderer.render(requests);

		assertImageEquals(
				"expected-min-scale.png",
				renderer.image,
				"actual-min-scale.png",
				true
		);

		renderer.destroy();
		font.destroy();
		instance.destroy();
	}

	@Test
	public void testTextAlignment() {
		var instance = new TextInstance();
		var font = new FontData(instance, new ClasspathFontsSource("fonts/unicode-polyglott.ttf"));
		var renderer = new BufferedImageTextRenderer(
				new BufferedImage(500, 280, BufferedImage.TYPE_INT_RGB),
				font, 10_000
		);

		List<TextPlaceRequest> requests = new ArrayList<>();
		requests.add(new TextPlaceRequest(
				"hello", 0, 0, 200, 39,
				37, 35, 1, TextAlignment.REVERSED, null
		));
		requests.add(new TextPlaceRequest(
				"hello", 0, 40, 200, 79,
				77, 35, 1, TextAlignment.LEFT, null
		));
		requests.add(new TextPlaceRequest(
				"hello", 0, 80, 200, 119,
				117, 35, 1, TextAlignment.RIGHT, null
		));
		requests.add(new TextPlaceRequest(
				"hellohello", 0, 120, 200, 159,
				157, 35, 1, TextAlignment.REVERSED, null
		));
		requests.add(new TextPlaceRequest(
				"hellohello", 0, 160, 200, 199,
				197, 35, 1, TextAlignment.DEFAULT, null
		));
		requests.add(new TextPlaceRequest(
				"hello", 0, 200, 200, 239,
				237, 35, 1, TextAlignment.CENTER, null
		));
		requests.add(new TextPlaceRequest(
				"hellohello", 0, 240, 200, 279,
				277, 35, 1, TextAlignment.CENTER, null
		));

		requests.add(new TextPlaceRequest(
				"مرحباً", 300, 0, 499, 39,
				32, 25, 1, TextAlignment.REVERSED, null
		));
		requests.add(new TextPlaceRequest(
				"مرحباً", 300, 40, 499, 79,
				72, 25, 1, TextAlignment.LEFT, null
		));
		requests.add(new TextPlaceRequest(
				"مرحباً", 300, 80, 499, 119,
				112, 25, 1, TextAlignment.RIGHT, null
		));
		requests.add(new TextPlaceRequest(
				"مرحبامرحبامرحباً", 300, 120, 499, 159,
				152, 25, 1, TextAlignment.REVERSED, null
		));
		requests.add(new TextPlaceRequest(
				"مرحبامرحبامرحباً", 300, 160, 499, 199,
				192, 25, 1, TextAlignment.DEFAULT, null
		));
		requests.add(new TextPlaceRequest(
				"مرحباً", 300, 200, 499, 239,
				232, 25, 1, TextAlignment.CENTER, null
		));
		requests.add(new TextPlaceRequest(
				"مرحبامرحبامرحباً", 300, 220, 499, 279,
				272, 25, 1, TextAlignment.CENTER, null
		));
		renderer.render(requests);

		assertImageEquals(
				"expected-text-alignment.png",
				renderer.image,
				"expected-text-alignment.png",
				true
		);

		renderer.destroy();
		font.destroy();
		instance.destroy();
	}
}
