package com.ninjaflip.androidrevenge.exceptions;

/**
 * Created by Solitario on 27/06/2017.
 *
 * This exception is by the adb manager when an ADB eencouter a problem
 */
public class AdbExecutionException extends Exception {

    private static final long serialVersionUID = 1927759363239857009L;

    public AdbExecutionException() {
    }

    public AdbExecutionException(String message) {
        super(message);
    }

    public AdbExecutionException(Throwable cause) {
        super(cause);
    }

    public AdbExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public AdbExecutionException(String message, Throwable cause,
                                            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}