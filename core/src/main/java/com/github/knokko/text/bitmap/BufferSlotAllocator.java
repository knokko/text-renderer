package com.github.knokko.text.bitmap;

public interface BufferSlotAllocator {

	int allocateIndex(int offsetX, int offsetY, int width, int height);
}
