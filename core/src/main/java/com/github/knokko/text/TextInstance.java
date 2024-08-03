package com.github.knokko.text;

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

	public synchronized TextFont createFontFromFile(File file) {
		try (var stack = stackPush()) {
			var pFace = stack.callocPointer(1);
			assertFtSuccess(FT_New_Face(
					ftLibrary, stack.UTF8(file.getAbsolutePath()), 0, pFace
			), "New_Face", "TextInstance.createFontFromFile");
			return new TextFont(FT_Face.create(pFace.get(0)), null);
		}
	}

	public synchronized TextFont createFontFromResourcePath(String path) {
		ByteBuffer fontBuffer;
		try (var input = TextInstance.class.getClassLoader().getResourceAsStream(path)) {
			if (input == null) throw new Error("Can't find resource at path " + path);
			byte[] fontArray = input.readAllBytes();

			fontBuffer = memAlloc(fontArray.length);
			fontBuffer.put(0, fontArray);
		} catch (IOException e) {
			throw new Error("Can't read font " + path);
		}

		return createFontFromBuffer(fontBuffer);
	}

	public TextFont createFontFromBuffer(ByteBuffer fontBuffer) {
		try (var stack = stackPush()) {
			var pFace = stack.callocPointer(1);
			assertFtSuccess(FT_New_Memory_Face(
					ftLibrary, fontBuffer, 0, pFace
			), "New_Memory_Face", "TextInstance.createFontFromResourcePath");
			return new TextFont(FT_Face.create(pFace.get(0)), fontBuffer);
		}
	}

	public void destroy() {
		FT_Done_FreeType(ftLibrary);
	}
}
