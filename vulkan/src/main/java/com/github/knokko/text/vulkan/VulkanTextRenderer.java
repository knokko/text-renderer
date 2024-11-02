package com.github.knokko.text.vulkan;

import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.utilities.ColorPacker;
import com.github.knokko.text.BoundingRectangle;
import com.github.knokko.text.bitmap.BitmapGlyphsBuffer;
import com.github.knokko.text.bitmap.FreeTypeGlyphRasterizer;
import com.github.knokko.text.bitmap.GlyphQuad;
import com.github.knokko.text.bitmap.GlyphRasterizer;
import com.github.knokko.text.font.FontData;
import com.github.knokko.text.placement.TextPlaceRequest;
import com.github.knokko.text.placement.TextPlacer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

/**
 * This class is responsible for actually rendering the text. It is not thread-safe at all, and you need 1 instance of
 * this class per frame in-flight.
 */
public class VulkanTextRenderer {

	private static final int QUAD_INTS = 8;

	private final VulkanTextInstance instance;
	private final VulkanTextPipeline pipeline;
	private final long descriptorSet;
	private final BitmapGlyphsBuffer glyphsBuffer;
	private final IntBuffer quadBuffer;

	private final TextPlacer placer;
	private final GlyphRasterizer rasterizer;
	private final int numTextPlacerThreads;

	VulkanTextRenderer(
			FontData font, VulkanTextInstance instance, VulkanTextPipeline pipeline,
			long descriptorSet, BitmapGlyphsBuffer glyphsBuffer, IntBuffer quadBuffer,
			int numTextPlacerThreads
	) {
		this.instance = instance;
		this.pipeline = pipeline;
		this.descriptorSet = descriptorSet;
		this.glyphsBuffer = glyphsBuffer;
		this.quadBuffer = quadBuffer;

		this.placer = new TextPlacer(font);
		this.rasterizer = new FreeTypeGlyphRasterizer(font);
		this.numTextPlacerThreads = numTextPlacerThreads;
	}

	/**
	 * Records commands to:
	 * <ol>
	 *     <li>Bind the text graphics pipeline</li>
	 *     <li>Sets dynamic viewport & scissor (if applicable)</li>
	 *     <li>Binds the descriptor set of this renderer</li>
	 *     <li>Sets push constant state</li>
	 *     <li>Draw the text quads</li>
	 * </ol>
	 * Furthermore, it will
	 * <ol>
	 *     <li>Use {@link TextPlacer} to place all requests</li>
	 *     <li>Use {@link BitmapGlyphsBuffer} to fill the mapped glyphs buffer</li>
	 *     <li>Put all the {@link GlyphQuad}s into the glyph buffer</li>
	 * </ol>
	 * Note that it's the responsibility of the caller to ensure that a compatible renderpass is currently active.
	 * @param recorder The command recorder to which this renderer should record the render commands
	 * @param framebufferWidth The width of the framebuffer/target image, in pixels
	 * @param framebufferHeight The height of the framebuffer/target image, in pixels
	 * @param requests The requests to be rendered
	 */
	public void recordCommands(
			CommandRecorder recorder, int framebufferWidth, int framebufferHeight, List<TextPlaceRequest> requests
	) {
		var bounds = new BoundingRectangle(0, 0, framebufferWidth, framebufferHeight);
		var placedGlyphs = placer.place(requests, numTextPlacerThreads, bounds);

		glyphsBuffer.startFrame();

		quadBuffer.position(0);

		glyphsBuffer.bufferGlyphs(rasterizer, placedGlyphs, bounds, quad -> {
			quadBuffer.put(quad.minX).put(quad.minY);
			quadBuffer.put(quad.getWidth()).put(quad.getHeight());
			quadBuffer.put(quad.bufferIndex).put(quad.sectionWidth).put(quad.scale);

			int color = ColorPacker.rgba(0, 0, 0, 255);
			if (quad.request.userData instanceof Integer) color = (Integer) quad.request.userData;

			quadBuffer.put(color);
		});

		vkCmdBindPipeline(recorder.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.graphicsPipeline);
		if (pipeline.hasDynamicViewport) {
			recorder.dynamicViewportAndScissor(framebufferWidth, framebufferHeight);
		}
		recorder.bindGraphicsDescriptors(instance.pipelineLayout, descriptorSet);
		pushConstants(recorder.commandBuffer, recorder.stack, framebufferWidth, framebufferHeight);

		vkCmdDraw(recorder.commandBuffer, 6 * (quadBuffer.position() / QUAD_INTS), 1, 0, 0);
	}

	private void pushConstants(
			VkCommandBuffer commandBuffer, MemoryStack stack,
			int framebufferWidth, int framebufferHeight
	) {
		vkCmdPushConstants(
				commandBuffer, instance.pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
				0, stack.ints(framebufferWidth, framebufferHeight)
		);
	}

	/**
	 * Destroys this renderer. You should call this when you no longer need this renderer, and all submission for which
	 * it has recorder commands, have completed execution.
	 */
	public void destroy() {
		placer.destroy();
		rasterizer.destroy();
	}
}
