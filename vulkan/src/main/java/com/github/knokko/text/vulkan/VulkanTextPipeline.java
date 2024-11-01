package com.github.knokko.text.vulkan;

import com.github.knokko.text.bitmap.BitmapGlyphsBuffer;
import com.github.knokko.text.font.FontData;

import java.nio.IntBuffer;

import static org.lwjgl.vulkan.VK10.*;

/**
 * This class wraps a Vulkan graphics pipeline for text rendering. To create an instance of this class, you need to
 * call {@link VulkanTextInstance#createPipelineWithRenderPass} or
 * {@link VulkanTextInstance#createPipelineWithDynamicRendering}. This class is completely thread-safe.
 */
public class VulkanTextPipeline {

	private final VulkanTextInstance instance;

	/**
	 * The Vulkan graphics pipeline handle
	 */
	public final long graphicsPipeline;

	final boolean hasDynamicViewport;

	VulkanTextPipeline(VulkanTextInstance instance, long graphicsPipeline, boolean hasDynamicViewport) {
		this.instance = instance;
		this.graphicsPipeline = graphicsPipeline;
		this.hasDynamicViewport = hasDynamicViewport;
	}

	/**
	 * Creates a {@link VulkanTextRenderer} that will use this graphics pipeline
	 * @param font The font(s) that the renderer will use
	 * @param descriptorSet The <i>VkDescriptorSet</i> that the renderer will bind before drawing
	 * @param glyphsBuffer the {@link BitmapGlyphsBuffer} where the renderer will store its rasterized glyphs
	 * @param quadBuffer the buffer where the renderer will store the quads
	 * @param numTextPlacerThreads The number of threads that the {@link com.github.knokko.text.placement.TextPlacer}
	 *                             of the renderer will use
	 * @return The created renderer
	 */
	public VulkanTextRenderer createRenderer(
			FontData font, long descriptorSet, BitmapGlyphsBuffer glyphsBuffer,
			IntBuffer quadBuffer, int numTextPlacerThreads
	) {
		return new VulkanTextRenderer(
				font, instance, this, descriptorSet, glyphsBuffer, quadBuffer, numTextPlacerThreads
		);
	}

	/**
	 * Destroys this text graphics pipeline. You should call this when you no longer need this pipeline, nor any of
	 * the renderers created for it
	 */
	public void destroy() {
		vkDestroyPipeline(instance.boiler.vkDevice(), graphicsPipeline, null);
	}
}
