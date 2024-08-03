package com.github.knokko.text;

import com.github.knokko.text.font.FontSource;
import com.github.knokko.text.font.LoadedFonts;
import com.github.knokko.text.font.TextFont;
import org.lwjgl.system.Configuration;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.util.freetype.FreeType.*;

public class TextInstance {

	static {
		Configuration.HARFBUZZ_LIBRARY_NAME.set(FreeType.getLibrary());
	}

	private static long createLibrary() {
		try (var stack = stackPush()) {
			var pLibrary = stack.callocPointer(1);
			assertFtSuccess(FT_Init_FreeType(pLibrary), "Init_FreeType", "TextInstance constructor");
			return pLibrary.get(0);
		}
	}

	private final long ftLibrary;

	public TextInstance(long ftLibrary) {
		this.ftLibrary = ftLibrary;
	}

	public TextInstance() {
		this(createLibrary());
	}

	public synchronized TextFont createFont(FontSource... fonts) {
		LoadedFonts[] loaded = new LoadedFonts[fonts.length];

		for (int index = 0; index < fonts.length; index++) {
			loaded[index] = fonts[index].load(ftLibrary);
		}

		int numFaces = 0;
		int numBuffers = 0;
		for (LoadedFonts font : loaded) {
			numFaces += font.ftFaces().length;
			numBuffers += font.fontBuffers().length;
		}

		FT_Face[] faces = new FT_Face[numFaces];
		ByteBuffer[] buffers = new ByteBuffer[numBuffers];

		int faceIndex = 0;
		int bufferIndex = 0;
		for (LoadedFonts font : loaded) {
			System.arraycopy(font.ftFaces(), 0, faces, faceIndex, font.ftFaces().length);
			faceIndex += font.ftFaces().length;
			System.arraycopy(font.fontBuffers(), 0, buffers, bufferIndex, font.fontBuffers().length);
			bufferIndex += font.fontBuffers().length;
		}

		return new TextFont(faces, buffers);
	}

	public void destroy() {
		FT_Done_FreeType(ftLibrary);
	}
}
