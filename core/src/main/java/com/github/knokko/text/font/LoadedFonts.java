package com.github.knokko.text.font;

import org.lwjgl.util.freetype.FT_Face;

import java.nio.ByteBuffer;

public record LoadedFonts(FT_Face[] ftFaces, ByteBuffer[] fontBuffers) { }
