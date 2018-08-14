package com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.parser;

import com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.struct.xml.*;


public class CompositeXmlStreamer implements XmlStreamer {

    public XmlStreamer[] xmlStreamers;

    public CompositeXmlStreamer(XmlStreamer... xmlStreamers) {
        this.xmlStreamers = xmlStreamers;
    }

    @Override
    public void onStartTag(XmlNodeStartTag xmlNodeStartTag) {
        for (XmlStreamer xmlStreamer : xmlStreamers) {
            xmlStreamer.onStartTag(xmlNodeStartTag);
        }
    }

    @Override
    public void onEndTag(XmlNodeEndTag xmlNodeEndTag) {
        for (XmlStreamer xmlStreamer : xmlStreamers) {
            xmlStreamer.onEndTag(xmlNodeEndTag);
        }
    }

    @Override
    public void onCData(XmlCData xmlCData) {
        for (XmlStreamer xmlStreamer : xmlStreamers) {
            xmlStreamer.onCData(xmlCData);
        }
    }

    @Override
    public void onNamespaceStart(XmlNamespaceStartTag tag) {
        for (XmlStreamer xmlStreamer : xmlStreamers) {
            xmlStreamer.onNamespaceStart(tag);
        }
    }

    @Override
    public void onNamespaceEnd(XmlNamespaceEndTag tag) {
        for (XmlStreamer xmlStreamer : xmlStreamers) {
            xmlStreamer.onNamespaceEnd(tag);
        }
    }
}
