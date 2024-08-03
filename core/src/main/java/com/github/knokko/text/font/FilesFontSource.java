package com.github.knokko.text.font;

import org.lwjgl.util.freetype.FT_Face;

import java.io.File;
import java.nio.ByteBuffer;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.freetype.FreeType.FT_New_Face;

public class FilesFontSource extends FontSource {

	private final File[] files;

	public FilesFontSource(File... files) {
		this.files = files;
	}

	@Override
	public LoadedFonts load(long ftLibrary) {
		FT_Face[] faces = new FT_Face[files.length];
		try (var stack = stackPush()) {
			var pFace = stack.callocPointer(1);
			for (int index = 0; index < faces.length; index++) {
				assertFtSuccess(FT_New_Face(
						ftLibrary, stack.UTF8(files[index].getAbsolutePath()), 0, pFace
				), "FT_New_Face", files[index].getAbsolutePath());

				faces[index] = FT_Face.create(pFace.get(index));
			}
		}

		return new LoadedFonts(faces, new ByteBuffer[0]);
	}
}
