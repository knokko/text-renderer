package com.github.knokko.text.font;

import org.lwjgl.util.freetype.FT_Face;

import java.nio.ByteBuffer;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memCalloc;
import static org.lwjgl.util.freetype.FreeType.FT_New_Memory_Face;

public class ByteArraysFontSource extends FontSource {

	private final String context;
	private final byte[][] arrays;

	public ByteArraysFontSource(String context, byte[]... arrays) {
		this.context = context;
		this.arrays = arrays;
	}

	@Override
	public LoadedFonts load(long ftLibrary) {
		ByteBuffer[] buffers = new ByteBuffer[arrays.length];
		FT_Face[] faces = new FT_Face[arrays.length];

		try (var stack = stackPush()) {
			var pFace = stack.callocPointer(1);
			for (int index = 0; index < buffers.length; index++) {
				buffers[index] = memCalloc(arrays[index].length);
				buffers[index].put(0, arrays[index]);
				assertFtSuccess(FT_New_Memory_Face(
						ftLibrary, buffers[index], 0, pFace
				), "New_Memory_Face", context + "[" + index + "]");
				faces[index] = FT_Face.create(pFace.get(0));
			}
		}

		return new LoadedFonts(faces, buffers);
	}
}
