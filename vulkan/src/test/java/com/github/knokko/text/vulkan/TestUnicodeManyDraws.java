package com.github.knokko.text.vulkan;

import com.github.knokko.boiler.builder.BoilerBuilder;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.images.VmaImage;
import com.github.knokko.boiler.sync.ResourceUsage;
import com.github.knokko.text.TextInstance;
import com.github.knokko.text.bitmap.BitmapGlyphsBuffer;
import com.github.knokko.text.font.UnicodeFonts;
import com.github.knokko.text.placement.TextPlaceRequest;
import org.junit.jupiter.api.Test;
import org.lwjgl.vulkan.VkClearColorValue;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

import static com.github.knokko.boiler.exceptions.VulkanFailureException.assertVkSuccess;
import static com.github.knokko.text.util.ImageChecks.assertImageEquals;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memIntBuffer;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;

public class TestUnicodeManyDraws {

	@Test
	public void testUnicodeWith1DrawCallPerLine() {
		long startTime = System.nanoTime();
		int width = 2000;
		int height = 20_000;
		int colorFormat = VK_FORMAT_R8G8B8A8_UNORM;

		var instance = new TextInstance();
		var font = instance.createFont(UnicodeFonts.SOURCE);

		var scanner = new Scanner(Objects.requireNonNull(
				TestUnicode1Draw.class.getClassLoader().getResourceAsStream("unicode-3.2-test-page.html")
		), StandardCharsets.UTF_8);

		List<TextPlaceRequest> requests = new ArrayList<>();
		int minY = 5;
		while (scanner.hasNextLine()) {
			int maxY = minY + 40;
			requests.add(new TextPlaceRequest(scanner.nextLine(), 0, minY, width, maxY, false, Color.WHITE));
			minY = maxY;
		}
		scanner.close();

		System.out.println("initial: " + (System.nanoTime() - startTime) / 1000_000);

		var boiler = new BoilerBuilder(
				VK_API_VERSION_1_2, "TestUnicode1Draw", 1
		).validation().forbidValidationErrors().build();

		var vkTextInstance = new VulkanTextInstance(boiler);
		long renderPass = vkTextInstance.createRenderPass(
				colorFormat, VK_ATTACHMENT_LOAD_OP_LOAD,
				VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
				null, null
		);

		var vkTextPipeline = vkTextInstance.createPipelineWithRenderPass(renderPass, 0, width, height);

		var textDescriptorPool = vkTextInstance.descriptorSetLayout.createPool(1, 0, "TextPool");

		var glyphBuffer = boiler.buffers.createMapped(30_000, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "GlyphBuffer");
		var glyphsBuffer = new BitmapGlyphsBuffer(glyphBuffer.hostAddress(), (int) glyphBuffer.size());
		var quadBuffer = boiler.buffers.createMapped(10_000, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "QuadBuffer");
		var quadHostBuffer = memIntBuffer(quadBuffer.hostAddress(), (int) quadBuffer.size() / 4);
		var fence = boiler.sync.createFences(false, 1, "DrawFence")[0];

		var resultBuffer = boiler.buffers.createMapped(4 * width * height, VK_BUFFER_USAGE_TRANSFER_DST_BIT, "ResultBuffer");
		VmaImage image;
		long framebuffer;
		long descriptorSet;

		try (var stack = stackPush()) {
			image = boiler.images.createSimple(
					stack, width, height, colorFormat,
					VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT,
					VK_IMAGE_ASPECT_COLOR_BIT, "DrawImage"
			);
			framebuffer = boiler.images.createFramebuffer(
					stack, renderPass, width, height,
					"TextFramebuffer", image.vkImageView()
			);

			descriptorSet = textDescriptorPool.allocate(stack, 1)[0];
			vkTextInstance.updateDescriptorSet(descriptorSet, quadBuffer, glyphBuffer);
		}

		var commandPool = boiler.commands.createPool(0, boiler.queueFamilies().graphics().index(), "CommandPool");
		var commandBuffer = boiler.commands.createPrimaryBuffers(commandPool, 1, "CommandBuffer")[0];

		var vkTextRenderer = vkTextPipeline.createRenderer(font, descriptorSet, glyphsBuffer, quadHostBuffer);

		System.out.println("created resources: " + (System.nanoTime() - startTime) / 1000_000);

		for (var request : requests) {
			try (var stack = stackPush()) {
				var recorder = CommandRecorder.begin(commandBuffer, boiler, stack, "Drawing");

				if (request == requests.get(0)) {
					recorder.transitionColorLayout(image.vkImage(), null, ResourceUsage.TRANSFER_DEST);

					var clearColor = VkClearColorValue.calloc(stack);
					clearColor.float32(0, 0f);
					clearColor.float32(1, 0f);
					clearColor.float32(2, 0f);
					clearColor.float32(3, 1f);

					vkCmdClearColorImage(
							commandBuffer, image.vkImage(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, clearColor,
							boiler.images.subresourceRange(stack, null, VK_IMAGE_ASPECT_COLOR_BIT)
					);
					recorder.transitionColorLayout(
							image.vkImage(), ResourceUsage.TRANSFER_DEST, new ResourceUsage(
									VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
									VK_ACCESS_COLOR_ATTACHMENT_READ_BIT,
									VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
							)
					);
				}

				var biRenderPass = VkRenderPassBeginInfo.calloc(stack);
				biRenderPass.sType$Default();
				biRenderPass.renderPass(renderPass);
				biRenderPass.framebuffer(framebuffer);
				biRenderPass.renderArea().offset().set(0, 0);
				biRenderPass.renderArea().extent().set(width, height);
				biRenderPass.clearValueCount(0);
				biRenderPass.pClearValues(null);

				vkCmdBeginRenderPass(commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE);

				vkTextRenderer.recordCommands(commandBuffer, stack, width, height, Collections.singletonList(request));

				vkCmdEndRenderPass(commandBuffer);

				if (request == requests.get(requests.size() - 1)) {
					recorder.transitionColorLayout(image.vkImage(), ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.TRANSFER_SOURCE);
					recorder.copyImageToBuffer(VK_IMAGE_ASPECT_COLOR_BIT, image.vkImage(), width, height, resultBuffer.vkBuffer());
				}

				recorder.end();

				boiler.queueFamilies().graphics().queues().get(0).submit(commandBuffer, "Draw", null, fence);
				boiler.sync.waitAndReset(stack, fence);
				assertVkSuccess(vkResetCommandPool(boiler.vkDevice(), commandPool, 0), "ResetCommandPool", "draw");
			}
		}

		System.out.println("finished drawing: " + (System.nanoTime() - startTime) / 1000_000);

		vkDestroyCommandPool(boiler.vkDevice(), commandPool, null);
		image.destroy(boiler);

		assertImageEquals(
				"expected-unicode-test-result.png",
				boiler.buffers.decodeBufferedImageRGBA(resultBuffer, 0, width, height),
				"actual-unicode-test-result-vulkan-many.png"
		);

		resultBuffer.destroy(boiler.vmaAllocator());
		vkDestroyFramebuffer(boiler.vkDevice(), framebuffer, null);
		vkDestroyFence(boiler.vkDevice(), fence, null);
		glyphBuffer.destroy(boiler.vmaAllocator());
		quadBuffer.destroy(boiler.vmaAllocator());
		textDescriptorPool.destroy();
		vkTextPipeline.destroy();
		vkDestroyRenderPass(boiler.vkDevice(), renderPass, null);
		vkTextInstance.destroyInitialObjects();
		boiler.destroyInitialObjects();
		font.destroy();
		instance.destroy();

		System.out.println("final: " + (System.nanoTime() - startTime) / 1000_000);
	}
}
