package com.github.knokko.text.renderer.cpu;

import com.github.knokko.text.TextInstance;
import com.github.knokko.text.font.ClasspathFontsSource;
import com.github.knokko.text.font.UnicodeFonts;
import com.github.knokko.text.placement.TextPlaceRequest;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import static com.github.knokko.text.util.ImageChecks.assertImageEquals;

public class TestBufferedImageRenderer {

	@Test
	public void testPartialHebrew() {
		var instance = new TextInstance();
		var font = instance.createFont(new ClasspathFontsSource("fonts/unicode-polyglott.ttf"));
		var renderer = new BufferedImageTextRenderer(
				new BufferedImage(500, 130, BufferedImage.TYPE_INT_RGB),
				font, 10_000
		);

		List<TextPlaceRequest> requests = new ArrayList<>();
		requests.add(new TextPlaceRequest("hello world", 10, 10, 200, 40, null));
		requests.add(new TextPlaceRequest("אלט", 210, 10, 300, 40, null));
		requests.add(new TextPlaceRequest("ؤلاششششششش" + "  hi  " + "يييييييثب", 10, 50, 490, 80, null));
		requests.add(new TextPlaceRequest("(Only) 1 word (אלט) is Hebrew", 10, 90, 490, 120, null));
		renderer.render(requests);

		assertImageEquals("expected-english-hebrew-mix.png", renderer.image, "actual-english-hebrew-mix.png");

		renderer.destroy();
		font.destroy();
		instance.destroy();
	}

	@Test
	public void unicodeTestCase() {
		var instance = new TextInstance();
		var font = instance.createFont(UnicodeFonts.SOURCE);
		var renderer = new BufferedImageTextRenderer(
				new BufferedImage(2000, 20_000, BufferedImage.TYPE_INT_RGB),
				font, 3_000_000
		);

		var scanner = new Scanner(Objects.requireNonNull(
				TestBufferedImageRenderer.class.getClassLoader().getResourceAsStream("unicode-3.2-test-page.html")
		), StandardCharsets.UTF_8);

		List<TextPlaceRequest> requests = new ArrayList<>();
		int minY = 5;
		while (scanner.hasNextLine()) {
			int maxY = minY + 40;
			requests.add(new TextPlaceRequest(scanner.nextLine(), 0, minY, renderer.image.getWidth(), maxY, null));
			minY = maxY;
		}
		scanner.close();

		renderer.render(requests);

		assertImageEquals("expected-unicode-test-result.png", renderer.image, "actual-unicode-test-result.png");

		renderer.destroy();
		font.destroy();
		instance.destroy();
	}
}
