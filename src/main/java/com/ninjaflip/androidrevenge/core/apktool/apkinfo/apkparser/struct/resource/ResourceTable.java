package com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.struct.resource;


import com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.struct.StringPool;
import com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.utils.ResourceLoader;

import java.util.HashMap;
import java.util.Map;


public class ResourceTable {
    private Map<Short, ResourcePackage> packageMap = new HashMap<>();
    private StringPool stringPool;

    public static Map<Integer, String> sysStyle = ResourceLoader.loadSystemStyles();

    public void addPackage(ResourcePackage resourcePackage) {
        this.packageMap.put(resourcePackage.getId(), resourcePackage);
    }

    public ResourcePackage getPackage(short id) {
        return this.packageMap.get(id);
    }

    public StringPool getStringPool() {
        return stringPool;
    }

    public void setStringPool(StringPool stringPool) {
        this.stringPool = stringPool;
    }
}
