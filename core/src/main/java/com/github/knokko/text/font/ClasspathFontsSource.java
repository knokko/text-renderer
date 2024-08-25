package com.github.knokko.text.font;

import java.io.IOException;

import static org.lwjgl.system.MemoryUtil.memCalloc;

public class ClasspathFontsSource extends FontSource {

	private final String[] paths;

	public ClasspathFontsSource(String... paths) {
		this.paths = paths;
	}

	@Override
	public LoadedFonts load(long ftLibrary) {
		byte[][] arrays = new byte[paths.length][];
		for (int index = 0; index < paths.length; index++) {
			try (var input = ClasspathFontsSource.class.getClassLoader().getResourceAsStream(paths[index])) {
				if (input == null) throw new IllegalArgumentException("Can't find font at " + paths[index]);
				arrays[index] = input.readAllBytes();
			} catch (IOException e) {
				throw new IllegalArgumentException("Encountered IO exception while loading font at " + paths[index]);
			}
		}

		return new ByteArraysFontSource("ClasspathsFontSource", arrays).load(ftLibrary);
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
