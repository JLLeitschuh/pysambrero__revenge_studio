package com.ninjaflip.androidrevenge.beans.containers;

import java.util.HashMap;

/**
 * Created by Solitario on 01/11/2017.
 */
public class MimeTypeMap extends HashMap<String, String> {

    public MimeTypeMap(int initialCapacity) {
        super(initialCapacity);
    }

    public void put1(String key, String value) {
        if (put(key, value) != null) {
            throw new IllegalArgumentException("Duplicated extension: " + key);
        }
    }
}
