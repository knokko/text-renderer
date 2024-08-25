package com.github.knokko.text.font;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Face;

import java.io.File;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.lwjgl.util.freetype.FreeType.FT_New_Face;

class FileFaceSource implements FreeTypeFaceSource {

	private final File file;

	FileFaceSource(File file) {
		this.file = file;
	}

	@Override
	public FT_Face createFreeTypeFace(long ftLibrary, MemoryStack stack) {
		if (!file.exists()) throw new IllegalStateException("File " + file + " doesn't exist");
		if (!file.isFile()) throw new IllegalStateException("File " + file + " is no regular file");

		var pFace = stack.callocPointer(1);
		assertFtSuccess(FT_New_Face(
				ftLibrary, stack.UTF8(file.getPath()), 0, pFace
		), "New_Face", file.getPath());
		return FT_Face.create(pFace.get(0));
	}

	@Override
	public void destroy() {}
}
