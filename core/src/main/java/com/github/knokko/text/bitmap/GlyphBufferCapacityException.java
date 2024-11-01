package com.github.knokko.text.bitmap;

/**
 * This exception will be thrown when a {@link BitmapGlyphsBuffer} doesn't have enough capacity to store all rasterized
 * glyph in the current frame. Note that this exception could also be thrown because you forgot to call
 * {@link BitmapGlyphsBuffer#startFrame()}, which would prevent it from reclaiming the space of rasterized glyphs
 * used by previous frames.
 */
public class GlyphBufferCapacityException extends RuntimeException {

    GlyphBufferCapacityException() {
        super("The BitmapGlyphsBuffer doesn't have enough slots (capacity)");
    }
}
