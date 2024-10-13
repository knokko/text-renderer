package com.github.knokko.text.font;

import java.util.function.IntUnaryOperator;

public class HeightSearcher {

	private final int[] cache;
	private final IntUnaryOperator computeHeight;

	HeightSearcher(int capacity, IntUnaryOperator computeHeight) {
		this.cache = new int[capacity];
		this.computeHeight = computeHeight;
	}

	int getHeight(int size) {
		if (size <= 0) throw new IllegalArgumentException("Size (" + size + ") must be positive");
		if (size >= cache.length) return computeHeight.applyAsInt(size);

		int cachedHeight = cache[size];
		if (cachedHeight != 0) return cachedHeight - 10;

		int height = computeHeight.applyAsInt(size);
		cache[size] = height + 10;
		return height;
	}

	int search(
			int desiredResult, int initialInput,
			int minInput, int maxInput
	) {
		int input = initialInput;

		int highInput = maxInput;
		int lowInput = minInput;

		while (true) {
			int oldLowInput = lowInput;
			int oldHighInput = highInput;

			int result = getHeight(input);
			if (result == desiredResult) return input;

			if (result > desiredResult) {
				int lowResult = getHeight(lowInput);
				float resultFactor = (desiredResult - lowResult) / (float) (result - lowResult);
				highInput = input;
				input = lowInput + Math.round((input - lowInput) * resultFactor);
			} else {
				int highResult = getHeight(highInput);
				float resultFactor = (desiredResult - result) / (float) (highResult - result);
				lowInput = input;
				input = input + Math.round((highInput - input) * resultFactor);
			}

			if (input < lowInput) input = lowInput;
			if (input > highInput) input = highInput;

			if (lowInput == oldLowInput && highInput == oldHighInput) return input;
		}
	}
}
