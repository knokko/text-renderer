package com.github.knokko.text.renderer.cpu;

import com.github.knokko.text.TextFont;

import java.awt.*;
import java.awt.image.BufferedImage;

public class BufferedImageTextRenderer extends CpuTextRenderer {

	public final BufferedImage image;

	public BufferedImageTextRenderer(BufferedImage image, TextFont font, int glyphBufferCapacity) {
		super(font, glyphBufferCapacity);
		this.image = image;
	}

	@Override
	public void setPixel(int x, int y, int value) {
		if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) {
			image.setRGB(x, y, new Color(value, value, value).getRGB());
		}
	}
}
