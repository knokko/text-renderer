package com.github.knokko.text.placement;

import java.util.Objects;

public class TextPlaceRequest implements Comparable<TextPlaceRequest> {

	public final String text;
	public final int minX, minY, maxX, maxY;
	// TODO Optionally enforce vertical bounds
	public final Object userData;

	public TextPlaceRequest(String text, int minX, int minY, int maxX, int maxY, Object userData) {
		if (minX > maxX) {
			throw new IllegalArgumentException("minX (" + minX + ") must not be larger than maxX (" + maxX + ")");
		}
		if (minY > maxY) {
			throw new IllegalArgumentException("minY (" + minY + ") must not be larger than maxY (" + maxY + ")");
		}
		this.text = Objects.requireNonNull(text);
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		this.userData = userData;
	}

	public int getWidth() {
		return 1 + maxX - minX;
	}

	public int getHeight() {
		return 1 + maxY - minY;
	}

	@Override
	public int compareTo(TextPlaceRequest other) {
		return this.getHeight() - other.getHeight();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof TextPlaceRequest) {
			TextPlaceRequest request = (TextPlaceRequest) other;
			return this.text.equals(request.text) && Objects.equals(this.userData, request.userData) &&
					this.minX == request.minX && this.minY == request.minY &&
					this.maxX == request.maxX && this.maxY == request.maxY;
		} else return false;
	}

	@Override
	public int hashCode() {
		return text.hashCode() + minX + 13 * minY - 31 * maxX + maxY;
	}
}
