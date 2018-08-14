package com.ninjaflip.androidrevenge.enums;

/**
 * Created by Solitario on 14/11/2017.
 *
 * Scrapper enumerations
 */
public class EnumerationScrapper {

    /**
     * used by web socket to notify for process state.
     */
    public enum EnumProcessType {
        COUNTRY_PROGRESS(100), SINGLE_APP_PROGRESS(200);
        private final int value;

        EnumProcessType(final int newValue) {
            value = newValue;
        }

        public int getValue() {
            return value;
        }

        public static String stringValue(int value) {
            if (value == COUNTRY_PROGRESS.getValue())
                return COUNTRY_PROGRESS.name();
            else if(value == SINGLE_APP_PROGRESS.getValue())
                return SINGLE_APP_PROGRESS.name();
            else
                return null;
        }

        public static String description(int value) {
            if (value == COUNTRY_PROGRESS.getValue())
                return "COUNTRY SCRAPPER";
            else if (value == SINGLE_APP_PROGRESS.getValue())
                return "APP DETAILS SCRAPPER";
            else
                return null;
        }
    }

    /**
     * Process execution states
     */
    public enum EnumProcessState {
        STARTED(10), INTERRUPTED(20), ERROR(30), COMPLETED(40);
        private final int value;

        EnumProcessState(final int newValue) {
            value = newValue;
        }

        public int getValue() {
            return value;
        }

        public static String stringValue(int value) {
            if (value == STARTED.getValue())
                return STARTED.name();
            else if (value == INTERRUPTED.getValue())
                return INTERRUPTED.name();
            else if (value == ERROR.getValue())
                return ERROR.name();
            else if (value == COMPLETED.getValue())
                return COMPLETED.name();
            else
                return null;
        }
    }

    /**
     * used to specify which scrapper log event is sent to client.
     * Logs are displayed in different DOM elements depending on log event type
     */
    public enum EnumScrapperLogType {
        COUNTRY_PROGRESS(100), SINGLE_APP_PROGRESS(200);
        private final int value;

        EnumScrapperLogType(final int newValue) {
            value = newValue;
        }

        public int getValue() {
            return value;
        }

        public static String stringValue(int value) {
            if (value == COUNTRY_PROGRESS.getValue())
                return COUNTRY_PROGRESS.name();
            else if (value == SINGLE_APP_PROGRESS.getValue())
                return SINGLE_APP_PROGRESS.name();
            else
                return null;
        }
    }
}
