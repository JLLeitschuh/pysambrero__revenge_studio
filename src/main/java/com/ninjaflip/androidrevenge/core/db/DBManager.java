package com.ninjaflip.androidrevenge.core.db;

import com.ninjaflip.androidrevenge.core.Configurator;
import com.ninjaflip.androidrevenge.beans.UserBean;
import com.ninjaflip.androidrevenge.core.db.dao.UserDao;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.internals.JacksonMapper;
import org.dizitart.no2.objects.filters.ObjectFilters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * Created by Solitario on 21/05/2017.
 * <p>
 * A wrapper class for Nitrite database
 */

public class DBManager {

    private static volatile DBManager INSTANCE;
    private Nitrite db;

    private DBManager() {
        Map<String, String> config = readConfigurationFile();

        String databaseName = config.get("dbname");
        String db_path = Configurator.getInstance().getDB() + File.separator + databaseName;

        db = Nitrite.builder().nitriteMapper(new JacksonMapper())
                .compressed().filePath(db_path)
                .openOrCreate(config.get("dbuser"), config.get("dbpass"));
        // create default user
        List<UserBean> users = db.getRepository(UserBean.class).find().toList();
        if (users.size() == 0) {
            try {
                // TODO "admin" must be encrypted ==> against reverse engineering
                UserBean userBean = new UserBean("admin", "admin");
                userBean.setReminderPhrase("username is 'admin' and password is 'admin'");
                db.getRepository(UserBean.class).insert(userBean);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static DBManager getInstance() {
        if (INSTANCE == null) {
            // synchronized block to avoid dead-lock on Nitrite db initialization
            // be cause nitrite isn't thread safe
            synchronized(DBManager.class) {
                if (INSTANCE == null)
                    INSTANCE = new DBManager();
            }
        }
        return INSTANCE;
    }

    public Nitrite getDb() {
        return db;
    }

    /**
     * Close Nitrite database
     */
    public void closeDB() {
        if (db != null && !db.isClosed()) {
            try {
                if (db.hasUnsavedChanges())
                    db.commit();

                db.close();
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    /**
     * Load properties from configuration files
     * @return Map containing database configuration parameters
     */
    private Map<String, String> readConfigurationFile() {
        Properties prop = new Properties();
        Map<String, String> propertiesMap = new HashMap<>();
        InputStream input = null;
        try {
            input = this.getClass().getClassLoader().getResourceAsStream("conf/config.properties");
            // load a properties file
            prop.load(input);

            for (String key : prop.stringPropertyNames()) {
                String value = prop.getProperty(key);
                propertiesMap.put(key, value);
            }

            propertiesMap.put("dbname", prop.getProperty("dbname"));
            propertiesMap.put("dbuser", prop.getProperty("dbuser"));
            propertiesMap.put("dbpass", prop.getProperty("dbpass"));

            return propertiesMap;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
