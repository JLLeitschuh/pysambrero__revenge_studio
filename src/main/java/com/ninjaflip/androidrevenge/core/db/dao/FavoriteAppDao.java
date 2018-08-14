package com.ninjaflip.androidrevenge.core.db.dao;

import com.ninjaflip.androidrevenge.beans.FavoriteAppBean;
import com.ninjaflip.androidrevenge.core.db.DBManager;
import org.dizitart.no2.FindOptions;
import org.dizitart.no2.SortOrder;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;

import java.util.List;

/**
 * Created by Solitario on 12/11/2017.
 *
 * Data Access Object for FavoriteAppBean class
 */
public class FavoriteAppDao {
    private static FavoriteAppDao INSTANCE;
    private ObjectRepository<FavoriteAppBean> repository;

    private FavoriteAppDao() {
        repository = DBManager.getInstance().getDb().getRepository(FavoriteAppBean.class);
    }

    public static FavoriteAppDao getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FavoriteAppDao();
        }
        return INSTANCE;
    }

    /**
     * Insert new record into database
     *
     * @param object the object to insert
     */
    public void insert(FavoriteAppBean object) {
        repository.insert(object);
    }


    /**
     * Get all records
     *
     * @return a list of FavoriteAppBean objects
     */
    public List<FavoriteAppBean> getAll(){
        return repository.find().toList();
    }

    /**
     * Get all records that belongs to user having uuid equal to userUuid
     *
     * @return a list of FavoriteAppBean objects
     */
    public List<FavoriteAppBean> getAll(String userUuid) {
        return repository.find(ObjectFilters.eq("owner.uuid",userUuid),
                FindOptions.sort("dateCreated", SortOrder.Descending)).toList();
    }

    /**
     * Remove a record
     *
     * @param record to be removed
     */
    public void delete(FavoriteAppBean record) {
        repository.remove(record);
    }

    public void deleteAll(String userUuid) {
        List<FavoriteAppBean> listFavApps = repository.find(ObjectFilters.eq("owner.uuid",userUuid),
                FindOptions.sort("dateCreated", SortOrder.Descending)).toList();
        for(FavoriteAppBean fa : listFavApps){
            repository.remove(fa);
        }
    }

    /**
     * Get a record by its Id
     *
     * @param uuid id of the record
     * @return FavoriteAppBean object
     */
    public FavoriteAppBean getByUuid(String uuid) {
        return repository.find(ObjectFilters.regex("uuid", uuid)).firstOrDefault();
    }

    /**
     * Get a record by its appId
     *
     * @param appId of the record
     * @return FavoriteAppBean object
     */
    public FavoriteAppBean getByAppId(String appId) {
        return repository.find(ObjectFilters.regex("appId", appId)).firstOrDefault();
    }

    /**
     * Check whether or not an app with a certain package name exists and belongs to user having uuid = userUuid
     * @param userUuid user's uuid
     * @param appId app id (package name)
     * @return true if the app exists and belong to that user, false otherwise
     */
    public boolean favoriteAppExistsAndBelongsToUser(String appId, String userUuid){
        return repository.find(ObjectFilters.and(ObjectFilters.eq("owner.uuid", userUuid),
                ObjectFilters.eq("appId", appId))).size() != 0;
    }

    /**
     *  Get total number of favorite apps for a certain user
     * @param userUuid user's id
     * @return number of favorite apps per user defined by userUuid
     */
    public int countFavoriteAppsPerUser(String userUuid){
        return repository.find(ObjectFilters.eq("owner.uuid",userUuid)).size();
    }
}
