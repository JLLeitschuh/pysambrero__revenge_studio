package com.ninjaflip.androidrevenge.core.scrapper.manager;

import com.ninjaflip.androidrevenge.beans.RestrictedOpBean;
import com.ninjaflip.androidrevenge.core.db.dao.RestrictedOpDao;
import com.ninjaflip.androidrevenge.enums.RestrictedOpType;
import com.ninjaflip.androidrevenge.utils.Utils;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Solitario on 27/12/2017.
 * <p>
 * This manager wraps Quota business logic applied to scrapping operations
 */
public class ScrappingQuotaManager {
    private final static Logger LOGGER = Logger.getLogger(ScrappingQuotaManager.class);
    private static ScrappingQuotaManager INSTANCE;

    // production
    private static final int QUOTA_PERIOD_UNIT = Calendar.MINUTE;
    private static final int QUOTA_PERIOD_AMOUNT = 60;
    private static final int QUOTA_MAX_APP_DETAILS_PER_PERIOD = 9;
    private static final int QUOTA_MAX_APP_COUNTRY_PER_PERIOD = 3;
    private static int QUOTA_PERIOD_IN_MINUTES = 0;
    public static final String QUOTA_DESCRIPTION_APP_DETAILS = QUOTA_MAX_APP_DETAILS_PER_PERIOD + " requests/Hour";
    public static final String QUOTA_DESCRIPTION_APP_COUNTRY = QUOTA_MAX_APP_COUNTRY_PER_PERIOD + " requests/Hour";



    // test only
    /*private static final int QUOTA_PERIOD_UNIT = Calendar.MINUTE;
    private static final int QUOTA_PERIOD_AMOUNT = 10;
    private static final int QUOTA_MAX_APP_DETAILS_PER_PERIOD = 2;
    private static final int QUOTA_MAX_APP_COUNTRY_PER_PERIOD = 1;
    private static int QUOTA_PERIOD_IN_MINUTES = 0;
    public static final String QUOTA_DESCRIPTION_APP_DETAILS = QUOTA_MAX_APP_DETAILS_PER_PERIOD + " requests/120 min";
    public static final String QUOTA_DESCRIPTION_APP_COUNTRY = QUOTA_MAX_APP_COUNTRY_PER_PERIOD + " requests/120 min";*/

    static {
        switch (QUOTA_PERIOD_UNIT) {
            case Calendar.MINUTE: {
                QUOTA_PERIOD_IN_MINUTES = QUOTA_PERIOD_AMOUNT;
                break;
            }
            case Calendar.HOUR: {
                QUOTA_PERIOD_IN_MINUTES = QUOTA_PERIOD_AMOUNT * 60;
                break;
            }
            case Calendar.DAY_OF_YEAR:
            case Calendar.DAY_OF_MONTH:
            case Calendar.DAY_OF_WEEK:
            case Calendar.DAY_OF_WEEK_IN_MONTH:{
                QUOTA_PERIOD_IN_MINUTES = QUOTA_PERIOD_AMOUNT * 24 * 60;
                break;
            }
        }
    }

    private ScrappingQuotaManager() {
    }

    public static ScrappingQuotaManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ScrappingQuotaManager();
        }
        return INSTANCE;
    }

    /**
     * @return JSON object containing the number of remaining APP_DETAILS scrapping
     * operations for the current Quota Period cycle, and ttw (time to wait) if quota is reached
     */
    public JSONObject getAppDetailsQuotaInfo(String userUuid) {
        JSONObject result = new JSONObject();
        Date now = Utils.getNtpTime();
        if(now == null){
            now = new Date();
        }

        // get list of APP_DETAILS operations for the current Quota Period cycle
        List<RestrictedOpBean> lastOps = RestrictedOpDao.getInstance()
                .getLastRecordsByType(userUuid, RestrictedOpType.APP_DETAILS, now, QUOTA_PERIOD_UNIT, QUOTA_PERIOD_AMOUNT);
        int nbLastOps = lastOps.size();

        if (nbLastOps >= QUOTA_MAX_APP_DETAILS_PER_PERIOD) {
            RestrictedOpBean lasOperation = lastOps.get(nbLastOps-1);
            Date lasOperationDate = lasOperation.getDateCreated();
            long timeDiff = Math.abs(lasOperationDate.getTime() - now.getTime());
            long minutesToWait = Math.abs(QUOTA_PERIOD_IN_MINUTES - TimeUnit.MILLISECONDS.toMinutes(timeDiff));
            result.put("remaining", 0); // remaining number of operations for the current Quota Period cycle, 0 means Quota exceeded
            result.put("ttw", minutesToWait); // ttw : time to wait in minutes
        } else {
            result.put("remaining", QUOTA_MAX_APP_DETAILS_PER_PERIOD - nbLastOps);// remaining number of operations for the current Quota Period cycle, 0 means Quota exceeded
            result.put("ttw", 0);// ttw : time to wait in minutes , 0 means Quota not reached yet
        }
        return result;
    }

    /**
     * @return JSON object containing the number of remaining APP_COUNTRY scrapping
     * operations for the current Quota Period cycle, and ttw (time to wait) if quota is reached
     */
    public JSONObject getAppCountryQuotaInfo(String userUuid) {
        JSONObject result = new JSONObject();

        Date now = Utils.getNtpTime();
        if(now == null){
            now = new Date();
        }
        // get list of APP_COUNTRY operations for the current Quota Period cycle
        List<RestrictedOpBean> lastOps = RestrictedOpDao.getInstance()
                .getLastRecordsByType(userUuid, RestrictedOpType.APP_COUNTRY, now, QUOTA_PERIOD_UNIT, QUOTA_PERIOD_AMOUNT);
        int nbLastOps = lastOps.size();

        if (nbLastOps >= QUOTA_MAX_APP_COUNTRY_PER_PERIOD) {
            RestrictedOpBean lasOperation = lastOps.get(nbLastOps-1);
            Date lasOperationDate = lasOperation.getDateCreated();
            long timeDiff = Math.abs(lasOperationDate.getTime() - now.getTime());
            long minutesToWait = Math.abs(QUOTA_PERIOD_IN_MINUTES - TimeUnit.MILLISECONDS.toMinutes(timeDiff));
            result.put("remaining", 0); // remaining number of operations for the current Quota Period cycle, 0 means Quota exceeded
            result.put("ttw", minutesToWait); // ttw : time to wait in minutes
        } else {
            result.put("remaining", QUOTA_MAX_APP_COUNTRY_PER_PERIOD - nbLastOps);// remaining number of operations for the current Quota Period cycle, 0 means Quota exceeded
            result.put("ttw", 0);// ttw : time to wait in minutes , 0 means Quota not reached yet
        }
        return result;
    }


    /**
     * This method will remove all records that are present in the database more than 30 hours.
     * Since we use RestrictedOpBean for Quota period cycle, we only need fresh records (last 30 hours).
     * So we need to clean the database at every program start to keep its size reasonable.
     * This method will clean the old records and keep the fresh ones.
     */
    public void cleanOldRecords(){
        try {
            RestrictedOpDao.getInstance().deleteOldRecords();
        }catch (Exception e){
            // do nothing
        }
    }

    /**
     * convert minutes to readable time string example: 245 min => 4 hours and 5 minutes
     * @param ttwInMinutes integer representing a number of minutes
     * @return String duration
     */
    public String timeToWaitAsString(int ttwInMinutes) {
        int hours = ttwInMinutes / 60;
        int minutes = ttwInMinutes % 60;
        String result;
        String hoursStr;

        if (hours == 0) {
            hoursStr = "";
        } else if (hours == 1) {
            hoursStr = "1 hour";
        } else {
            hoursStr = hours + " hours";
        }

        if (minutes == 0) {
            result = hoursStr;
        } else if (minutes == 1) {
            if(hours == 0) {
                result = minutes + " minute";
            }else{
                result = hoursStr + " and " + minutes + " minute";
            }
        } else {
            if(hours == 0) {
                result = minutes + " minutes";
            }else{
                result = hoursStr+ " and " + minutes + " minutes";
            }
        }

        return result;
    }
}
