package com.github.knokko.text.font;

/**
 * Represents a 'source' of 1 or more font 'files'. A <i>FontSource</i> essentially describes where the text renderer
 * can find the fonts that you want to use. This library provides 3 implementations:
 * <ul>
 *     <li>{@link FilesFontSource}s point to ttf files and/or otf files</li>
 *     <li>
 *         {@link ClasspathFontsSource}s point to ttf files and/or otf files on the classpath, that can be accessed
 *         via {@code someClass.getClassLoader().getResourceAsStream(...)}
 *     </li>
 *     <li>{@link ByteArraysFontSource}s contains the content of ttf files and/or otf files in byte arrays</li>
 * </ul>
 * That last one can be used as 'fallback' when you can't use any of the first two.
 */
public abstract class FontSource {

	abstract FreeTypeFaceSource[] loadData();
}
