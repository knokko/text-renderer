package com.github.knokko.text.vulkan;

import com.github.knokko.boiler.builder.BoilerBuilder;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.sync.ResourceUsage;
import com.github.knokko.text.TextInstance;
import com.github.knokko.text.bitmap.BitmapGlyphsBuffer;
import com.github.knokko.text.font.UnicodeFonts;
import com.github.knokko.text.placement.TextPlaceRequest;
import org.junit.jupiter.api.Test;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import static com.github.knokko.text.util.ImageChecks.assertImageEquals;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memIntBuffer;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;

public class TestUnicode1Draw {

	@Test
	public void testUnicodeWithOnly1DrawCall() {
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

		var boiler = new BoilerBuilder(
				VK_API_VERSION_1_2, "TestUnicode1Draw", 1
		).validation().forbidValidationErrors().enableDynamicRendering().build();

		var vkTextInstance = new VulkanTextInstance(boiler);
		var vkTextPipeline = vkTextInstance.createPipelineWithDynamicRendering(0, colorFormat, width, height);

		var textDescriptorPool = vkTextInstance.descriptorSetLayout.createPool(1, 0, "TextPool");

		System.out.println("initial: " + (System.nanoTime() - startTime) / 1000_000);

		var glyphBuffer = boiler.buffers.createMapped(3_000_000, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "GlyphBuffer");
		var glyphsBuffer = new BitmapGlyphsBuffer(glyphBuffer.hostAddress(), (int) glyphBuffer.size());
		var quadBuffer = boiler.buffers.createMapped(1_000_000, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "QuadBuffer");
		var quadHostBuffer = memIntBuffer(quadBuffer.hostAddress(), (int) quadBuffer.size() / 4);
		var fence = boiler.sync.createFences(false, 1, "DrawFence")[0];

		var resultBuffer = boiler.buffers.createMapped(4 * width * height, VK_BUFFER_USAGE_TRANSFER_DST_BIT, "ResultBuffer");

		try (var stack = stackPush()) {
			var image = boiler.images.createSimple(
					stack, width, height, colorFormat,
					VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT,
					VK_IMAGE_ASPECT_COLOR_BIT, "DrawImage"
			);

			var descriptorSet = textDescriptorPool.allocate(stack, 1)[0];
			vkTextInstance.updateDescriptorSet(descriptorSet, quadBuffer, glyphBuffer);

			var vkTextRenderer = vkTextPipeline.createRenderer(font, descriptorSet, glyphsBuffer, quadHostBuffer);

			var commandPool = boiler.commands.createPool(0, boiler.queueFamilies().graphics().index(), "CommandPool");
			var commandBuffer = boiler.commands.createPrimaryBuffers(commandPool, 1, "CommandBuffer")[0];

			System.out.println("start recording: " + (System.nanoTime() - startTime) / 1000_000);

			var recorder = CommandRecorder.begin(commandBuffer, boiler, stack, "Drawing");

			recorder.transitionColorLayout(image.vkImage(), null, ResourceUsage.COLOR_ATTACHMENT_WRITE);

			var colorAttachments = VkRenderingAttachmentInfo.calloc(1, stack);
			recorder.simpleColorRenderingAttachment(
					colorAttachments.get(0), image.vkImageView(),
					VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE,
					0f, 0f, 0f, 1f
			);
			recorder.beginSimpleDynamicRendering(width, height, colorAttachments, null, null);

			System.out.println("start heavy lifting: " + (System.nanoTime() - startTime) / 1000_000);
			vkTextRenderer.recordCommands(commandBuffer, stack, width, height, requests);
			System.out.println("finished heavy lifting: " + (System.nanoTime() - startTime) / 1000_000);

			recorder.endDynamicRendering();

			recorder.transitionColorLayout(image.vkImage(), ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.TRANSFER_SOURCE);
			recorder.copyImageToBuffer(VK_IMAGE_ASPECT_COLOR_BIT, image.vkImage(), width, height, resultBuffer.vkBuffer());

			recorder.end();

			System.out.println("finished recording: " + (System.nanoTime() - startTime) / 1000_000);
			boiler.queueFamilies().graphics().queues().get(0).submit(commandBuffer, "Draw", null, fence);
			boiler.sync.waitAndReset(stack, fence);
			System.out.println("finished drawing: " + (System.nanoTime() - startTime) / 1000_000);

			vkDestroyCommandPool(boiler.vkDevice(), commandPool, null);
			image.destroy(boiler);
		}

		assertImageEquals(
				"expected-unicode-test-result.png",
				boiler.buffers.decodeBufferedImageRGBA(resultBuffer, 0, width, height),
				"actual-unicode-test-result-vulkan1.png"
		);

		resultBuffer.destroy(boiler.vmaAllocator());
		vkDestroyFence(boiler.vkDevice(), fence, null);
		glyphBuffer.destroy(boiler.vmaAllocator());
		quadBuffer.destroy(boiler.vmaAllocator());
		textDescriptorPool.destroy();
		vkTextPipeline.destroy();
		vkTextInstance.destroyInitialObjects();
		boiler.destroyInitialObjects();
		font.destroy();
		instance.destroy();
	}
}
