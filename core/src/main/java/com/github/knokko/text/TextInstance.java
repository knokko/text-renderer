package com.github.knokko.text;

import com.github.knokko.text.font.*;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.freetype.FreeType.*;

/**
 * The 'root' of the text renderer hierarchy. Anything in this library requires a <i>TextInstance</i>, either
 * directly or indirectly. You should create 1 <i>TextInstance</i> per FT_Library... so usually just 1. This class is
 * completely thread-safe.
 */
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

	/**
	 * Creates a <i>TextInstance</i> that will use the given <i>FT_Library</i>. This constructor is recommended when
	 * you want to create and/or also use the FT_Library yourself.
	 */
	public TextInstance(long ftLibrary) {
		this.ftLibrary = ftLibrary;
	}

	/**
	 * Creates a new <i>TextInstance</i> and <i>FT_Library</i>. This constructor is recommended when you don't need to
	 * use the <i>FT_Library</i> yourself.
	 */
	public TextInstance() {
		this(createLibrary());
	}

	/**
	 * This method is recommended for internal library use only. It creates a FT_Face from a source
	 */
	public synchronized FT_Face createFreeTypeFace(FreeTypeFaceSource source, MemoryStack stack) {
		return source.createFreeTypeFace(ftLibrary, stack);
	}

	/**
	 * You should call this method when you no longer need this instance, nor any of its children.
	 */
	public void destroy() {
		FT_Done_FreeType(ftLibrary);
	}
}
