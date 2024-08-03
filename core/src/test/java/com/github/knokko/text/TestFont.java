package com.github.knokko.text;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestFont {

	@Test
	public void testSetHeight() {
		var instance = new TextInstance();
		var font = instance.createFontFromFile(new File("../unicode-fonts/src/main/resources/unicode1.ttf"));

		font.setHeight(15);
		int height = font.getHeight();
		assertTrue(height >= 14 && height <= 16, "Font height (" + height + ") should be in range [14, 16]");

		font.destroy();
		instance.destroy();
	}

	@Test
	public void testSetHeightRegression() {
		var instance = new TextInstance();
		var font = instance.createFontFromFile(new File("../unicode-fonts/src/main/resources/unicode1.ttf"));

		font.setHeight(21);
		int height = font.getHeight();
		assertTrue(height >= 20 && height <= 22, "Font height (" + height + ") should be in range [20, 22]");

		font.destroy();
		instance.destroy();
	}

	@Test
	public void testSetSize() {
		var instance = new TextInstance();
		var font = instance.createFontFromResourcePath("unicode1.ttf");

		font.setSize(10);
		int height = font.getHeight();
		assertTrue(height >= 11 && height <= 15, "Font height (" + height + ") should be in range [11, 15]");

		font.destroy();
		instance.destroy();
	}
}
