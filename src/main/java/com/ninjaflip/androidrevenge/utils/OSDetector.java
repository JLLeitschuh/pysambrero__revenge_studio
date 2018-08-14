package com.ninjaflip.androidrevenge.utils;

/**
 * Created by Solitario on 04/12/2017.
 */
public class OSDetector {
    private static String OS = System.getProperty("os.name").toLowerCase();
    private static String Bit = System.getProperty("sun.arch.data.model").toLowerCase();

    public static boolean isWindows() {
        return (OS.contains("win"));
    }

    public static boolean isMacOSX() {
        return (OS.contains("mac"));
    }

    public static boolean isUnix() {
        return (OS.contains("nix") || OS.contains("nux") || OS.contains("aix") || (OS.contains("sunos")));
    }

    public static boolean is64Bit() {
        return Bit.equalsIgnoreCase("64");
    }

    public static String returnOS() {
        return OS;
    }
}
