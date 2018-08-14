package com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.struct.xml;

import com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.struct.ResourceValue;
import com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.struct.resource.ResourceTable;
import com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.utils.ResourceLoader;

import java.util.Locale;
import java.util.Map;

/**
 * xml node attribute
 */
public class Attribute {
    private String namespace;
    private String name;
    // The original raw string value of this 
    private String rawValue;
    // Processed typed value of this
    private ResourceValue typedValue;
    // the final value as string
    private String value;

    public String toStringValue(ResourceTable resourceTable, Locale locale) {
        if (rawValue != null) {
            return rawValue;
        } else if (typedValue != null) {
            return typedValue.toStringValue(resourceTable, locale);
        } else {
            // something happen;
            return "";
        }
    }

    /**
     * These are attribute resource constants for the platform; as found in android.R.attr
     */
    public static class AttrIds {

        private static final Map<Integer, String> ids = ResourceLoader.loadSystemAttrIds();

        public static String getString(long id) {
            String value = ids.get((int) id);
            if (value == null) {
                value = "AttrId:0x" + Long.toHexString(id);
            }
            return value;
        }

    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRawValue() {
        return rawValue;
    }

    public void setRawValue(String rawValue) {
        this.rawValue = rawValue;
    }

    public ResourceValue getTypedValue() {
        return typedValue;
    }

    public void setTypedValue(ResourceValue typedValue) {
        this.typedValue = typedValue;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Attribute{" +
                "name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                '}';
    }
}
