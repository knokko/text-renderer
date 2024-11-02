package com.github.knokko.text.vulkan;

import com.github.knokko.boiler.buffers.MappedVkbBufferRange;
import com.github.knokko.text.bitmap.BitmapGlyphsBuffer;
import com.github.knokko.text.font.FontData;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

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
	 *                      {@link BitmapGlyphsBuffer#BitmapGlyphsBuffer(long, int, int)}
	 * @param numTextPlacerThreads The number of threads that the {@link com.github.knokko.text.placement.TextPlacer}
	 *                             of the renderer will use
	 * @return The created renderer
	 */
	public VulkanTextRenderer createRenderer(
			FontData font, long descriptorSet,
			MappedVkbBufferRange glyphBuffer,
			MappedVkbBufferRange quadBuffer,
			int glyphSlotSize, int numTextPlacerThreads
	) {
		try (var stack = stackPush()) {
			var descriptorWrites = VkWriteDescriptorSet.calloc(2, stack);
			instance.boiler.descriptors.writeBuffer(
					stack, descriptorWrites, descriptorSet, 0,
					VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, quadBuffer.range()
			);
			instance.boiler.descriptors.writeBuffer(
					stack, descriptorWrites, descriptorSet, 1,
					VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, glyphBuffer.range()
			);
			vkUpdateDescriptorSets(instance.boiler.vkDevice(), descriptorWrites, null);
		}

		var glyphsBuffer = new BitmapGlyphsBuffer(glyphBuffer.hostAddress(), glyphBuffer.intSize(), glyphSlotSize);
		return new VulkanTextRenderer(
				font, instance, this, descriptorSet, glyphsBuffer, quadBuffer.intBuffer(), numTextPlacerThreads
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
