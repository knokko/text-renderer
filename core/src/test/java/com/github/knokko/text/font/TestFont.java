package com.github.knokko.text.font;

import com.github.knokko.text.TextInstance;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Objects;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.util.freetype.FreeType.FT_LOAD_BITMAP_METRICS_ONLY;
import static org.lwjgl.util.freetype.FreeType.FT_Load_Char;

public class TestFont {

	@Test
	public void testSetHeight() {
		var instance = new TextInstance();
		var font = new FontData(instance, new FilesFontSource(new File(
				"../unicode-fonts/src/main/resources/fonts/unicode-freeserif.ttf"
		)));

		var face = font.borrowFaceWithHeightA(0, 15, 1);
		assertFtSuccess(FT_Load_Char(
				face.ftFace, 'A', FT_LOAD_BITMAP_METRICS_ONLY
		), "Load_Char", "TestFont.testSetHeight");

		long height = Objects.requireNonNull(face.ftFace.glyph()).metrics().height() / 64;
		assertTrue(height >= 14 && height <= 16, "Font height (" + height + ") should be in range [14, 16]");

		font.returnFace(face);
		font.destroy();
		instance.destroy();
	}

	@Test
	public void testSetSize() {
		var instance = new TextInstance();
		var font = new FontData(instance, new ClasspathFontsSource("fonts/unicode-polyglott.ttf"));
		var face = font.borrowFaceWithSize(0, 10 * 64, 1);

		assertFtSuccess(FT_Load_Char(
				face.ftFace, 'A', FT_LOAD_BITMAP_METRICS_ONLY
		), "Load_Char", "TestFont.testSetHeight");

		long height = Objects.requireNonNull(face.ftFace.glyph()).metrics().height() / 64;
		assertTrue(height >= 7 && height <= 9, "Font height (" + height + ") should be in range [7, 9]");

		font.destroy();
		instance.destroy();
	}
}
