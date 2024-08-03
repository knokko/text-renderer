package com.github.knokko.text.renderer.cpu;

import com.github.knokko.text.font.TextFont;

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
			Color oldColor = new Color(image.getRGB(x, y));
			double newValue = value / 255.0;
			double oldFactor = 1.0 - newValue;
			double oldValue = (oldColor.getRed() + oldColor.getGreen() + oldColor.getBlue()) / (3.0 * 255.0);
			double mergedValue = newValue + oldFactor * oldValue;
			int mergedIntValue = Math.min(255, Math.max(0, Math.round((float) mergedValue * 255f)));
			image.setRGB(x, y, new Color(mergedIntValue, mergedIntValue, mergedIntValue).getRGB());
		}
	}
}
