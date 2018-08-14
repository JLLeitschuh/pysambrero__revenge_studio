package com.ninjaflip.androidrevenge.exceptions;

import com.ninjaflip.androidrevenge.core.apktool.apkinfo.xmlpull.XmlPullParser;

public class XmlPullParserException
        extends Exception {
    protected Throwable detail;
    protected int row = -1;
    protected int column = -1;


    public XmlPullParserException(String s) {
        super(s);
    }


    public XmlPullParserException(String msg, XmlPullParser parser, Throwable chain) {
        super((msg == null ? "" : new StringBuilder(String.valueOf(msg)).append(" ").toString()) + (parser == null ? "" : new StringBuilder("(position:").append(parser.getPositionDescription()).append(") ").toString()) + (chain == null ? "" : new StringBuilder("caused by: ").append(chain).toString()));

        if (parser != null) {
            row = parser.getLineNumber();
            column = parser.getColumnNumber();
        }
        detail = chain;
    }

    public Throwable getDetail() {
        return detail;
    }

    public int getLineNumber() {
        return row;
    }

    public int getColumnNumber() {
        return column;
    }


    public void printStackTrace() {
        if (detail == null) {
            super.printStackTrace();
        } else {
            synchronized (System.err) {
                System.err.println(super.getMessage() + "; nested exception is:");
                detail.printStackTrace();
            }
        }
    }
}
