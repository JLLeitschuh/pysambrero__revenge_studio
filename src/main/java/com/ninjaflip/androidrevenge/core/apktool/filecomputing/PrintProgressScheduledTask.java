package com.ninjaflip.androidrevenge.core.apktool.filecomputing;

import org.apache.log4j.Logger;

import java.util.TimerTask;

/**
 * Created by Solitario on 09/10/2017.
 *
 * TimerTask that prints progress every period of time
 */
public class PrintProgressScheduledTask extends TimerTask {
    private int progress = 0;
    private boolean stop = false;
    private String msg;
    private Logger LOGGER;

    public PrintProgressScheduledTask(String msg, Logger LOGGER) {
        this.msg = msg;
        this.LOGGER = LOGGER;
    }

    public void run() {
        if (stop) {
            return;
        }
        LOGGER.debug(msg + " > progress: " + progress + "%");
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public void printEndMessage() {
        LOGGER.debug(msg + " > progress: 100%");
    }
}