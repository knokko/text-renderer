package com.github.knokko.text.vulkan;

import com.github.knokko.boiler.builder.BoilerBuilder;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.images.VmaImage;
import com.github.knokko.boiler.sync.ResourceUsage;
import com.github.knokko.text.TextInstance;
import com.github.knokko.text.bitmap.BitmapGlyphsBuffer;
import com.github.knokko.text.font.UnicodeFonts;
import com.github.knokko.text.placement.TextPlaceRequest;
import org.lwjgl.vulkan.*;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.vma.Vma.vmaDestroyImage;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;

public class VulkanPlayground {

	public static void main(String[] args) {
		var boiler = new BoilerBuilder(
				VK_API_VERSION_1_2, "TextPlayground", 1
		).validation().enableDynamicRendering().build();

		var requests = new ArrayList<TextPlaceRequest>();
		requests.add(new TextPlaceRequest(
				"Hello, world!", 10, 20, 500, 100, false, null
		));

		var vulkanTextInstance = new VulkanTextInstance(boiler);

		int framebufferFormat = VK_FORMAT_R8G8B8A8_UNORM;

		int framebufferWidth = 1000;
		int framebufferHeight = 500;

		VmaImage framebufferImage;
		try (var stack = stackPush()) {
			framebufferImage = boiler.images.createSimple(
					stack, framebufferWidth, framebufferHeight, framebufferFormat,
					VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT,
					VK_IMAGE_ASPECT_COLOR_BIT, "FramebufferImage"
			);
		}

		var quadBuffer = boiler.buffers.createMapped(50_000, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "QuadBuffer");
		var glyphBuffer = boiler.buffers.createMapped(50_000, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "GlyphsBuffer");
		var glyphsBuffer = new BitmapGlyphsBuffer(glyphBuffer.hostAddress(), (int) glyphBuffer.size());

		var descriptorPool = vulkanTextInstance.descriptorSetLayout.createPool(1, 0, "TextPool");
		long descriptorSet;
		try (var stack = stackPush()) {
			descriptorSet = descriptorPool.allocate(stack, 1)[0];

			vulkanTextInstance.updateDescriptorSet(descriptorSet, quadBuffer, glyphBuffer);
		}

		var textPipeline = vulkanTextInstance.createPipelineWithDynamicRendering(
				0, framebufferFormat, framebufferWidth, framebufferHeight
		);

		var textInstance = new TextInstance();
		var font = textInstance.createFont(UnicodeFonts.SOURCE);

		var textRenderer = textPipeline.createRenderer(
				font, descriptorSet, glyphsBuffer,
				memIntBuffer(quadBuffer.hostAddress(), Math.toIntExact(quadBuffer.size()))
		);

		var destinationBuffer = boiler.buffers.createMapped(
				4 * framebufferWidth * framebufferHeight, VK_BUFFER_USAGE_TRANSFER_DST_BIT, "CopyDestination"
		);

		var commandPool = boiler.commands.createPool(
				VK_COMMAND_POOL_CREATE_TRANSIENT_BIT,
				boiler.queueFamilies().graphics().index(),
				"DrawPool"
		);
		var commandBuffer = boiler.commands.createPrimaryBuffers(commandPool, 1, "DrawCmdBuffer")[0];
		var fence = boiler.sync.createFences(false, 1, "Drawing")[0];
		try (var stack = stackPush()) {
			var recorder = CommandRecorder.begin(commandBuffer, boiler, stack, "Drawing");

			recorder.transitionColorLayout(framebufferImage.vkImage(), null, ResourceUsage.COLOR_ATTACHMENT_WRITE);

			var colorAttachments = VkRenderingAttachmentInfo.calloc(1, stack);
			recorder.simpleColorRenderingAttachment(
					colorAttachments.get(0), framebufferImage.vkImageView(),
					VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE,
					0f, 1f, 0f, 1f
			);
			recorder.beginSimpleDynamicRendering(
					framebufferWidth, framebufferHeight, colorAttachments, null, null
			);

			textRenderer.recordCommands(commandBuffer, stack, framebufferWidth, framebufferHeight, requests);

			recorder.endDynamicRendering();

			recorder.transitionColorLayout(
					framebufferImage.vkImage(), ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.TRANSFER_SOURCE
			);

			recorder.copyImageToBuffer(
					VK_IMAGE_ASPECT_COLOR_BIT, framebufferImage.vkImage(),
					framebufferWidth, framebufferHeight, destinationBuffer.vkBuffer()
			);

			recorder.end();

			boiler.queueFamilies().graphics().queues().get(0).submit(commandBuffer, "Draw", null, fence);
			boiler.sync.waitAndReset(stack, fence);

			var destinationImage = boiler.buffers.decodeBufferedImageRGBA(
					destinationBuffer, 0, framebufferWidth, framebufferHeight
			);
			try {
				ImageIO.write(destinationImage, "PNG", new File("test.png"));
			} catch (IOException io) {
				throw new RuntimeException(io);
			}
		}

		descriptorPool.destroy();
		quadBuffer.destroy(boiler.vmaAllocator());
		destinationBuffer.destroy(boiler.vmaAllocator());
		glyphBuffer.destroy(boiler.vmaAllocator());
		vkDestroyImageView(boiler.vkDevice(), framebufferImage.vkImageView(), null);
		vmaDestroyImage(boiler.vmaAllocator(), framebufferImage.vkImage(), framebufferImage.vmaAllocation());
		vkDestroyFence(boiler.vkDevice(), fence, null);
		vkDestroyCommandPool(boiler.vkDevice(), commandPool, null);
		textPipeline.destroy();
		vulkanTextInstance.destroyInitialObjects();
		font.destroy();
		textInstance.destroy();

		boiler.destroyInitialObjects();
	}
}
