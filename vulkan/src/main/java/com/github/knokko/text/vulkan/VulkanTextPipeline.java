package com.github.knokko.text.vulkan;

import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.descriptors.DescriptorUpdater;
import com.github.knokko.boiler.memory.callbacks.CallbackUserData;
import com.github.knokko.text.bitmap.BitmapGlyphsBuffer;
import com.github.knokko.text.font.FontData;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
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
	 * @param glyphBuffer the buffer where the {@link BitmapGlyphsBuffer} will store its rasterized glyphs
	 * @param quadBuffer the buffer where the renderer will store the quads
	 * @param glyphSlotSize The slot size that will be used by the {@link BitmapGlyphsBuffer}, see
	 *                      {@link BitmapGlyphsBuffer#BitmapGlyphsBuffer(ByteBuffer, int)}
	 * @param numTextPlacerThreads The number of threads that the {@link com.github.knokko.text.placement.TextPlacer}
	 *                             of the renderer will use
	 * @return The created renderer
	 */
	public VulkanTextRenderer createRenderer(
			FontData font, long descriptorSet,
			MappedVkbBuffer glyphBuffer,
			MappedVkbBuffer quadBuffer,
			int glyphSlotSize, int numTextPlacerThreads
	) {
		try (var stack = stackPush()) {
			var updater = new DescriptorUpdater(stack, 2);
			updater.writeStorageBuffer(0, descriptorSet, 0, quadBuffer);
			updater.writeStorageBuffer(1, descriptorSet, 1, glyphBuffer);
			updater.update(instance.boiler);
		}

		var glyphsBuffer = new BitmapGlyphsBuffer(glyphBuffer.byteBuffer(), glyphSlotSize);
		return new VulkanTextRenderer(
				font, instance, this, descriptorSet, glyphsBuffer, quadBuffer.intBuffer(), numTextPlacerThreads
		);
	}

	/**
	 * Destroys this text graphics pipeline. You should call this when you no longer need this pipeline, nor any of
	 * the renderers created for it
	 */
	public void destroy() {
		try (var stack = stackPush()) {
			vkDestroyPipeline(
					instance.boiler.vkDevice(), graphicsPipeline,
					CallbackUserData.PIPELINE.put(stack, instance.boiler)
			);
		}
	}
}
