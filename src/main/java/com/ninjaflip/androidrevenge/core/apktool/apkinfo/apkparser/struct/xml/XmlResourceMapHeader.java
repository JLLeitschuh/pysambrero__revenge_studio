package com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.struct.xml;

import com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.struct.ChunkHeader;

public class XmlResourceMapHeader extends ChunkHeader {
    public XmlResourceMapHeader(int chunkType, int headerSize, long chunkSize) {
        super(chunkType, headerSize, chunkSize);
    }
}
