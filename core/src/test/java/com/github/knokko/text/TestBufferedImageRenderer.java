package com.github.knokko.text;

import com.github.knokko.text.placement.TextPlaceRequest;
import com.github.knokko.text.renderer.cpu.BufferedImageTextRenderer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class TestBufferedImageRenderer {

	@Test
	public void testPartialHebrew() throws IOException {
		var instance = new TextInstance();
		var font = instance.createFontFromResourcePath("unicode1.ttf");
		var renderer = new BufferedImageTextRenderer(
				new BufferedImage(500, 100, BufferedImage.TYPE_INT_RGB),
				font, 10_000
		);

		List<TextPlaceRequest> requests = new ArrayList<>();
		requests.add(new TextPlaceRequest("Hello, world!", 10, 10, 200, 40, null));
		requests.add(new TextPlaceRequest("(Only) 1 word (אלט) is Hebrew", 10, 50, 200, 80, null));
		requests.add(new TextPlaceRequest("אלט", 210, 10, 400, 40, null));
		requests.add(new TextPlaceRequest("אאלטאלטאלטאלטאלטלט", 320, 50, 400, 80, null));
		renderer.render(requests);

		// TODO Compare the result with something
		ImageIO.write(renderer.image, "PNG", new File("hello-world.png"));

		renderer.destroy();
		font.destroy();
		instance.destroy();
	}

	@Test
	public void unicodeTestCase() throws IOException {
		var instance = new TextInstance();
		var font = instance.createFontFromResourcePath("unicode1.ttf");
		var renderer = new BufferedImageTextRenderer(
				new BufferedImage(1000, 9_000, BufferedImage.TYPE_INT_RGB),
				font, 3_000_000 // TODO Check whether I can reduce this after optimizing
		);

		var scanner = new Scanner(Objects.requireNonNull(
				TestBufferedImageRenderer.class.getClassLoader().getResourceAsStream("unicode-3.2-test-page.html")
		), StandardCharsets.UTF_8);

		List<TextPlaceRequest> requests = new ArrayList<>();
		int minY = 5;
		while (scanner.hasNextLine()) {
			int maxY = minY + 20;
			requests.add(new TextPlaceRequest(scanner.nextLine(), 0, minY, renderer.image.getWidth(), maxY, null));
			minY = maxY;
		}
		scanner.close();

		renderer.render(requests);

		// TODO Compare the result with something
		ImageIO.write(renderer.image, "PNG", new File("unicode-test.png"));

		renderer.destroy();
		font.destroy();
		instance.destroy();
	}
}
