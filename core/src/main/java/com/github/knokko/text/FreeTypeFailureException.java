package com.github.knokko.text;

import org.lwjgl.util.freetype.FreeType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.lwjgl.util.freetype.FreeType.*;

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

		String errorName = "unknown";
		try {
			Field[] fields = FreeType.class.getDeclaredFields();
			for (Field field : fields) {
				if (field.getType() == int.class && field.getName().startsWith("FT_Err_") && Modifier.isStatic(field.getModifiers())) {
					if (field.getInt(null) == result) {
						errorName = field.getName();
						break;
					}
				}
			}
		} catch (Exception cannotFindErrorName) {
			errorName = "ERROR";
		}

		return functionContext + " returned " + result + " (" + errorName + ")";
	}

	public FreeTypeFailureException(String functionName, int result, String context) {
		super(generateMessage(functionName, result, context));
	}
}
