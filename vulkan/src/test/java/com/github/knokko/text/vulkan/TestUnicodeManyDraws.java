package com.github.knokko.text.vulkan;

import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.text.TextInstance;
import com.github.knokko.text.bitmap.BitmapGlyphsBuffer;
import com.github.knokko.text.font.FontData;
import com.github.knokko.text.font.UnicodeFonts;
import com.github.knokko.text.placement.TextPlaceRequest;
import com.github.knokko.text.util.UnicodeLines;
import org.junit.jupiter.api.Test;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;

import java.util.*;
import java.util.List;

import static com.github.knokko.boiler.utilities.ColorPacker.rgba;
import static com.github.knokko.text.util.ImageChecks.assertImageEquals;
import static org.lwjgl.system.MemoryUtil.memIntBuffer;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;

public class TestUnicodeManyDraws {

	@Test
	public void testUnicodeWith1DrawCallPerLine() {
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

		System.out.println("initial: " + (System.nanoTime() - startTime) / 1000_000);

		var boiler = new BoilerBuilder(
				VK_API_VERSION_1_2, "TestUnicode1Draw", 1
		).validation().forbidValidationErrors().defaultTimeout(10_000_000_000L).build();

		var vkTextInstance = new VulkanTextInstance(boiler);
		long renderPass = vkTextInstance.createRenderPass(
				colorFormat, VK_ATTACHMENT_LOAD_OP_LOAD,
				VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
				null, null
		);

		var vkTextPipeline = vkTextInstance.createPipelineWithRenderPass(renderPass, 0, width, height);

		var textDescriptorPool = vkTextInstance.descriptorSetLayout.createPool(1, 0, "TextPool");

		var glyphBuffer = boiler.buffers.createMapped(90_000, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "GlyphBuffer");
		var glyphsBuffer = new BitmapGlyphsBuffer(glyphBuffer.hostAddress(), (int) glyphBuffer.size());
		var quadBuffer = boiler.buffers.createMapped(30_000, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "QuadBuffer");
		var quadHostBuffer = memIntBuffer(quadBuffer.hostAddress(), (int) quadBuffer.size() / 4);

		var resultBuffer = boiler.buffers.createMapped(4 * width * height, VK_BUFFER_USAGE_TRANSFER_DST_BIT, "ResultBuffer");
		var image = boiler.images.createSimple(
				width, height, colorFormat,
				VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT,
				VK_IMAGE_ASPECT_COLOR_BIT, "DrawImage"
		);
		var framebuffer = boiler.images.createFramebuffer(
				renderPass, width, height,
				"TextFramebuffer", image.vkImageView()
		);
		long descriptorSet = textDescriptorPool.allocate(1)[0];
		vkTextInstance.updateDescriptorSet(descriptorSet, quadBuffer.fullRange(), glyphBuffer.fullRange());

		var vkTextRenderer = vkTextPipeline.createRenderer(font, descriptorSet, glyphsBuffer, quadHostBuffer, 1);

		System.out.println("created resources: " + (System.nanoTime() - startTime) / 1000_000);

		var commands = new SingleTimeCommands(boiler);

		for (var request : requests) {
			commands.submit("Rendering " + request.text, recorder -> {
				if (request == requests.get(0)) {
					recorder.transitionLayout(image, null, ResourceUsage.TRANSFER_DEST);
					recorder.clearColorImage(image.vkImage(), 0f, 0f, 0f, 1f);
					recorder.transitionLayout(image, ResourceUsage.TRANSFER_DEST, new ResourceUsage(
							VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
							VK_ACCESS_COLOR_ATTACHMENT_READ_BIT,
							VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
					));
				}

				var biRenderPass = VkRenderPassBeginInfo.calloc(recorder.stack);
				biRenderPass.sType$Default();
				biRenderPass.renderPass(renderPass);
				biRenderPass.framebuffer(framebuffer);
				biRenderPass.renderArea().offset().set(0, 0);
				biRenderPass.renderArea().extent().set(width, height);
				biRenderPass.clearValueCount(0);
				biRenderPass.pClearValues(null);

				vkCmdBeginRenderPass(recorder.commandBuffer, biRenderPass, VK_SUBPASS_CONTENTS_INLINE);
				vkTextRenderer.recordCommands(recorder, width, height, Collections.singletonList(request));
				vkCmdEndRenderPass(recorder.commandBuffer);

				if (request == requests.get(requests.size() - 1)) {
					recorder.transitionLayout(image, ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.TRANSFER_SOURCE);
					recorder.copyImageToBuffer(image, resultBuffer.fullRange());
				}
			}).awaitCompletion();
		}

		System.out.println("finished drawing: " + (System.nanoTime() - startTime) / 1000_000);

		commands.destroy();
		vkTextRenderer.destroy();
		image.destroy(boiler);

		assertImageEquals(
				"expected-unicode-test-result.png",
				boiler.buffers.decodeBufferedImageRGBA(resultBuffer, 0, width, height),
				"actual-unicode-test-result-vulkan-many.png", false
		);

		resultBuffer.destroy(boiler);
		vkDestroyFramebuffer(boiler.vkDevice(), framebuffer, null);
		glyphBuffer.destroy(boiler);
		quadBuffer.destroy(boiler);
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
