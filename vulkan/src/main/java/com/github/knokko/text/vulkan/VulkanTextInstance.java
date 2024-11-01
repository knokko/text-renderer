package com.github.knokko.text.vulkan;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.VkbBufferRange;
import com.github.knokko.boiler.descriptors.VkbDescriptorSetLayout;
import com.github.knokko.boiler.pipelines.GraphicsPipelineBuilder;
import org.lwjgl.vulkan.*;

import static com.github.knokko.boiler.exceptions.VulkanFailureException.assertVkSuccess;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * The 'root' of the hierarchy of the Vulkan implementation of stage 3 of the text rendering pipeline. You should
 * create 1 instance of this class per {@link BoilerInstance}, so probably just 1. This class is completely
 * thread-safe.
 */
public class VulkanTextInstance {

	public final BoilerInstance boiler;

	/**
	 * The {@link VkbDescriptorSetLayout} from which you need to allocate descriptor sets for the text rendering
	 * graphics pipeline.
	 */
	public final VkbDescriptorSetLayout descriptorSetLayout;

	/**
	 * The <i>VkPipelineLayout</i> that all text graphics pipelines will have
	 */
	public final long pipelineLayout;

	/**
	 * Constructs a new {@link VulkanTextInstance} using the given {@link BoilerInstance}
	 */
	public VulkanTextInstance(BoilerInstance boiler) {
		this.boiler = boiler;
		try (var stack = stackPush()) {
			var bindings = VkDescriptorSetLayoutBinding.calloc(2, stack);
			boiler.descriptors.binding(bindings, 0, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_VERTEX_BIT);
			boiler.descriptors.binding(bindings, 1, VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK_SHADER_STAGE_FRAGMENT_BIT);
			descriptorSetLayout = boiler.descriptors.createLayout(stack, bindings, "TextBuffersLayout");

			var pushConstants = VkPushConstantRange.calloc(1, stack);
			pushConstants.stageFlags(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT);
			pushConstants.offset(0);
			pushConstants.size(8);
			pipelineLayout = boiler.pipelines.createLayout(
					pushConstants, "TextPipelineLayout", descriptorSetLayout.vkDescriptorSetLayout
			);
		}
	}

	/**
	 * Creates a <i>VkRenderpass</i> that will be compatible with the text rendering graphics pipelines.
	 * @param framebufferFormat {@link VkAttachmentDescription#format()}
	 * @param loadOp {@link VkAttachmentDescription#loadOp()}
	 * @param initialLayout {@link VkAttachmentDescription#initialLayout()}
	 * @param finalLayout {@link VkAttachmentDescription#finalLayout()}
	 * @param dstStageMask {@link VkSubpassDependency#dstStageMask()}
	 * @param dstAccessMask {@link VkSubpassDependency#dstAccessMask()}
	 * @return The created <i>VkRenderPass</i>
	 */
	public long createRenderPass(
			int framebufferFormat, int loadOp, int initialLayout, int finalLayout,
			Integer dstStageMask, Integer dstAccessMask
	) {
		if ((dstStageMask == null) != (dstAccessMask == null)) {
			throw new IllegalArgumentException("dstStageMask must be null if and only if dstAccessMask is null");
		}

		try (var stack = stackPush()) {
			var attachments = VkAttachmentDescription.calloc(1, stack);
			var colorAttachment = attachments.get(0);
			colorAttachment.flags(0);
			colorAttachment.format(framebufferFormat);
			colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
			colorAttachment.loadOp(loadOp);
			colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
			colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
			colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
			colorAttachment.initialLayout(initialLayout);
			colorAttachment.finalLayout(finalLayout);

			var colorReferences = VkAttachmentReference.calloc(1, stack);
			var colorReference = colorReferences.get(0);
			colorReference.attachment(0);
			colorReference.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

			var subpasses = VkSubpassDescription.calloc(1, stack);
			var subpass = subpasses.get(0);
			subpass.flags(0);
			subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
			subpass.pInputAttachments(null);
			subpass.colorAttachmentCount(1);
			subpass.pColorAttachments(colorReferences);
			subpass.pResolveAttachments(null);
			subpass.pDepthStencilAttachment(null);
			subpass.pPreserveAttachments(null);

			var dependencies = VkSubpassDependency.calloc(1, stack);
			if (dstAccessMask != null) {
				var copyDependency = dependencies.get(0);
				copyDependency.srcSubpass(0);
				copyDependency.dstSubpass(VK_SUBPASS_EXTERNAL);
				copyDependency.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
				copyDependency.dstStageMask(dstStageMask);
				copyDependency.srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
				copyDependency.dstAccessMask(dstAccessMask);
				copyDependency.dependencyFlags(0);
			}

			var ciRenderPass = VkRenderPassCreateInfo.calloc(stack);
			ciRenderPass.sType$Default();
			ciRenderPass.flags(0);
			ciRenderPass.pAttachments(attachments);
			ciRenderPass.pSubpasses(subpasses);
			if (dstAccessMask != null) ciRenderPass.pDependencies(dependencies);
			else ciRenderPass.pDependencies(null);

			var pRenderPass = stack.callocLong(1);
			assertVkSuccess(vkCreateRenderPass(
					boiler.vkDevice(), ciRenderPass, null, pRenderPass
			), "CreateRenderPass", "TextRenderPass");
			return pRenderPass.get(0);
		}
	}

	/**
	 * Calls <i>vkUpdateDescriptorSets</i> to update the given <i>VkDescriptorSet</i>. You need to call this once
	 * after allocating {@code descriptorSet}, and after each time you change the {@code quadBuffer} and/or
	 * {@code glyphBuffer}.
	 * @param descriptorSet The descriptor set to be updated. It should have the layout {@link #descriptorSetLayout}
	 * @param quadBuffer The {@link VkbBufferRange} containing all the <i>GlyphQuad</i> data
	 * @param glyphBuffer The {@link VkbBufferRange} containing all the rasterized glyphs, which should be mapped to
	 *                    the {@link com.github.knokko.text.bitmap.BitmapGlyphsBuffer}
	 */
	public void updateDescriptorSet(long descriptorSet, VkbBufferRange quadBuffer, VkbBufferRange glyphBuffer) {
		try (var stack = stackPush()) {
			var descriptorWrites = VkWriteDescriptorSet.calloc(2, stack);
			boiler.descriptors.writeBuffer(
					stack, descriptorWrites, descriptorSet, 0,
					VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, quadBuffer
			);
			boiler.descriptors.writeBuffer(
					stack, descriptorWrites, descriptorSet, 1,
					VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, glyphBuffer
			);
			vkUpdateDescriptorSets(boiler.vkDevice(), descriptorWrites, null);
		}
	}

	/**
	 * Creates a {@link VulkanTextPipeline} that will be compatible with the given {@code subpass} of the given
	 * {@code renderPass}. You should probably have used {@link #createRenderPass} to create the given
	 * {@code renderPass}, but that's not a hard requirement.
	 * <ul>
	 *     <li>
	 *         When {@code framebufferWidth} and {@code framebufferHeight} are null, the pipeline will have a dynamic
	 *         viewport state and scissor state.
	 *     </li>
	 *     <li>
	 *         When {@code framebufferWidth} and {@code framebufferHeight} are not null, the pipeline will have a
	 *         fixed viewport and scissor of {@code framebufferWidth} by {@code framebufferHeight} pixels.
	 *     </li>
	 * </ul>
	 */
	public VulkanTextPipeline createPipelineWithRenderPass(
			long renderPass, int subpass, Integer framebufferWidth, Integer framebufferHeight
	) {
		return createPipeline(renderPass, subpass, 0, 0, framebufferWidth, framebufferHeight);
	}

	/**
	 * Creates a {@link VulkanTextPipeline} that will use dynamic rendering. the {@code viewMask} will be propagated to
	 * {@link VkPipelineRenderingCreateInfo#viewMask()} and the {@code colorAttachmentFormat} will be propagated to
	 * {@link VkPipelineRenderingCreateInfo#pColorAttachmentFormats()}.
	 * <ul>
	 *     <li>
	 *         When {@code framebufferWidth} and {@code framebufferHeight} are null, the pipeline will have a dynamic
	 *         viewport state and scissor state.
	 *     </li>
	 *     <li>
	 *         When {@code framebufferWidth} and {@code framebufferHeight} are not null, the pipeline will have a
	 *         fixed viewport and scissor of {@code framebufferWidth} by {@code framebufferHeight} pixels.
	 *     </li>
	 * </ul>
	 */
	public VulkanTextPipeline createPipelineWithDynamicRendering(
			int viewMask, int colorAttachmentFormat, Integer framebufferWidth, Integer framebufferHeight
	) {
		return createPipeline(null, 0, viewMask, colorAttachmentFormat, framebufferWidth, framebufferHeight);
	}

	private VulkanTextPipeline createPipeline(
			Long renderPass, int subpass,
			int viewMask, int colorAttachmentFormat,
			Integer framebufferWidth, Integer framebufferHeight
	) {
		if ((framebufferWidth == null) != (framebufferHeight == null)) {
			throw new IllegalArgumentException("framebufferWidth must be null if and only if framebufferHeight is null");
		}

		try (var stack = stackPush()) {
			var pipeline = new GraphicsPipelineBuilder(boiler, stack);
			pipeline.simpleShaderStages(
					"Text", "com/github/knokko/text/vulkan/shader.vert.spv",
					"com/github/knokko/text/vulkan/shader.frag.spv"
			);
			pipeline.noVertexInput();
			pipeline.simpleInputAssembly();
			pipeline.ciPipeline.pTessellationState(null);
			if (framebufferWidth != null) {
				pipeline.fixedViewport(framebufferWidth, framebufferHeight);
			} else {
				pipeline.dynamicViewports(1);
			}
			pipeline.simpleRasterization(VK_CULL_MODE_NONE);
			pipeline.noMultisampling();
			pipeline.noDepthStencil();
			pipeline.simpleColorBlending(1);
			if (framebufferWidth != null) {
				pipeline.ciPipeline.pDynamicState(null);
			} else {
				pipeline.dynamicStates(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR);
			}
			pipeline.ciPipeline.layout(pipelineLayout);
			if (renderPass != null) {
				pipeline.ciPipeline.renderPass(renderPass);
				pipeline.ciPipeline.subpass(subpass);
			} else {
				pipeline.dynamicRendering(viewMask, VK_FORMAT_UNDEFINED, VK_FORMAT_UNDEFINED, colorAttachmentFormat);
			}
			pipeline.ciPipeline.basePipelineHandle(VK_NULL_HANDLE);
			pipeline.ciPipeline.basePipelineIndex(0);

			long graphicsPipeline = pipeline.build("TextPipeline");

			return new VulkanTextPipeline(this, graphicsPipeline, framebufferWidth == null);
		}
	}

	/**
	 * Destroys all Vulkan objects that were created in the constructor of this class
	 */
	public void destroyInitialObjects() {
		descriptorSetLayout.destroy();
		vkDestroyPipelineLayout(boiler.vkDevice(), pipelineLayout, null);
	}
}
