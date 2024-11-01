package com.github.knokko.text.vulkan;

/**
 * This exception will be thrown when the quad buffer of a {@link VulkanTextRenderer} is too small to store all the
 * glyph quads to render the current frame.
 */
public class QuadBufferOverflowException extends RuntimeException {

	QuadBufferOverflowException(int capacity, int neededCapacity) {
		super("Quad buffer is too small: it has " + capacity + " ints, but needs at least " + neededCapacity + " this frame");
	}
}
