package com.github.knokko.text.font;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Face;

import java.nio.ByteBuffer;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.util.freetype.FreeType.FT_New_Memory_Face;

class ByteBufferFaceSource implements FreeTypeFaceSource {

	private final ByteBuffer byteBuffer;

	ByteBufferFaceSource(ByteBuffer byteBuffer) {
		this.byteBuffer = byteBuffer;
	}

	@Override
	public FT_Face createFreeTypeFace(long ftLibrary, MemoryStack stack) {
		var pFace = stack.callocPointer(1);
		assertFtSuccess(FT_New_Memory_Face(
				ftLibrary, byteBuffer, 0, pFace
		), "New_Memory_Face", "ByteBufferFaceData");
		return FT_Face.create(pFace.get(0));
	}

	@Override
	public void destroy() {
		memFree(byteBuffer);
	}
}
