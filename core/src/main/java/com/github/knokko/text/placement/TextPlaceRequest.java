package com.github.knokko.text.placement;

import java.util.Objects;

/**
 * The input of stage 1 of the text rendering pipeline is a collection of <i>TextPlaceRequest</i>s. Each request
 * consists of a string to render, a start position, a size, a bounding rectangle, and optional user data.
 */
public class TextPlaceRequest implements Comparable<TextPlaceRequest> {

	/**
	 * The text that should be rendered
	 */
	public final String text;

	/**
	 * The bounding rectangle in which the text should be rendered. The renderer will ensure that all glyphs that fall
	 * outside the bounding rectangle, are discarded. Furthermore
	 * <ul>
	 *     <li>
	 *         For text that is primary left-to-right,
	 *         the <i>minX</i> is the X-coordinate of the start position of the text.
	 *     </li>
	 *     <li>
	 *         For text that is primarily right-to-left,
	 *         the <i>maxX</i> is the X-coordinate of the start position of the text.
	 *     </li>
	 * </ul>
	 * The Y-coordinate of the start position is always <i>baseY</i>
	 */
	public final int minX, minY, maxX, maxY;

	/**
	 * The Y-coordinate of the start position of the text.
	 */
	public final int baseY;

	/**
	 * The <i>heightA</i> determines the size of the rendered text. The size will be chosen such that the height of
	 * the (uppercase) <b>A</b> character will be <i>heightA</i> pixels. This 'weird' way of specifying the size
	 * ensures that the text size is mostly independent of the font.
	 */
	public final int heightA;

	/**
	 * The minimum {@link com.github.knokko.text.SizedGlyph#scale}, typically 1
	 */
	public final int minScale;

	/**
	 * The {@link TextAlignment}
	 */
	public final TextAlignment alignment;

	/**
	 * Optional arbitrary user data that can be given to the renderer. This user data will be propagated to stage 3
	 * of the renderer. If the userData is an <i>Integer</i>, the built-in CPU renderer and Vulkan renderer will
	 * use it as text color, and assume that it was packed using the <i>ColorPacker</i> of vk-boiler. The built-in
	 * renderers will ignore anything else.
	 */
	public final Object userData;

	/**
	 * Constructs a new <i>TextPlaceRequest</i> with the given parameters
	 * @param text {@link #text}
	 * @param minX {@link #minX}
	 * @param minY {@link #minY}
	 * @param maxX {@link #maxX}
	 * @param maxY {@link #maxY}
	 * @param baseY {@link #baseY}
	 * @param heightA {@link #heightA}
	 * @param minScale {@link #minScale}
	 * @param alignment {@link TextAlignment}
	 * @param userData {@link #userData}
	 */
	public TextPlaceRequest(
			String text, int minX, int minY, int maxX, int maxY,
			int baseY, int heightA, int minScale, TextAlignment alignment, Object userData
	) {
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
		this.baseY = baseY;
		this.heightA = heightA;
		this.minScale = minScale;
		this.alignment = Objects.requireNonNull(alignment);
		this.userData = userData;
	}

	public int getWidth() {
		return 1 + maxX - minX;
	}

	@Override
	public int compareTo(TextPlaceRequest other) {
		return Integer.compare(this.heightA, other.heightA);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof TextPlaceRequest) {
			TextPlaceRequest request = (TextPlaceRequest) other;
			return this.text.equals(request.text) && Objects.equals(this.userData, request.userData) &&
					this.minX == request.minX && this.minY == request.minY &&
					this.maxX == request.maxX && this.maxY == request.maxY &&
					this.baseY == request.baseY && this.heightA == request.heightA &&
					this.minScale == request.minScale && this.alignment == request.alignment;
		} else return false;
	}

	@Override
	public int hashCode() {
		return text.hashCode() + minX + 13 * minY - 31 * maxX + maxY + 93 * baseY - 113 * heightA;
	}
}
