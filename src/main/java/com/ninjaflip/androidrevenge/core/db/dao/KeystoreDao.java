package com.ninjaflip.androidrevenge.core.db.dao;

import com.ninjaflip.androidrevenge.beans.KeystoreBean;
import com.ninjaflip.androidrevenge.core.db.DBManager;
import org.dizitart.no2.FindOptions;
import org.dizitart.no2.SortOrder;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;

import java.util.List;

/**
 * Created by Solitario on 22/05/2017.
 * <p>
 * Data Access Object for KeystoreBean class
 */
public class KeystoreDao {
    private static KeystoreDao INSTANCE;
    private ObjectRepository<KeystoreBean> repository;

    private KeystoreDao() {
        repository = DBManager.getInstance().getDb().getRepository(KeystoreBean.class);
    }

    public static KeystoreDao getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new KeystoreDao();
        }
        return INSTANCE;
    }

    /**
     * Insert new record into database
     *
     * @param ks the object to insert
     */
    public void insert(KeystoreBean ks) {
        repository.insert(ks);
    }


    /**
     * Get all records
     *
     * @return a list of KeystoreBean objects
     */
    public List<KeystoreBean> getAll(){
        return repository.find().toList();
    }

    /**
     * Get all records that belongs to user having uuid equal to userUuid
     *
     * @return a list of KeystoreBean objects
     */
    public List<KeystoreBean> getAll(String userUuid) {
        return repository.find(ObjectFilters.eq("owner.uuid",userUuid),
                FindOptions.sort("dateCreated", SortOrder.Descending)).toList();
    }

    /**
     * Update a record
     *
     * @param record to be updated
     */
    public void update(KeystoreBean record) {
        repository.update(record, true);
    }

    /**
     * Remove a record
     *
     * @param record to be removed
     */
    public void delete(KeystoreBean record) {
        repository.remove(record);
    }

    /**
     * Get a record by its Id
     *
     * @param uuid id of the record
     * @return KeystoreBean object
     */
    public KeystoreBean getByUuid(String uuid) {
        return repository.find(ObjectFilters.regex("uuid", uuid)).firstOrDefault();
    }

    /**
     * Check whether or not a keystore with a certain uuid exists and belongs to user having uuid = userUuid
     * @param userUuid user's uuid
     * @param keystoreUuid keystore's uuid
     * @return true if the keystore exist and belong to that user, false otherwise
     */
    public boolean keystoreExistsAndBelongsToUser(String keystoreUuid, String userUuid){
        return repository.find(ObjectFilters.and(ObjectFilters.eq("owner.uuid", userUuid),
                ObjectFilters.eq("uuid", keystoreUuid))).size() != 0;
    }

    /**
     * Check whether a record with the same alias already exists in the database
     *
     * @param alias the alias t search
     * @return true if found, false otherwise
     */
    public boolean repoContainsKeystore(String alias) {
        List<KeystoreBean> items = repository.find(ObjectFilters.regex("alias", alias)).toList();
        return items.size() > 0;
    }
}
