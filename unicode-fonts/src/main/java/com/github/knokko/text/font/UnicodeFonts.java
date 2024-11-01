package com.github.knokko.text.font;

/**
 * This class (only) has {@link #SOURCE}
 */
public class UnicodeFonts {

	/**
	 * A {@link FontSource} that combines 16 fonts that together cover most unicode characters, all of which are free
	 * to use for both personal and commercial projects. This font source should be an excellent fallback font
	 * (or a simple font for testing).
	 */
	public static final FontSource SOURCE = new ClasspathFontsSource(
			"fonts/unicode-freeserif.ttf",
			"fonts/unicode-quivira.ttf",
			"fonts/unicode-polyglott.ttf",
			"fonts/cjk.ttf",
			"fonts/thaana.ttf",
			"fonts/syriac.otf",
			"fonts/gujarati.ttf",
			"fonts/oriya.ttf",
			"fonts/telugu.otf",
			"fonts/kannada.ttf",
			"fonts/tibetan.ttf",
			"fonts/myanmar.ttf",
			"fonts/hangul.ttf",
			"fonts/mongolian.ttf",
			"fonts/yi.ttf",
			"fonts/marks.ttf"
	);
}
