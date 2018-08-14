package com.ninjaflip.androidrevenge.core.db.dao;

import com.ninjaflip.androidrevenge.beans.AppAvailableCountries;
import com.ninjaflip.androidrevenge.core.db.DBManager;
import org.dizitart.no2.FindOptions;
import org.dizitart.no2.SortOrder;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;

import java.util.List;

/**
 * Created by Solitario on 15/11/2017.
 *
 * Data Access Object for AppAvailableCountries class
 */
public class AppAvailableCountriesDao {
    private static AppAvailableCountriesDao INSTANCE;
    private ObjectRepository<AppAvailableCountries> repository;

    private AppAvailableCountriesDao() {
        repository = DBManager.getInstance().getDb().getRepository(AppAvailableCountries.class);
    }

    public static AppAvailableCountriesDao getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AppAvailableCountriesDao();
        }
        return INSTANCE;
    }

    /**
     * Insert new record into database
     *
     * @param object the object to insert
     */
    public void insert(AppAvailableCountries object) {
        repository.insert(object);
    }


    /**
     * Get all records
     *
     * @return a list of AppAvailableCountries objects
     */
    public List<AppAvailableCountries> getAll(){
        return repository.find().toList();
    }

    /**
     * Get all records that belongs to user having uuid equal to userUuid
     *
     * @return a list of AppAvailableCountries objects
     */
    public List<AppAvailableCountries> getAll(String userUuid) {
        return repository.find(ObjectFilters.eq("owner.uuid",userUuid),
                FindOptions.sort("dateCreated", SortOrder.Descending)).toList();
    }

    /**
     * Remove a record
     *
     * @param record to be removed
     */
    public void delete(AppAvailableCountries record) {
        repository.remove(record);
    }

    public void deleteAll(String userUuid) {
        List<AppAvailableCountries> list = repository.find(ObjectFilters.eq("owner.uuid",userUuid),
                FindOptions.sort("dateCreated", SortOrder.Descending)).toList();
        for(AppAvailableCountries item : list){
            repository.remove(item);
        }
    }

    /**
     * Get a record by its Id
     *
     * @param uuid id of the record
     * @return AppAvailableCountries object
     */
    public AppAvailableCountries getByUuid(String uuid) {
        return repository.find(ObjectFilters.regex("uuid", uuid)).firstOrDefault();
    }

    /**
     * Get a record by its appId
     *
     * @param appId of the record
     * @return AppAvailableCountries object
     */
    public AppAvailableCountries getByAppId(String appId) {
        return repository.find(ObjectFilters.regex("appId", appId)).firstOrDefault();
    }

    /**
     * Check whether or not an app with a certain package name exists and belongs to user having uuid = userUuid
     * @param userUuid user's uuid
     * @param appId app id (package name)
     * @return true if the app exists and belong to that user, false otherwise
     */
    public boolean existsAndBelongsToUser(String appId, String userUuid){
        return repository.find(ObjectFilters.and(ObjectFilters.eq("owner.uuid", userUuid),
                ObjectFilters.eq("appId", appId))).size() != 0;
    }

    /**
     *  Get total number of items for a certain user
     * @param userUuid user's id
     * @return number of items per user defined by userUuid
     */
    public int countItemsPerUser(String userUuid){
        return repository.find(ObjectFilters.eq("owner.uuid",userUuid)).size();
    }
}
