/*
 * Description: Detect the type of operating system that is available
 * License: MIT
 * Copyright: (c) 2020 Yong Mook Kim
 * Origin: https://github.com/mkyong/core-java/blob/master/java-basic/src/main/java/com/mkyong/system/OSValidator.java 
 */
package utils;

public class OSValidator {

    private final static String OS = System.getProperty("os.name").toLowerCase();
    
    public final static boolean 
            IS_WINDOWS = (OS.contains("win")),
            IS_MAC = (OS.contains("mac")),
            IS_UNIX = (OS.contains("nix") || OS.contains("nux") || OS.indexOf("aix") > 0),
            IS_SOLARIS = (OS.contains("sunos"));
}