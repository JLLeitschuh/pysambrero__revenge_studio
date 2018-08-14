package com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.struct.xml;

import com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.struct.ChunkHeader;

/**
 * Binary XML header. It is simply a struct ResChunk_header.
 * The header.type is always 0×0003 (XML).
 */
public class XmlHeader extends ChunkHeader {
    public XmlHeader(int chunkType, int headerSize, long chunkSize) {
        super(chunkType, headerSize, chunkSize);
    }
}
