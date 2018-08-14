package com.ninjaflip.androidrevenge.core.security;

import com.ninjaflip.androidrevenge.core.PreferencesManager;
import com.ninjaflip.androidrevenge.utils.Utils;

import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Solitario on 29/11/2017.
 * <p>
 * This class makes a periodical check for licensee using an internal timer task that check if license is valid periodically.
 * In case someone has cracked the software and disabled the first license check at the login page,
 * this timer task will do the check after login and will exit the program if license is not valid
 */
public class LicensePeriodicChecker {
    private static LicensePeriodicChecker INSTANCE;
    private boolean stop = false;
    private TimerTask licenseCheckerTask = null;
    private Timer timer;


    public LicensePeriodicChecker() {

    }

    public static LicensePeriodicChecker getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LicensePeriodicChecker();
        }
        return INSTANCE;
    }


    public void startTimerTask(){
        if(licenseCheckerTask != null){
            endTimerTask();
        }
        stop = false;
        licenseCheckerTask = new TimerTask() {
            @Override
            public void run() {
                if (stop) {
                    return;
                }
                checkSoftwareLicenseIsOk();
            }
        };
        timer = new Timer();
        // start checker task after a random time between 30 seconds and 2 minutes
        int delay = Utils.generateRandomIntBetween(30000, 120000);
        int period = 300000; // every five minutes
        timer.schedule(licenseCheckerTask, delay, period);
    }

    private void endTimerTask(){
        stop = true;
        if(licenseCheckerTask != null){
            licenseCheckerTask.cancel();
        }

        if(timer != null){
            timer.cancel();
            timer.purge();
        }

        licenseCheckerTask = null;
        timer = null;
    }

    private void checkSoftwareLicenseIsOk() {
        String licenseKey = PreferencesManager.getInstance().getLicenseKey();
        if (licenseKey == null || !checkGeneratedSerialIsValid(licenseKey)) {
            exitSilently();
        }
    }


    private boolean checkGeneratedSerialIsValid(String serial) {
        try {
            String hexToStringRealSerial = Utils.hexToString(serial);
            String decryptedComputerUniqueId = TripleDesEncryption.decrypt(hexToStringRealSerial, LicenseManager.SKEY_FOR_GENERATED_SERIAL);
            return decryptedComputerUniqueId.equals(LicenseManager.getInstance().getComputerUniqueId());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Anti reverse engineering exit
     * <p>
     * This method invokes System.exit() using java reflexion
     */
    private void exitSilently() {
        try {
            //String parameter
            Class[] paramString = new Class[1];
            paramString[0] = String.class;
            //int parameter
            Class[] paramInt = new Class[1];
            paramInt[0] = Integer.TYPE;

            Class clsSystem = Class.forName(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));//java.lang.System
            Method exit = clsSystem.getMethod(new String(Base64.getDecoder().decode("ZXhpdA==")), paramInt);//exit
            exit.invoke(null, Integer.valueOf(new String(Base64.getDecoder().decode("LTQw"))));//System.exit(-40)
        } catch (Exception e) {
            //System.exit(-40);
        }
    }
}
