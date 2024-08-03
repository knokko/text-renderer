package com.github.knokko.text.placement;

public record TextRun(String text, int faceIndex, int offset) {

	@Override
	public String toString() {
		return "TextRun(" + text + ", face=" + faceIndex + ", offset=" + offset + ")";
	}
}
