package com.github.knokko.text;

import org.lwjgl.util.freetype.FreeType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.lwjgl.util.freetype.FreeType.*;

/**
 * This exception will be thrown when a FreeType function does not return {@link FreeType#FT_Err_Ok}.
 */
public class FreeTypeFailureException extends RuntimeException {

	/**
	 * Asserts that {@code result} is {@link FreeType#FT_Err_Ok}, or an element of {@code allowedResults}. If not,
	 * a {@link FreeTypeFailureException} will be thrown.
	 * @param result The result of a FreeType function call
	 * @param functionName The name of the FreeType function that returned {@code result}. The {@code "FT_} prefix
	 *                     may be omitted.
	 * @param context When this method throws a {@link FreeTypeFailureException}, this context will be included in the
	 *                message of the exception
	 * @param allowedResults A (possibly empty) list of alternative results that are allowed.
	 */
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

	/**
	 * @param functionName The name of the FreeType function that returned {@code result}. It should start with
	 *                     {@code "FT_"}
	 * @param result The result that was returned by the FreeType function, should not be {@link FreeType#FT_Err_Ok}
	 * @param context Optional additional information that will be included in the message
	 */
	public FreeTypeFailureException(String functionName, int result, String context) {
		super(generateMessage(functionName, result, context));
	}
}
