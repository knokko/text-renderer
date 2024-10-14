package com.github.knokko.text.vulkan;

import com.github.knokko.text.bitmap.BitmapGlyphsBuffer;
import com.github.knokko.text.font.FontData;

import java.nio.IntBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanTextPipeline {

	private final VulkanTextInstance instance;
	public final long graphicsPipeline;

	final boolean hasDynamicViewport;

	VulkanTextPipeline(VulkanTextInstance instance, long graphicsPipeline, boolean hasDynamicViewport) {
		this.instance = instance;
		this.graphicsPipeline = graphicsPipeline;
		this.hasDynamicViewport = hasDynamicViewport;
	}

	public VulkanTextRenderer createRenderer(
			FontData font, long descriptorSet, BitmapGlyphsBuffer glyphsBuffer,
			IntBuffer quadBuffer, int numTextPlacerThreads
	) {
		return new VulkanTextRenderer(
				font, instance, this, descriptorSet, glyphsBuffer, quadBuffer, numTextPlacerThreads
		);
	}

	public void destroy() {
		vkDestroyPipeline(instance.boiler.vkDevice(), graphicsPipeline, null);
	}
}
