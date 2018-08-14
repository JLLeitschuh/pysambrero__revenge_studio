package com.ninjaflip.androidrevenge.beans.containers;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Solitario on 01/11/2017.
 */
public class FixedSizeLinkedHashMap<K,V> extends LinkedHashMap<K,V> {
    private int initialCapacity;
    public FixedSizeLinkedHashMap(int initialCapacity) {
        super(initialCapacity);
        this.initialCapacity = initialCapacity;
    }

    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > this.initialCapacity;
    }
}
