package com.github.knokko.text;

import org.junit.jupiter.api.Test;

import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestHeightSearcher {

	@Test
	public void testIdentityFunction() {
		HeightSearcher searcher = new HeightSearcher(100, x -> x);
		assertEquals(46, searcher.search(46, 20, 10, 80));
		assertEquals(123, searcher.search(123, 30, 25, 200));
		assertEquals(123, searcher.search(123, 30, 25, 123));
		assertEquals(120, searcher.search(123, 30, 25, 120));
		assertEquals(10, searcher.search(5, 23, 10, 100));
	}

	private void checkInexactResult(IntUnaryOperator function, int targetHeight, int size) {
		int minHeight = function.applyAsInt(size - 1);
		int maxHeight = function.applyAsInt(size + 1);
		assertTrue(minHeight < targetHeight, "Expected f(" + (size - 1) + ") = " + minHeight + " < " + targetHeight);
		assertTrue(maxHeight > targetHeight, "Expected f(" + (size + 1) + ") = " + maxHeight + " > " + targetHeight);
	}

	@Test
	public void testNonLinearFunction() {
		IntUnaryOperator function = x -> (int) (1.23 * Math.pow(x, 1.45));
		HeightSearcher searcher = new HeightSearcher(500, function);
		checkInexactResult(function, 50, searcher.search(50, 85, 1, 1000));
		checkInexactResult(function, 5, searcher.search(5, 2, 1, 30));
	}
}
