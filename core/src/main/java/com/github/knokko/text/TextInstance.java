package com.github.knokko.text;

import com.github.knokko.text.font.*;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.lwjgl.system.MemoryStack.stackPush;
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

	public synchronized FT_Face createFreeTypeFace(FreeTypeFaceSource source, MemoryStack stack) {
		return source.createFreeTypeFace(ftLibrary, stack);
	}

	public void destroy() {
		FT_Done_FreeType(ftLibrary);
	}
}
