package com.ninjaflip.androidrevenge.enums;

/**
 * Created by Solitario on 27/12/2017.
 *
 * An enumeration that represents all restricted operation (operation that are objects of daily quota)
 */
public enum RestrictedOpType {
    APP_COUNTRY(100), APP_DETAILS(200);
    private final int value;

    RestrictedOpType(final int newValue) {
        value = newValue;
    }

    public int getValue() {
        return value;
    }

    public static String stringValue(int value) {
        if (value == APP_COUNTRY.getValue())
            return APP_COUNTRY.name();
        else if(value == APP_DETAILS.getValue())
            return APP_DETAILS.name();
        else
            return null;
    }

    public static String description(int value) {
        if (value == APP_COUNTRY.getValue())
            return "APP_COUNTRY";
        else if (value == APP_DETAILS.getValue())
            return "APP_DETAILS";
        else
            return null;
    }
}