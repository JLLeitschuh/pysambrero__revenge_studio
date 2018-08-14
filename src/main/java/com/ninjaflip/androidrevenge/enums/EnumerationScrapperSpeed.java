package com.ninjaflip.androidrevenge.enums;


public class EnumerationScrapperSpeed {

    public enum ScrapperSpeed {
        SLOW(500), MEDIUM(350), FAST(200), VERY_FAST(100), ULTRA_FAST(50);
        private final int value;

        ScrapperSpeed(final int newValue) {
            value = newValue;
        }

        public int getValue() {
            return value;
        }

        public static String stringValue(int value) {
            if (value == SLOW.getValue())
                return SLOW.name();
            else if (value == MEDIUM.getValue())
                return MEDIUM.name();
            else if (value == FAST.getValue())
                return FAST.name();
            else if (value == VERY_FAST.getValue())
                return VERY_FAST.name();
            else if (value == ULTRA_FAST.getValue())
                return ULTRA_FAST.name();
            else
                return null;
        }

        public static ScrapperSpeed speedValueFromStringRepresentation(String stringRepresentation) {
            if (stringRepresentation.equals("slow"))
                return SLOW;
            else if (stringRepresentation.equals("medium"))
                return MEDIUM;
            else if (stringRepresentation.equals("fast"))
                return FAST;
            else if (stringRepresentation.equals("very_fast"))
                return VERY_FAST;
            else if (stringRepresentation.equals("ultra_fast"))
                return ULTRA_FAST;
            else
                return null;
        }
    }
}
