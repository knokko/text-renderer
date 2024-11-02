package com.github.knokko.text;

public record BoundingRectangle(int minX, int minY, int maxX, int maxY) {

	public boolean hasOverlap(int minX, int minY, int maxX, int maxY) {
		return minX <= this.maxX && minY <= this.maxY && maxX >= this.minX && maxY >= this.minY;
	}
}
