package com.github.knokko.text.font;

import java.io.File;

/**
 * An implementation of {@link FontSource} that points to ttf files and/or otf files
 */
public class FilesFontSource extends FontSource {

	private final File[] files;

	/**
	 * Constructs a new {@link FilesFontSource}. Each element of {@code files} should point to a ttf file or otf file.
	 */
	public FilesFontSource(File... files) {
		this.files = files;
	}

	@Override
	FreeTypeFaceSource[] loadData() {
		var sources = new FreeTypeFaceSource[files.length];
		for (int index = 0; index < files.length; index++) {
			sources[index] = new FileFaceSource(files[index]);
		}
		return sources;
	}
}
