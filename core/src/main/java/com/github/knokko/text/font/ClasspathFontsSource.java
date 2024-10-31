package com.github.knokko.text.font;

import java.io.IOException;

import static org.lwjgl.system.MemoryUtil.memCalloc;

/**
 * An implementation of {@link FontSource} that points to ttf files and/or otf files that can be loaded as resource
 * from the classpath using {@code someClass.getClassLoader().getResourceAsStream(...)}
 */
public class ClasspathFontsSource extends FontSource {

	private final String[] paths;

	/**
	 * Constructs a new {@link ClasspathFontsSource}. Each element in {@code paths} must point to 1 ttf file and/or
	 * otf file on the classpath
	 */
	public ClasspathFontsSource(String... paths) {
		this.paths = paths;
	}

	@Override
	FreeTypeFaceSource[] loadData() {
		var sources = new FreeTypeFaceSource[paths.length];
		for (int index = 0; index < paths.length; index++) {
			try (var input = ClasspathFontsSource.class.getClassLoader().getResourceAsStream(paths[index])) {
				if (input == null) throw new IllegalArgumentException("Can't find font at " + paths[index]);

				var byteArray = input.readAllBytes();
				var byteBuffer = memCalloc(byteArray.length);
				byteBuffer.put(0, byteArray);
				sources[index] = new ByteBufferFaceSource(byteBuffer);
			} catch (IOException e) {
				throw new IllegalArgumentException("Encountered IO exception while loading font at " + paths[index]);
			}
		}
		return sources;
	}
}
