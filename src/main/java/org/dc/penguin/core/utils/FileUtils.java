package org.dc.penguin.core.utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileUtils {
	public static void main(String[] args) throws Exception {
		String content = "Hello World !!\r\n";  
		Files.write(Paths.get("C:\\Users\\Administrator\\Desktop\\output.txt"), content.getBytes(),StandardOpenOption.APPEND);
	}
}
