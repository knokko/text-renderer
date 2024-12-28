# Text renderer for LWJGL games
This library aims to make text rendering less difficult for
LWJGL games, especially those using Vulkan. This text rendering
library provides a 3-stage text rendering 'pipeline', where only
the last stage requires Vulkan. Thus, the first 2 stages can just
as well be used in OpenGL games.

## The pipeline
Before the real pipeline starts, you need to create a
`TextInstance` and at least 1 `FontData`. The simplest way would
be:
```java
var instance = new TextInstance();
var unicodeFontData = new FontData(instance, UnicodeFonts.SOURCE);
```
Alternatively, you can choose your own font:
```java
var fontData = new FontData(instance, new ClasspathFontsSource("fonts/unicode-polyglott.ttf"));
```
or from a file:
```java
var fontData = new FontData(instance, new FilesFontSource(new File("path.ttf")));
```

### Stage 1: text placing
This is the most complicated part of the pipeline (at least, it
was the most complicated to implement). Given a list of
`TextPlaceRequest`s, it returns a list of `PlacedGlyph`s.
A `TextPlaceRequest` consists of a string to render,
font size, position, and bounding rectangle. A `PlacedGlyph`
consists of a position, size, and glyph.

Essentially, the text placement stage tells you *where* you should
render each *glyph* in the input string. It is the
[text shaping](https://harfbuzz.github.io/what-is-harfbuzz.html#what-is-text-shaping)
of HarfBuzz, plus quite some additional logic to handle
positions, bidirectional text, bounding rectangles, and
fallback fonts. To use it, you need to create a `TextPlacer` and call its
`place` method, for instance:
```java
var textPlacer = new TextPlacer(fontData);
List<TextPlaceRequest> requests = new ArrayList<>();
// parameters: text, minX, minY, maxX, maxY, baseY, heightA, minScale, alignment, userData
requests.add(new TextPlaceRequest("hello world", 10, 10, 200, 40, 34, 18, 1, TextAlignment.DEFAULT, null));

var placedGlyphs = textPlacer.place(requests);
```
Consult the documentation of
[TextPlaceRequest](./core/src/main/java/com/github/knokko/text/placement/TextPlaceRequest.java)
for more information about the parameters.

### Stage 2: glyph rasterization & packing
The output of stage 1 is a list of `PlacedGlyph`s, but it does
**not** tell you what these glyphs look like. During this stage,
all relevant glyphs will be rasterized, and stored in 1 (big)
`ByteBuffer`.

To use it, you need to allocate a sufficiently large *direct*
`ByteBuffer` that will be used to store the rasterized
glyphs. The required size depends on the amount of text and
distinct characters in the text you need to render, but 10MB
is usually more than enough.
```java
var byteBuffer = memAlloc(10_000_000L);
```
When you use Vulkan, you should allocate a `ByteBuffer` at
mapped memory using `memByteBuffer(mappedAddress, size)`.

To actually rasterize the glyphs, you need an implementation of
`GlyphRasterizer`. You will probably want to use
`FreeTypeGlyphRasterizer`:
```java
var rasterizer = new FreeTypeGlyphRasterizer(fontData);
```
Finally, you need to create a `BitmapGlyphsBuffer`, and call
its `bufferGlyphs` method.
```java
var glyphsBuffer = new BitmapGlyphsBuffer(
		memAddress(byteBuffer), byteBuffer.capacity()
);
var glyphQuads = glyphsBuffer.bufferGlyphs(
		rasterizer, placedGlyphs
);
```
where `placedGlyphs` is the output from stage 1.

The result is a list of `GlyphQuad`s, where each `GlyphQuad`
describes 1 rectangle where the pixels should be 'sampled' from
the given section of the byte/glyph buffer

### Stage 3: rendering
Stage 1 will tell you *where* to render each glyph, and stage 2
will basically give you a list of quads to render. In stage 3,
you need to actually render these quads. I deliberately made
this stage as simple as possible, since real rendering code can
be difficult to reuse. This library provides 2 implementations
of stage 3.

#### CPU implementation
The [BufferedImageTextRenderer](./core/src/main/java/com/github/knokko/text/renderer/cpu/BufferedImageTextRenderer.java)
class which extends
[CpuTextRenderer](./core/src/main/java/com/github/knokko/text/renderer/cpu/BufferedImageTextRenderer.java)
. This implementation is not very useful (since the Java
standard library is perfectly capable of rendering text on
`BufferedImage`s), but the `CpuTextRenderer` class is
**an excellent example implementation** that shows you exactly
how to use the data from stage 1 and stage 2. The links above
should lead you to their source code. If you want to use the
CPU renderer for some reason, this code shows how:
```java
var renderer = new BufferedImageTextRenderer(
		new BufferedImage(50, 10, BufferedImage.TYPE_INT_RGB),
		fontData, 1_000
);

List<TextPlaceRequest> requests = new ArrayList<>();
requests.add(new TextPlaceRequest("hello", 0, 0, 10, 9, 7, 0, 1, TextAlignment.DEFAULT, null));
renderer.render(requests);
// The result is now visible in renderer.image, which you can
// see by e.g. using ImageIO.write
```

#### Vulkan implementation
My Vulkan implementation requires Vulkan 1.0 and
[vk-boiler 4.3.1+](https://github.com/knokko/vk-boiler). Note that
you are free to create your own Vulkan implementation if you
don't want to use my vk-boiler library, or just need to
support more features. When you do so, I recommend looking at the
[source code](./vulkan/src/main/java/com/github/knokko/text/vulkan)
and
[shaders](./vulkan/src/main/resources/com/github/knokko/text/vulkan)
. You can use the Vulkan implementation like this:
```java
// Setup code, only run this once
var textInstance = new TextInstance();
var unicodeFont = new FontData(textInstance, UnicodeFonts.SOURCE);
var vkTextInstance = new VulkanTextInstance(boiler);
var vkTextPipeline = vkTextInstance.createPipelineWithDynamicRendering(
		0, window.surfaceFormat, null, null
);
var textDescriptorPool = vkTextInstance.descriptorSetLayout.createPool(
		1, 0, "TextPool"
);
var glyphBuffer = boiler.buffers.createMapped(
		30_000_000, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "GlyphBuffer"
);
var quadBuffer = boiler.buffers.createMapped(
		10_000_000, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, "QuadBuffer"
);
long descriptorSet = textDescriptorPool.allocate(1)[0];
var vkTextRenderer = vkTextPipeline.createRenderer(
		unicodeFont, descriptorSet, glyphBuffer.fullMappedRange(),
		quadBuffer.fullMappedRange(), 360, 3
);

// To record render commands, you need to run the following code during a renderpass
vkTextRenderer.recordCommands(recorder, acquiredImage.width(), acquiredImage.height(), requests);
```
The above code was basically ripped from the
[unicode sample](./sample-unicode-renderer/src/main/java/com/github/knokko/text/sample/UnicodeRendererSample.java)
. You should look at this file for a complete example.

## Adding this library as dependency
This library consists of multiple modules, and you need to decide
which of those you want:
- `core`
- `core-bundle`
- `sample-unicode-renderer`
- `test-helper`
- `unicode-fonts`
- `vulkan`
- `vulkan-bundle`

You should probably **not** add `sample-unicode-renderer` and
`test-helper` as dependency.

### Core (bundle)
The `core` module contains stage 1 and stage 2 of the pipeline,
plus the CPU renderer of stage 3. The `core` module requires
`freetype` and `harfbuzz`.
- If you add `core-bundle` as dependency, you will get the
`core` module, bundled with `lwjgl-freetype` and `lwjgl-harfbuzz`
- If you add the 'raw' `core` as dependency, you will only get
the `core` module. You will have to ensure that `lwjgl-freetype`
and `lwjgl-harfbuzz` are also present at runtime.

### Unicode fonts
The `unicode-fonts` module contains a bunch of fonts that
together cover most Unicode characters. They are meant to be
used as fallback fonts. You can use them like this:
```java
var font = new FontData(
		instance, fontThatYouWantToUse, UnicodeFonts.SOURCE
);
```
The created `font` will use `fontThatYouWantToUse` for each
character supported by that font, and use the `UnicodeFonts`
as fallback for all other characters. Adding `unicode-fonts`
as dependency should take about 10MB of storage, and about
20MB of memory.

### Vulkan (bundle)
The `vulkan` module contains my Vulkan implementation of stage
3 of the pipeline. You should only use this module if you want
to use my Vulkan implementation. The `vulkan` module requires
`core`, `lwjgl-vulkan`, `lwjgl-vma`, and `vk-boiler`.
- If you add `vulkan-bundle` as dependency, you will get the
`core` module, `vulkan` module, bundled with `lwjgl-vulkan`,
`lwjgl-vma`, `lwjgl-freetype`, and `lwjgl-harfbuzz`.
- If you add `vulkan` as dependency, you will just get the
`vulkan` module, and need to ensure that all its dependencies
will be available at runtime.

### Gradle
```
...
repositories {
	...
	maven { url 'https://jitpack.io' }
}
...
dependencies {
	...
	// For each module
	implementation 'com.github.knokko.text-renderer:module:v0.1.0'
}
```

### Maven
```
...
<repositories>
	...
	<repository>
		<id>jitpack.io</id>
		<url>https://jitpack.io</url>
	</repository>
</repositories>
...
<dependency>
	<-- for each module -->
	<groupId>com.github.knokko.text-renderer</groupId>
	<artifactId>module</artifactId>
	<version>v0.1.0</version>
</dependency>
```