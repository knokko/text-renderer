package com.github.knokko.text.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

public class ImageChecks {

	public static void assertImageEquals(String expectedResource, BufferedImage actual, String outputPath) {
		BufferedImage expected;
		try (var input = ImageChecks.class.getClassLoader().getResourceAsStream(expectedResource)) {
			if (input == null) throw new IllegalArgumentException("Can't find " + expectedResource);
			expected = ImageIO.read(input);
		} catch (IOException io) {
			throw new RuntimeException(io);
		}

		String failure = null;
		if (expected.getWidth() != actual.getWidth() || expected.getHeight() != actual.getHeight()) {
			failure = "Size mismatch: expected " + expected.getWidth() + " x " + expected.getHeight() +
					", but got " + actual.getWidth() + " x " + actual.getHeight();
		} else {
			compareLoop:
			for (int y = 0; y < expected.getHeight(); y++) {
				for (int x = 0; x < expected.getWidth(); x++) {
					if (expected.getRGB(x, y) != actual.getRGB(x, y)) {
						failure = "Pixel at (" + x + ", " + y + ") differs";
						break compareLoop;
					}
				}
			}
		}

		if (failure != null) {
			try {
				ImageIO.write(actual, "PNG", new File(outputPath));
				fail(failure);
			} catch (IOException io) {
				throw new RuntimeException(io);
			}
		}
	}
}
