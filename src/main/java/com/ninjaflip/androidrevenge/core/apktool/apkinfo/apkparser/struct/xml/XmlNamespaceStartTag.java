package com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.struct.xml;


public class XmlNamespaceStartTag {
    private String prefix;
    private String uri;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String toString() {
        return prefix + "=" + uri;
    }
}
