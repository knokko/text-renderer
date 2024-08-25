package com.github.knokko.text.font;

import com.github.knokko.text.TextInstance;

import java.util.Objects;

import static org.lwjgl.system.MemoryStack.stackPush;

public class FontData {

	private final TextInstance textInstance;
	private final FreeTypeFaceSource[] faceSources;
	private final HeightSearcher[] heightSearchers;
	private final int maxHeight;

	public FontData(TextInstance textInstance, int maxHeight, FontSource... fonts) {
		this.textInstance = textInstance;
		this.maxHeight = maxHeight; // TODO Not sure this is the right place

		FreeTypeFaceSource[][] sources = new FreeTypeFaceSource[fonts.length][];
		int numFreeTypeFonts = 0;
		for (int index = 0; index < fonts.length; index++) {
			sources[index] = fonts[index].loadData();
			numFreeTypeFonts += sources[index].length;
		}

		this.faceSources = new FreeTypeFaceSource[numFreeTypeFonts];
		int index = 0;
		for (var fontSources : sources) {
			System.arraycopy(fontSources, 0, this.faceSources, index, fontSources.length);
			index += fontSources.length;
		}

		this.heightSearchers = new HeightSearcher[faceSources.length];
		for (index = 0; index < faceSources.length; index++) {
			int faceIndex = index;
			this.heightSearchers[faceIndex] = new HeightSearcher(5000, size -> {
				var face = this.borrowFaceWithSize(faceIndex, size, 1);
				var metrics = Objects.requireNonNull(face.ftFace.size()).metrics();
				int height = Math.toIntExact(face.getScale() * (metrics.ascender() - metrics.descender()));
				this.returnFace(face);
				return height;
			});
		}
	}

	public TextFace borrowFaceWithHeight(int faceIndex, int height) {
		int size, heightScale;
		synchronized (this.heightSearchers[faceIndex]) {
			int originalHeight = height;
			heightScale = 1;
			while (height > maxHeight) {
				heightScale += 1;
				height = originalHeight / heightScale;
			}

			int desiredRawHeight = height * 64;
			size = heightSearchers[faceIndex].search(desiredRawHeight, height, 3, 10 * height);
		}

		return borrowFaceWithSize(faceIndex, size, heightScale);
	}

	public TextFace borrowFaceWithSize(int faceIndex, int size, int heightScale) {
		// Performance measurements: creating a FT_Face takes 10 to 40 microseconds, and allocates 10 to 30 KB
		// Resizing an existing FT_Face takes 1 to 15 microseconds
		// TODO Consider reusing them
		try (var stack = stackPush()) {
			var ftFace = textInstance.createFreeTypeFace(faceSources[faceIndex], stack);
			return new TextFace(ftFace, size, heightScale);
		}
	}

	public void returnFace(TextFace face) {
		face.destroy();
	}

	public void destroy() {
		for (var source : faceSources) source.destroy();
	}
}
