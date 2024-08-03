package com.github.knokko.text;

import static org.lwjgl.util.freetype.FreeType.FT_Err_Ok;
import static org.lwjgl.util.freetype.FreeType.FT_Error_String;

public class FreeTypeFailureException extends RuntimeException {

	public static void assertFtSuccess(int result, String functionName, String context, int... allowedResults) {
		if (result == FT_Err_Ok) return;
		for (int allowed : allowedResults) {
			if (result == allowed) return;
		}

		if (!functionName.startsWith("FT_")) functionName = "FT_" + functionName;
		throw new FreeTypeFailureException(functionName, result, context);
	}

	private static String generateMessage(String functionName, int result, String context) {
		String functionContext = functionName;
		if (context != null) functionContext += " (" + context + ")";
		return functionContext + " returned " + result + " (" + FT_Error_String(result) + ")";
	}

	public FreeTypeFailureException(String functionName, int result, String context) {
		super(generateMessage(functionName, result, context));
	}
}
