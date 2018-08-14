package com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.struct.xml;

import com.ninjaflip.androidrevenge.core.apktool.apkinfo.apkparser.struct.ChunkHeader;

/**
 * Null header.
 */
public class NullHeader extends ChunkHeader {
    public NullHeader(int chunkType, int headerSize, long chunkSize) {
        super(chunkType, headerSize, chunkSize);
    }
}
