package com.github.knokko.text.font;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntUnaryOperator;

class HeightSearcher {

	private final ConcurrentHashMap<Integer, Integer> cache = new ConcurrentHashMap<>();
	private final IntUnaryOperator computeHeight;
	private final int capacity;

	HeightSearcher(int capacity, IntUnaryOperator computeHeight) {
		this.capacity = capacity;
		this.computeHeight = computeHeight;
	}

	int getHeight(int size) {
		if (size <= 0) throw new IllegalArgumentException("Size (" + size + ") must be positive");

		Integer cachedHeight = cache.get(size);
		if (cachedHeight != null) return cachedHeight;

		// Prevent the cache from getting too large
		if (cache.size() > capacity) cache.clear();

		int height = computeHeight.applyAsInt(size);
		cache.put(size, height);
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
