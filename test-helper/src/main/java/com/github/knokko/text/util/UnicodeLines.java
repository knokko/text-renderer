package com.github.knokko.text.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class UnicodeLines {

	public static List<String> get() {
		var scanner = new Scanner(Objects.requireNonNull(
				UnicodeLines.class.getClassLoader().getResourceAsStream("unicode-3.2-test-page.html")
		), StandardCharsets.UTF_8);

		List<String> lines = new ArrayList<>();
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (lines.isEmpty() || isNotPartOfTestCase(line) || isNotPartOfTestCase(lines.get(lines.size() - 1))) lines.add(line);
			else lines.set(lines.size() - 1, lines.get(lines.size() - 1) + " " + line);
		}

		scanner.close();

		return lines;
	}

	private static boolean isNotPartOfTestCase(String line) {
		return line.length() < 20 || line.chars().filter(candidate -> candidate == ' ').count() < line.chars().count() / 3;
	}
}
