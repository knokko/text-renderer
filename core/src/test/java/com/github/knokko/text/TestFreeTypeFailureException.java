package com.github.knokko.text;

import org.junit.jupiter.api.Test;

import static com.github.knokko.text.FreeTypeFailureException.assertFtSuccess;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.lwjgl.util.freetype.FreeType.FT_Err_Invalid_Pixel_Size;
import static org.lwjgl.util.freetype.FreeType.FT_Err_Ok;

public class TestFreeTypeFailureException {

	@Test
	public void testAssertSuccess() {
		assertFtSuccess(FT_Err_Ok, "bla", null);
		assertFtSuccess(FT_Err_Ok, "bla", "bla");

		assertThrows(
				FreeTypeFailureException.class,
				() -> assertFtSuccess(FT_Err_Invalid_Pixel_Size, "FT_Set_Char_size", null),
				"FT_Set_Char_size returned 23 (FT_Err_Invalid_Pixel_Size)"
		);

		assertThrows(
				FreeTypeFailureException.class,
				() -> assertFtSuccess(FT_Err_Invalid_Pixel_Size, "FT_Set_Char_size", "test1234"),
				"FT_Set_Char_size (test1234) returned 23 (FT_Err_Invalid_Pixel_Size)"
		);
	}
}
