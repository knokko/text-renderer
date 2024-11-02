package com.github.knokko.text.vulkan;

import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.text.TextInstance;
import com.github.knokko.text.font.FontData;
import com.github.knokko.text.font.UnicodeFonts;
import com.github.knokko.text.placement.TextPlaceRequest;
import com.github.knokko.text.util.UnicodeLines;
import org.junit.jupiter.api.Test;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;

import java.util.ArrayList;
import java.util.List;

import static com.github.knokko.boiler.utilities.ColorPacker.rgba;
import static com.github.knokko.text.util.ImageChecks.assertImageEquals;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;

public class TestUnicode1Draw {

	@Test
	public void testUnicodeWithOnly1DrawCall() {
		long startTime = System.nanoTime();
		int width = 3500;
		int height = 9700;
		int colorFormat = VK_FORMAT_R8G8B8A8_UNORM;

		var instance = new TextInstance();
		var font = new FontData(instance, UnicodeFonts.SOURCE);

		List<TextPlaceRequest> requests = new ArrayList<>();
		int minY = 5;
		for (String line : UnicodeLines.get()) {
			int maxY = minY + 40;
			requests.add(new TextPlaceRequest(line, 0, minY, width, maxY, minY + 20, 15, rgba(255, 255, 255, 255)));
			minY = maxY;
		}

		var boiler = new BoilerBuilder(
				VK_API_VERSION_1_2, "TestUnicode1Draw", 1
		).validation().forbidValidationErrors().enableDynamicRendering().defaultTimeout(10_000_000_000L).build();

		var vkTextInstance = new VulkanTextInstance(boiler);
		var vkTextPipeline = vkTextInstance.createPipelineWithDynamicRendering(0, colorFormat, width, height);

		var textDescriptorPool = vkTextInstance.descriptorSetLayout.createPool(1, 0, "TextPool");

		System.out.println("initial: " + (System.nanoTime() - startTime) / 1000_000);

		var glyphBuffer = boiler.buffers.createMapped(3_000_000, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "GlyphBuffer");
		var quadBuffer = boiler.buffers.createMapped(1_000_000, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "QuadBuffer");

		var resultBuffer = boiler.buffers.createMapped(4 * width * height, VK_BUFFER_USAGE_TRANSFER_DST_BIT, "ResultBuffer");
		var image = boiler.images.createSimple(
				width, height, colorFormat,
				VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT,
				VK_IMAGE_ASPECT_COLOR_BIT, "DrawImage"
		);

		var descriptorSet = textDescriptorPool.allocate(1)[0];

		var vkTextRenderer = vkTextPipeline.createRenderer(
				font, descriptorSet, glyphBuffer.fullMappedRange(),
				quadBuffer.fullMappedRange(), 120, 10
		);

		var commands = new SingleTimeCommands(boiler);
		commands.submit("TextRendering", recorder -> {
			System.out.println("start recording: " + (System.nanoTime() - startTime) / 1000_000);

			recorder.transitionLayout(image, null, ResourceUsage.COLOR_ATTACHMENT_WRITE);

			var colorAttachments = VkRenderingAttachmentInfo.calloc(1, recorder.stack);
			recorder.simpleColorRenderingAttachment(
					colorAttachments.get(0), image.vkImageView(),
					VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE,
					0f, 0f, 0f, 1f
			);
			recorder.beginSimpleDynamicRendering(width, height, colorAttachments, null, null);

			System.out.println("start heavy lifting: " + (System.nanoTime() - startTime) / 1000_000);
			vkTextRenderer.recordCommands(recorder, width, height, requests);
			System.out.println("finished heavy lifting: " + (System.nanoTime() - startTime) / 1000_000);

			recorder.endDynamicRendering();
			recorder.transitionLayout(image, ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.TRANSFER_SOURCE);
			recorder.copyImageToBuffer(image, resultBuffer.fullRange());

			System.out.println("finished recording: " + (System.nanoTime() - startTime) / 1000_000);
		}).awaitCompletion();
		System.out.println("finished drawing: " + (System.nanoTime() - startTime) / 1000_000);
		commands.destroy();

		image.destroy(boiler);
		vkTextRenderer.destroy();

		assertImageEquals(
				"expected-unicode-test-result.png",
				boiler.buffers.decodeBufferedImageRGBA(resultBuffer, 0, width, height),
				"actual-unicode-test-result-vulkan1.png", false
		);

		resultBuffer.destroy(boiler);
		glyphBuffer.destroy(boiler);
		quadBuffer.destroy(boiler);
		textDescriptorPool.destroy();
		vkTextPipeline.destroy();
		vkTextInstance.destroyInitialObjects();
		boiler.destroyInitialObjects();
		font.destroy();
		instance.destroy();
	}
}
