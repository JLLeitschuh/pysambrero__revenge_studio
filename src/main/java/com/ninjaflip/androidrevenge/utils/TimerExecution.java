package com.ninjaflip.androidrevenge.utils;

import java.util.Timer;
import java.util.TimerTask;

public abstract class TimerExecution {
    private Timer timer;
    private boolean abortExit = false;


    public TimerExecution(int exitDelay, int beepDelay) {
        timer = new Timer();
        timer.schedule(new RemindTask(exitDelay), 0, beepDelay * 1000);
    }

    public void setAbortExit(boolean abortExit) {
        this.abortExit = abortExit;
    }

    // Must override this method
    public abstract void performAction();

    class RemindTask extends TimerTask {
        private int exitDelay;

        public RemindTask(int _exitDelay) {
            exitDelay = _exitDelay;
        }

        @SuppressWarnings("synthetic-access")
        @Override
        public void run() {
            if (exitDelay == 0) {
                performAction();
            }
            if (abortExit) {
                timer.cancel();
            }
            exitDelay--;
        }
    }
}
