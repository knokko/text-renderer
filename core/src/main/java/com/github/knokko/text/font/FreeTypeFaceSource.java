package com.github.knokko.text.font;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Face;

public interface FreeTypeFaceSource {

	FT_Face createFreeTypeFace(long ftLibrary, MemoryStack stack);

	void destroy();
}
