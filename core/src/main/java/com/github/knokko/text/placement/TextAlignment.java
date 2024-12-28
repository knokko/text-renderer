package com.github.knokko.text.placement;

/**
 * The text alignment determines what happens when there is more horizontal space than needed to place all text.
 * By default, left-to-right text will have a gap on the right of the text, and right-to-left text will have a gap
 * on the left of the text.
 */
public enum TextAlignment {

	/**
	 * <ul>
	 *     <li>Left-to-right text starts at the left boundary, and renders towards the right boundary</li>
	 *     <li>Right-to-left text starts at the right boundary, and renders towards the left boundary</li>
	 * </ul>
	 */
	DEFAULT,

	/**
	 * <ul>
	 *     <li>Left-to-right text renders towards the right boundary, and ends at the right boundary</li>
	 *     <li>Right-to-left text renders towards the left boundary, and ends at the left boundary</li>
	 * </ul>
	 */
	REVERSED,

	/**
	 * <ul>
	 *     <li>Left-to-right text starts at the left boundary, and renders towards the right boundary</li>
	 *     <li>Right-to-left text renders towards the left boundary, and ends at the left boundary</li>
	 * </ul>
	 */
	LEFT,

	/**
	 * <ul>
	 *     <li>Left-to-right text renders towards the right boundary, and ends at the right boundary</li>
	 *     <li>Right-to-left text starts at the right boundary, and renders towards the left boundary</li>
	 * </ul>
	 */
	RIGHT
}
