package com.ninjaflip.androidrevenge.core.apktool.apkinfo.res;


import com.ninjaflip.androidrevenge.core.apktool.apkinfo.util.AttributeSet;
import com.ninjaflip.androidrevenge.core.apktool.apkinfo.xmlpull.XmlPullParser;

public interface XmlResourceParser extends XmlPullParser, AttributeSet {
  void close();
}