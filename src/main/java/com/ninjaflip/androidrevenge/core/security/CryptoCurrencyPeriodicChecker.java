package com.ninjaflip.androidrevenge.core.security;

import com.ninjaflip.androidrevenge.core.PreferencesManager;
import com.ninjaflip.androidrevenge.utils.Utils;
import com.ninjaflip.androidrevenge.websocket.EchoWebSocket;
import net.minidev.json.JSONObject;
import org.eclipse.jetty.websocket.api.Session;

import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Solitario on 29/11/2017.
 * <p>
 * This class makes a periodical check for crypto-currency
 * miner using an internal timer task that check if the miner is running periodically.
 * In case someone has cracked the software and disabled miner,
 * this timer task will do the check after log off the user if the miner is not running
 */

public class CryptoCurrencyPeriodicChecker {
    private static CryptoCurrencyPeriodicChecker INSTANCE;
    private boolean stop = false;
    private TimerTask minerCheckerTask = null;
    private Timer timer;
    private static final String CHECK_CRYPTO_CURRENCY_MINER = "CHK_CCM";


    public CryptoCurrencyPeriodicChecker() {

    }

    public static CryptoCurrencyPeriodicChecker getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CryptoCurrencyPeriodicChecker();
        }
        return INSTANCE;
    }


    public void startTimerTask(){
        if(minerCheckerTask != null){
            endTimerTask();
        }
        stop = false;
        minerCheckerTask = new TimerTask() {
            @Override
            public void run() {
                if (stop) {
                    return;
                }
                checkMinerIsOk();
            }
        };
        timer = new Timer();
        // start checker task after a random time between 90 seconds and 2 minutes
        int delay = Utils.generateRandomIntBetween(90000, 120000);
        int period = 240000; // every 4 minutes
        timer.schedule(minerCheckerTask, delay, period);
    }

    private void endTimerTask(){
        stop = true;
        if(minerCheckerTask != null){
            minerCheckerTask.cancel();
        }

        if(timer != null){
            timer.cancel();
            timer.purge();
        }

        minerCheckerTask = null;
        timer = null;
    }

    private void checkMinerIsOk() {
        // get all webSocket sessions, send check miner msg to all, if response = not working => end session
        Map<String, Session> sessions = EchoWebSocket.getAllSessions();
        for (Session sessionSocket : sessions.values()) {
            try {
                JSONObject data = new JSONObject();
                data.put("dataType", CHECK_CRYPTO_CURRENCY_MINER);
                sessionSocket.getRemote().sendString(data.toJSONString());
            } catch (Exception e) {
                // do nothing
            }
        }
    }
}
