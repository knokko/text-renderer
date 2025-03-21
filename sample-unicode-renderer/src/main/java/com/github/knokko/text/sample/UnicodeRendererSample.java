package com.github.knokko.text.sample;

import com.github.knokko.boiler.BoilerInstance;
import com.github.knokko.boiler.buffers.MappedVkbBuffer;
import com.github.knokko.boiler.builders.BoilerBuilder;
import com.github.knokko.boiler.builders.WindowBuilder;
import com.github.knokko.boiler.commands.CommandRecorder;
import com.github.knokko.boiler.descriptors.HomogeneousDescriptorPool;
import com.github.knokko.boiler.synchronization.ResourceUsage;
import com.github.knokko.boiler.window.*;
import com.github.knokko.memory.MemorySnapshot;
import com.github.knokko.profiler.SampleProfiler;
import com.github.knokko.profiler.storage.FrequencyThreadStorage;
import com.github.knokko.profiler.storage.SampleStorage;
import com.github.knokko.text.TextInstance;
import com.github.knokko.text.font.FontData;
import com.github.knokko.text.font.UnicodeFonts;
import com.github.knokko.text.placement.TextAlignment;
import com.github.knokko.text.placement.TextPlaceRequest;
import com.github.knokko.text.util.UnicodeLines;
import com.github.knokko.text.vulkan.VulkanTextInstance;
import com.github.knokko.text.vulkan.VulkanTextPipeline;
import com.github.knokko.text.vulkan.VulkanTextRenderer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;

import java.util.ArrayList;
import java.util.List;

import static com.github.knokko.boiler.utilities.ColorPacker.rgba;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_API_VERSION_1_2;

public class UnicodeRendererSample extends SimpleWindowRenderLoop {

	@SuppressWarnings({"resource", "NonAtomicOperationOnVolatileField"})
	public static void main(String[] args) {
		System.out.println("initial memory usage is " + MemorySnapshot.take());
		var boiler = new BoilerBuilder(
				VK_API_VERSION_1_2, "UnicodeRendererSample", 1
		).enableDynamicRendering().addWindow(new WindowBuilder(
				1200, 700, VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
		)).build();
		System.out.println("initialized memory usage is " + MemorySnapshot.take());

		var glfwWindow = boiler.window().glfwWindow;
		var renderer = new UnicodeRendererSample(boiler.window());
		glfwSetKeyCallback(glfwWindow, (window, key, scancode, action, mods) -> {
			double arrowSpeed = 20 / renderer.scaleY;
			if (key == GLFW_KEY_DOWN) renderer.cameraY += arrowSpeed;
			if (key == GLFW_KEY_UP) renderer.cameraY -= arrowSpeed;
			if (key == GLFW_KEY_LEFT) renderer.cameraX -= arrowSpeed;
			if (key == GLFW_KEY_RIGHT) renderer.cameraX += arrowSpeed;
			if ((mods & GLFW_MOD_CONTROL) != 0 && key == GLFW_KEY_EQUAL) renderer.scaleY *= 1.05;
			if ((mods & GLFW_MOD_CONTROL) != 0 && key == GLFW_KEY_MINUS) renderer.scaleY /= 1.05;
		});

		glfwSetScrollCallback(glfwWindow, ((window, offsetX, offsetY) -> {
			double scrollSpeed = 40 / renderer.scaleY;

			if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) != glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT)) {
				renderer.cameraX -= scrollSpeed * offsetY;
			} else if (glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) != glfwGetKey(window, GLFW_KEY_RIGHT_CONTROL)) {
				try (var stack = stackPush()) {
					var cursorX = stack.callocDouble(1);
					var cursorY = stack.callocDouble(1);
					glfwGetCursorPos(window, cursorX, cursorY);

					double virtualX = renderer.screenXtoVirtualX(cursorX.get(0));
					double virtualY = renderer.screenYtoVirtualY(cursorY.get(0));
					renderer.scaleY *= Math.pow(1.05, offsetY);
					double newVirtualX = renderer.screenXtoVirtualX(cursorX.get(0));
					double newVirtualY = renderer.screenYtoVirtualY(cursorY.get(0));
					renderer.cameraX += virtualX - newVirtualX;
					renderer.cameraY += virtualY - newVirtualY;
				}

			} else {
				renderer.cameraY -= scrollSpeed * offsetY;
			}
		}));

		var eventLoop = new WindowEventLoop();
		eventLoop.addWindow(renderer);

		System.out.println("before starting event loop: memory usage is " + MemorySnapshot.take());
		eventLoop.runMain();

		boiler.destroyInitialObjects();
	}

	private TextInstance textInstance;
	private FontData unicodeFont;

	private VulkanTextInstance vkTextInstance;
	private VulkanTextPipeline vkTextPipeline;
	private final VulkanTextRenderer[] vkTextRenderers = new VulkanTextRenderer[numFramesInFlight];
	private final MappedVkbBuffer[] glyphBuffers = new MappedVkbBuffer[numFramesInFlight],
			quadBuffers = new MappedVkbBuffer[numFramesInFlight];
	private HomogeneousDescriptorPool textDescriptorPool;

	private List<String> unicodeTestCase;
	volatile double cameraX = 0.0;
	volatile double cameraY = 0.0;
	volatile double scaleY = 50.0;

	SampleProfiler profiler;
	SampleStorage<FrequencyThreadStorage> profilerStorage;

	public UnicodeRendererSample(VkbWindow window) {
		super(
				window, 2, false, VK_PRESENT_MODE_MAILBOX_KHR,
				ResourceUsage.COLOR_ATTACHMENT_WRITE, ResourceUsage.COLOR_ATTACHMENT_WRITE
		);
	}

	@Override
	protected void setup(BoilerInstance boiler, MemoryStack stack) {
		System.out.println("Memory usage before setup is " + MemorySnapshot.take());
		profilerStorage = SampleStorage.frequency();
		profiler = new SampleProfiler(profilerStorage);
		profiler.start();

		super.setup(boiler, stack);

		textInstance = new TextInstance();
		unicodeFont = new FontData(textInstance, UnicodeFonts.SOURCE);
		vkTextInstance = new VulkanTextInstance(boiler);
		vkTextPipeline = vkTextInstance.createPipelineWithDynamicRendering(
				0, window.surfaceFormat, null, null
		);
		System.out.println("Memory usage mid setup is " + MemorySnapshot.take());
		textDescriptorPool = vkTextInstance.descriptorSetLayout.createPool(numFramesInFlight, 0, "TextPool");

		long[] descriptorSets = textDescriptorPool.allocate(numFramesInFlight);

		for (int index = 0; index < numFramesInFlight; index++) {
			glyphBuffers[index] = boiler.buffers.createMapped(30_000_000, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "GlyphBuffer");
			quadBuffers[index] = boiler.buffers.createMapped(10_000_000, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "QuadBuffer");
			vkTextRenderers[index] = vkTextPipeline.createRenderer(
					unicodeFont, descriptorSets[index], glyphBuffers[index].fullMappedRange(),
					quadBuffers[index].fullMappedRange(), 360, 3
			);
		}
		unicodeTestCase = UnicodeLines.get();
		System.out.println("Memory usage after setup is " + MemorySnapshot.take());
	}

	int virtualXtoScreenX(double cameraX, double scaleY, double virtualX) {
		return (int) ((virtualX - cameraX) * scaleY);
	}

	int virtualYtoScreenY(double cameraY, double scaleY, double virtualY) {
		return (int) ((virtualY - cameraY) * scaleY);
	}

	double screenXtoVirtualX(double screenX) {
		return cameraX + screenX / scaleY;
	}

	double screenYtoVirtualY(double screenY) {
		//int screenY = (int) ((virtualY - cameraY) * scaleY);
		// virtualY - cameraY = screenY / scaleY
		// virtualY = cameraY + screenY / scaleY
		return cameraY + screenY / scaleY;
	}

	private long lastFrame;
	private long lastCounter;

	@Override
	protected void recordFrame(
			MemoryStack stack, int frameIndex, CommandRecorder recorder,
			AcquiredImage acquiredImage, BoilerInstance boiler
	) {
		long currentTime = System.nanoTime();
		if (currentTime - lastFrame > 1_000_000_000) {
			if (lastCounter > 0) {
				System.out.println("FPS is approximately " + lastCounter + " and memory usage is " + MemorySnapshot.take());
			}
			lastFrame = currentTime;
			lastCounter = 0;
		}

		lastCounter += 1;

		double cameraX = this.cameraX;
		double cameraY = this.cameraY;
		double scaleY = this.scaleY;

		var requests = new ArrayList<TextPlaceRequest>();
		double offsetY = 0.01;
		int textHeight = (int) scaleY;
		for (String line : unicodeTestCase) {
			int minY = virtualYtoScreenY(cameraY, scaleY, offsetY);
			int minX = virtualXtoScreenX(cameraX, scaleY, 0.0);
			int maxY = minY + textHeight - 1;
			int maxX = virtualXtoScreenX(cameraX, scaleY, 70);
			requests.add(new TextPlaceRequest(
					line, minX, minY, maxX, maxY, (minY + maxY) / 2,
					textHeight / 3, 1, TextAlignment.DEFAULT, rgba(80, 180, 240, 200)
			));
			offsetY += 1.0;
		}

		var colorAttachments = VkRenderingAttachmentInfo.calloc(1, stack);
		recorder.simpleColorRenderingAttachment(
				colorAttachments.get(0), acquiredImage.image().vkImageView(), VK_ATTACHMENT_LOAD_OP_CLEAR,
				VK_ATTACHMENT_STORE_OP_STORE, 0.2f, 0.2f, 0.2f, 1f
		);
		recorder.beginSimpleDynamicRendering(acquiredImage.width(), acquiredImage.height(), colorAttachments, null, null);
		vkTextRenderers[frameIndex].recordCommands(recorder, acquiredImage.width(), acquiredImage.height(), requests);
		recorder.endDynamicRendering();
	}

	@Override
	protected void cleanUp(BoilerInstance boiler) {
		super.cleanUp(boiler);
		for (var glyphBuffer : glyphBuffers) glyphBuffer.destroy(boiler);
		for (var quadBuffer : quadBuffers) quadBuffer.destroy(boiler);
		textDescriptorPool.destroy();
		for (var renderer : vkTextRenderers) renderer.destroy();
		vkTextPipeline.destroy();
		vkTextInstance.destroyInitialObjects();
		unicodeFont.destroy();
		textInstance.destroy();
		profiler.stop();
		profilerStorage.getThreadStorage(Thread.currentThread().getId()).print(System.out, 35, 5);
	}
}
