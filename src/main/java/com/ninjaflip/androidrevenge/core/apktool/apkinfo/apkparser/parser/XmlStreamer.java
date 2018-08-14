package com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.parser;


import com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.struct.xml.*;

/**
 * callback interface for parse binary xml file.
 *
 */
public interface XmlStreamer {

    void onStartTag(XmlNodeStartTag xmlNodeStartTag);

    void onEndTag(XmlNodeEndTag xmlNodeEndTag);

    void onCData(XmlCData xmlCData);

    void onNamespaceStart(XmlNamespaceStartTag tag);

    void onNamespaceEnd(XmlNamespaceEndTag tag);
}
