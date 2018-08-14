package com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.bean;

/**
 * The apk icon file path, and data
 */
public class Icon {

    private final String path;
    private final byte[] data;

    public Icon(String path, byte[] data) {
        this.path = path;
        this.data = data;
    }

    public String getPath() {
        return path;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Icon{path='" + path + '\'' + ", size=" + (data == null ? 0 : data.length) + '}';
    }
}
