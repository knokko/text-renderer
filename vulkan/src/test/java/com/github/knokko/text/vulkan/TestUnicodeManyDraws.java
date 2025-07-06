package com.github.knokko.text.vulkan;

import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.boiler.commands.SingleTimeCommands;
import com.github.knokko.boiler.descriptors.DescriptorCombiner;
import com.github.knokko.boiler.images.ImageBuilder;
import com.github.knokko.boiler.memory.MemoryCombiner;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.text.TextInstance;
import com.github.knokko.text.font.FontData;
import com.github.knokko.text.font.UnicodeFonts;
import com.github.knokko.text.placement.TextAlignment;
import com.github.knokko.text.placement.TextPlaceRequest;
import com.github.knokko.text.util.UnicodeLines;
import org.junit.jupiter.api.Test;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;

import java.util.*;
import java.util.List;

import static com.github.knokko.boiler.utilities.ColorPacker.rgba;
import static com.github.knokko.text.util.ImageChecks.assertImageEquals;
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
			requests.add(new TextPlaceRequest(
					line, 0, minY, width, maxY,
					minY + 20, 15, 1, TextAlignment.DEFAULT, rgba(255, 255, 255, 255)
			));
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

		long alignment = boiler.deviceProperties.limits().minStorageBufferOffsetAlignment();
		var combiner = new MemoryCombiner(boiler, "UnicodeMemory");
		var glyphBuffer = combiner.addMappedBuffer(90_000, alignment, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT);
		var quadBuffer = combiner.addMappedBuffer(30_000, alignment, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT);

		var resultBuffer = combiner.addMappedBuffer(4 * width * height, 4, VK_BUFFER_USAGE_TRANSFER_DST_BIT);
		var image = combiner.addImage(
				new ImageBuilder("DrawImage", width, height)
						.format(colorFormat)
						.addUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT)
						.addUsage(VK_IMAGE_USAGE_TRANSFER_DST_BIT), 1f
		);
		var memory = combiner.build(false);
		var framebuffer = boiler.images.createFramebuffer(
				renderPass, width, height, "TextFramebuffer", image.vkImageView
		);

		var descriptorCombiner = new DescriptorCombiner(boiler);
		long[] descriptorSets = descriptorCombiner.addMultiple(vkTextInstance.descriptorSetLayout, 1);
		var descriptorPool = descriptorCombiner.build("UnicodeDescriptors");

		var vkTextRenderer = vkTextPipeline.createRenderer(
				font, descriptorSets[0], glyphBuffer, quadBuffer, 120, 1
		);

		System.out.println("created resources: " + (System.nanoTime() - startTime) / 1000_000);

		var commands = new SingleTimeCommands(boiler);

		for (var request : requests) {
			commands.submit("Rendering " + request.text, recorder -> {
				if (request == requests.get(0)) {
					recorder.transitionLayout(image, null, ResourceUsage.TRANSFER_DEST);
					recorder.clearColorImage(image.vkImage, 0f, 0f, 0f, 1f);
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
					recorder.copyImageToBuffer(image, resultBuffer);
				}
			}).awaitCompletion();
		}

		System.out.println("finished drawing: " + (System.nanoTime() - startTime) / 1000_000);

		commands.destroy();
		vkTextRenderer.destroy();

		assertImageEquals(
				"expected-unicode-test-result.png",
				resultBuffer.decodeBufferedImage(width, height),
				"actual-unicode-test-result-vulkan-many.png", false
		);

		vkDestroyFramebuffer(boiler.vkDevice(), framebuffer, null);
		vkDestroyDescriptorPool(boiler.vkDevice(), descriptorPool, null);
		memory.destroy(boiler);
		vkTextPipeline.destroy();
		vkDestroyRenderPass(boiler.vkDevice(), renderPass, null);
		vkTextInstance.destroyInitialObjects();
		boiler.destroyInitialObjects();
		font.destroy();
		instance.destroy();

		System.out.println("final: " + (System.nanoTime() - startTime) / 1000_000);
	}
}
