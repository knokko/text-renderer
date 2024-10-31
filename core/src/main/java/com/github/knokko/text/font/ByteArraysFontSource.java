package com.github.knokko.text.font;

import static org.lwjgl.system.MemoryUtil.memCalloc;

/**
 * A <i>FontSource</i> implementation that contains the content of ttf files and/or otf files in byte array(s). Using
 * {@link ClasspathFontsSource} or {@link FilesFontSource} is usually more convenient than using this class, but this
 * class is needed when you can't use those two for some reason.
 */
public class ByteArraysFontSource extends FontSource {

	private final byte[][] arrays;

	/**
	 * Constructs a new {@link ByteArraysFontSource}. Each element of {@code arrays} must contain the content of 1
	 * ttf file or otf file.
	 */
	public ByteArraysFontSource(byte[]... arrays) {
		this.arrays = arrays;
	}

	@Override
	FreeTypeFaceSource[] loadData() {
		var data = new FreeTypeFaceSource[arrays.length];

		for (int index = 0; index < arrays.length; index++) {
			var dataBuffer = memCalloc(arrays[index].length);
			dataBuffer.put(0, arrays[index]);
			data[index] = new ByteBufferFaceSource(dataBuffer);
		}

		return data;
	}
}
