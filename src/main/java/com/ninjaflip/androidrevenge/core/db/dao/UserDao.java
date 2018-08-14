package com.ninjaflip.androidrevenge.core.db.dao;

import com.ninjaflip.androidrevenge.beans.UserBean;
import com.ninjaflip.androidrevenge.core.db.DBManager;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;

import java.util.List;

/**
 * Created by Solitario on 04/06/2017.
 * <p>
 * Data Access Object for UserBean class
 */

public class UserDao {
    private static UserDao INSTANCE;
    private ObjectRepository<UserBean> repository;

    private UserDao() {
        repository = DBManager.getInstance().getDb().getRepository(UserBean.class);
    }

    public static UserDao getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UserDao();
        }
        return INSTANCE;
    }

    /**
     * Insert new record into database
     *
     * @param record the object to insert
     */
    public void insert(UserBean record) throws Exception {
        UserBean user = repository.find(ObjectFilters.regex("userId", record.getUserId())).firstOrDefault();
        if(user != null)
            throw new Exception("Username '" + record.getUserId() + "' already exists");
        List<UserBean> users = repository.find().toList();
        if(users.size() == 0) {
            repository.insert(record);
        }else{
            throw new Exception("Only one user can exist in the database");
        }
    }

    /**
     * Get all records from the db
     *
     * @return a list of objects
     */
    public List<UserBean> getAll() {
        return repository.find().toList();
    }

    /**
     * Update a record
     *
     * @param record to be updated
     */
    public void update(UserBean record) {
        repository.update(record, true);
    }

    /**
     * Remove a record
     *
     * @param record to be removed
     */
    public void delete(UserBean record) {
        repository.remove(record);
    }

    /**
     * Get a record by its uuid
     *
     * @param uuid id of the record
     * @return UserBean object
     */
    public UserBean getByUuid(String uuid) {
        return repository.find(ObjectFilters.regex("uuid", uuid)).firstOrDefault();
    }

    /**
     * Get a record by its userId (username)
     *
     * @param userName the username
     * @return UserBean object
     */
    public UserBean getByUserId(String userName) {
        return repository.find(ObjectFilters.regex("userId", userName)).firstOrDefault();
    }


    /**
     * Check whether a record with the given userId and password already exists in the database
     *
     * @param userName   user identifier
     * @param password user password
     * @return true if found, false otherwise
     */
    public boolean authenticateUser(String userName, String password) {
        UserBean items = repository.find(ObjectFilters.and(ObjectFilters.eq("userId", userName),
                ObjectFilters.eq("password", password))).firstOrDefault();
        return items != null;
    }
}
