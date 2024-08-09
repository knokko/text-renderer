package com.github.knokko.text.vulkan;

import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.text.bitmap.BitmapGlyphsBuffer;
import com.github.knokko.text.bitmap.GlyphQuad;
import com.github.knokko.text.bitmap.GlyphRasterizer;
import com.github.knokko.text.font.TextFont;
import com.github.knokko.text.placement.TextPlaceRequest;
import com.github.knokko.text.placement.TextPlacer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.awt.*;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanTextRenderer {

	private static final int QUAD_INTS = 9;
	private static final int QUAD_BYTES = QUAD_INTS * 4;

	private final VulkanTextInstance instance;
	private final VulkanTextPipeline pipeline;
	private final long descriptorSet;
	private final BitmapGlyphsBuffer glyphsBuffer;
	private final IntBuffer quadBuffer;

	private final TextPlacer placer;
	private final GlyphRasterizer rasterizer;


	public VulkanTextRenderer(
			TextFont font, VulkanTextInstance instance, VulkanTextPipeline pipeline,
			long descriptorSet, BitmapGlyphsBuffer glyphsBuffer, IntBuffer quadBuffer
	) {
		this.instance = instance;
		this.pipeline = pipeline;
		this.descriptorSet = descriptorSet;
		this.glyphsBuffer = glyphsBuffer;
		this.quadBuffer = quadBuffer;

		this.placer = new TextPlacer(font);
		this.rasterizer = font.rasterizer;
	}

	public void recordCommands(
			VkCommandBuffer commandBuffer, MemoryStack stack,
			int framebufferWidth, int framebufferHeight,
			List<TextPlaceRequest> requests
	) {
		long startTime = System.nanoTime();
		var placedGlyphs = placer.place(requests);
		System.out.println("placing " + requests.size() + " requests took " + (System.nanoTime() - startTime) / 1000_000);
		glyphsBuffer.startFrame();

		startTime = System.nanoTime();
		var placedQuads = glyphsBuffer.bufferGlyphs(rasterizer, placedGlyphs);
		System.out.println("buffering " + placedGlyphs.size() + " glyphs took " + (System.nanoTime() - startTime) / 1000_000);
		if (placedQuads.size() * QUAD_INTS > quadBuffer.remaining()) {
			throw new IllegalArgumentException("Quad buffer is too small: needed " +
					placedQuads.size() * QUAD_INTS + ", but got " + quadBuffer.remaining()
			);
		}

		vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.graphicsPipeline);
		if (pipeline.hasDynamicViewport) {
			CommandRecorder.alreadyRecording(
					commandBuffer, instance.boiler, stack
			).dynamicViewportAndScissor(framebufferWidth, framebufferHeight);
		}
		vkCmdBindDescriptorSets(
				commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, instance.pipelineLayout,
				0, stack.longs(descriptorSet), null
		);
		pushConstants(commandBuffer, stack, framebufferWidth, framebufferHeight);

		startTime = System.nanoTime();
		int quadIndex = 0;
		for (var quad : placedQuads) {
			putQuad(memAddress(quadBuffer), quadIndex, quad);
			quadIndex += 1;
		}
		System.out.println("putting quads took " + (System.nanoTime() - startTime) / 1000_000);

		vkCmdDraw(commandBuffer, 6 * placedQuads.size(), 1, 0, 0);
	}

	public void pushConstants(
			VkCommandBuffer commandBuffer, MemoryStack stack,
			int framebufferWidth, int framebufferHeight
	) {
		vkCmdPushConstants(
				commandBuffer, instance.pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
				0, stack.ints(framebufferWidth, framebufferHeight)
		);
	}

	public void putQuad(long bufferAddress, int index, GlyphQuad quad) {
		long address = bufferAddress + (long) QUAD_BYTES * index;
		memPutInt(address, quad.minX);
		memPutInt(address + 4, quad.minY);
		memPutInt(address + 8, quad.getActualWidth());
		memPutInt(address + 12, quad.getHeight());
		memPutInt(address + 16, quad.bufferIndex);
		memPutInt(address + 20, quad.bufferOffsetX);
		memPutInt(address + 24, quad.sectionWidth);
		memPutInt(address + 28, quad.scale);

		int color;
		if (quad.userData instanceof Color) color = ((Color) quad.userData).getRGB();
		else color = Color.BLACK.getRGB();

		memPutInt(address + 32, color);
	}
}
