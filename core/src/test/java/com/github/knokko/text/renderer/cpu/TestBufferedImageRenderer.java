package com.github.knokko.text.renderer.cpu;

import com.github.knokko.text.TextInstance;
import com.github.knokko.text.font.ClasspathFontsSource;
import com.github.knokko.text.font.FontData;
import com.github.knokko.text.font.UnicodeFonts;
import com.github.knokko.text.placement.TextPlaceRequest;
import com.github.knokko.text.util.UnicodeLines;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static com.github.knokko.text.util.ImageChecks.assertImageEquals;

public class TestBufferedImageRenderer {

	@Test
	public void testPartialHebrew() {
		var instance = new TextInstance();
		var font = new FontData(instance, 200, new ClasspathFontsSource("fonts/unicode-polyglott.ttf"));
		var renderer = new BufferedImageTextRenderer(
				new BufferedImage(500, 130, BufferedImage.TYPE_INT_RGB),
				font, 10_000
		);

		List<TextPlaceRequest> requests = new ArrayList<>();
		requests.add(new TextPlaceRequest("hello world", 10, 10, 200, 40, true, null));
		requests.add(new TextPlaceRequest("אלט", 210, 10, 300, 40, true, null));
		requests.add(new TextPlaceRequest("ؤلاششششششش" + "  hi  " + "يييييييثب", 10, 50, 490, 80, true, null));
		requests.add(new TextPlaceRequest("(Only) 1 word (אלט) is Hebrew", 10, 90, 490, 120, true, null));
		renderer.render(requests);

		assertImageEquals("expected-english-hebrew-mix.png", renderer.image, "actual-english-hebrew-mix.png");

		renderer.destroy();
		font.destroy();
		instance.destroy();
	}

	@Test
	public void unicodeTestCase() {
		var instance = new TextInstance();
		var font = new FontData(instance, 20_000, UnicodeFonts.SOURCE);
		var renderer = new BufferedImageTextRenderer(
				new BufferedImage(3500, 9700, BufferedImage.TYPE_INT_RGB),
				font, 3_000_000
		);

		List<TextPlaceRequest> requests = new ArrayList<>();
		int minY = 5;
		for (String line : UnicodeLines.get()) {
			int maxY = minY + 40;
			requests.add(new TextPlaceRequest(line, 0, minY, renderer.image.getWidth(), maxY, false, null));
			minY = maxY;
		}

		renderer.render(requests);

		assertImageEquals("expected-unicode-test-result.png", renderer.image, "actual-unicode-test-result.png");

		renderer.destroy();
		font.destroy();
		instance.destroy();
	}

	@Test
	public void unicodeTestCaseInMultipleFrames() {
		var instance = new TextInstance();
		var font = new FontData(instance, 20_000, UnicodeFonts.SOURCE);
		var renderer = new BufferedImageTextRenderer(
				new BufferedImage(3500, 9700, BufferedImage.TYPE_INT_RGB),
				font, 90_000
		);

		List<TextPlaceRequest> requests = new ArrayList<>(1);
		int minY = 5;
		for (String line : UnicodeLines.get()) {
			int maxY = minY + 40;
			requests.add(new TextPlaceRequest(line, 0, minY, renderer.image.getWidth(), maxY, false, null));
			renderer.render(requests);
			requests.clear();
			minY = maxY;
		}

		assertImageEquals(
				"expected-unicode-test-result.png", renderer.image,
				"actual-unicode-test-result-multiple-frames.png"
		);

		renderer.destroy();
		font.destroy();
		instance.destroy();
	}

	@Test
	public void testVeryLargeText() {
		var instance = new TextInstance();
		var font = new FontData(instance, 20_000, UnicodeFonts.SOURCE);
		var renderer = new BufferedImageTextRenderer(
				new BufferedImage(11000, 3000, BufferedImage.TYPE_INT_RGB),
				font, 1_000_000
		);

		var requests = new ArrayList<TextPlaceRequest>();
		int minX = 0;
		int minY = 0;
		for (int height = 1500; height < 5000; height += 900) {
			requests.add(new TextPlaceRequest("Big", minX, minY, minX + height, minY + height, false, null));
			// noinspection SuspiciousNameCombination
			minX += height;
			if (minX > renderer.image.getWidth()) {
				minX = 0;
				minY += height;
			}
		}

		renderer.render(requests);

		assertImageEquals("expected-very-large-text.png", renderer.image, "actual-very-large-text.png");

		renderer.destroy();
		font.destroy();
		instance.destroy();
	}

	@Test
	public void setMaxHeight() {
		var instance = new TextInstance();
		var font = new FontData(instance, 100, UnicodeFonts.SOURCE);
		var renderer = new BufferedImageTextRenderer(
				new BufferedImage(4000, 300, BufferedImage.TYPE_INT_RGB),
				font, 50_000
		);

		var requests = new ArrayList<TextPlaceRequest>();
		int minX = 0;
		int minY = 0;
		for (int height = 20; height < 500; height += 30) {
			requests.add(new TextPlaceRequest("Big", minX, minY, minX + height, minY + height, false, null));
			// noinspection SuspiciousNameCombination
			minX += height;
			if (minX > renderer.image.getWidth()) {
				minX = 0;
				minY += height;
			}
		}

		renderer.render(requests);

		assertImageEquals("expected-large-text.png", renderer.image, "actual-large-text.png");

		renderer.destroy();
		font.destroy();
		instance.destroy();
	}
}
