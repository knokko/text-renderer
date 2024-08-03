package com.github.knokko.text.font;

import com.github.knokko.text.TextInstance;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestFont {

	@Test
	public void testSetHeight() {
		var instance = new TextInstance();
		var font = instance.createFont(new FilesFontSource(new File(
				"../unicode-fonts/src/main/resources/fonts/unicode-freeserif.ttf"
		)));

		font.setHeight(15);
		int height = font.getHeight();
		assertTrue(height >= 14 && height <= 16, "Font height (" + height + ") should be in range [14, 16]");

		font.destroy();
		instance.destroy();
	}

	@Test
	public void testSetHeightRegression() {
		var instance = new TextInstance();
		var font = instance.createFont(new FilesFontSource(new File(
				"../unicode-fonts/src/main/resources/fonts/unicode-freeserif.ttf"
		)));

		font.setHeight(21);
		int height = font.getHeight();
		assertTrue(height >= 20 && height <= 22, "Font height (" + height + ") should be in range [20, 22]");

		font.destroy();
		instance.destroy();
	}

	@Test
	public void testSetSize() {
		var instance = new TextInstance();
		var font = instance.createFont(new ClasspathFontsSource("fonts/unicode-polyglott.ttf"));

		font.setSize(10);
		int height = font.getHeight();
		assertTrue(height >= 11 && height <= 15, "Font height (" + height + ") should be in range [11, 15]");

		font.destroy();
		instance.destroy();
	}
}
