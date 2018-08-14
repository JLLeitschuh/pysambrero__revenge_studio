package com.ninjaflip.androidrevenge.core.db.dao;

import com.ninjaflip.androidrevenge.beans.RestrictedOpBean;
import com.ninjaflip.androidrevenge.core.db.DBManager;
import com.ninjaflip.androidrevenge.enums.RestrictedOpType;
import com.ninjaflip.androidrevenge.utils.Utils;
import org.dizitart.no2.FindOptions;
import org.dizitart.no2.SortOrder;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Solitario on 12/11/2017.
 *
 * Data Access Object for RestrictedOpBean class
 */
public class RestrictedOpDao {
    private static RestrictedOpDao INSTANCE;
    private ObjectRepository<RestrictedOpBean> repository;

    private RestrictedOpDao() {
        repository = DBManager.getInstance().getDb().getRepository(RestrictedOpBean.class);
    }

    public static RestrictedOpDao getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RestrictedOpDao();
        }
        return INSTANCE;
    }

    /**
     * Insert new record into database
     *
     * @param object the object to insert
     */
    public void insert(RestrictedOpBean object) {
        repository.insert(object);
    }

    /**
     * Get a record by its Id
     *
     * @param uuid id of the record
     * @return RestrictedOpBean object
     */
    public RestrictedOpBean getByUuid(String uuid) {
        return repository.find(ObjectFilters.regex("uuid", uuid)).firstOrDefault();
    }

    /**
     * Remove a record
     *
     * @param record to be removed
     */
    public void delete(RestrictedOpBean record) {
        repository.remove(record);
    }


    /**
     * Get records of the last Quota Period cycle, that belongs to user having uuid equal to
     * userUuid, then sort those records in descending order
     * @return a list of RestrictedOpBean objects
     */
    public List<RestrictedOpBean> getLastRecordsByType(String userUuid, RestrictedOpType type, Date now, int quotaPeriodUnit, int quotaPeriodAmount) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(quotaPeriodUnit, -quotaPeriodAmount);
        Date before = cal.getTime();

        return repository.find(
                ObjectFilters.and(ObjectFilters.eq("owner.uuid",userUuid),
                        ObjectFilters.eq("type",type.name()), ObjectFilters.gt("dateCreated", before)),
                FindOptions.sort("dateCreated", SortOrder.Descending)).toList();
    }

    /**
     * This method will remove all records that are present in the database more than 10 hours.
     * Since we use RestrictedOpBean for Quota period cycle, we only need fresh records (last 10 hours).
     * So we need to clean the database at every program start to keep its size reasonable.
     * This method will clean the old records and keep the fresh ones.
     */
    public void deleteOldRecords() {
        Date now = Utils.getNtpTime();
        if(now == null){
            now = new Date();
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.HOUR, -10);
        Date before = cal.getTime();

        List<RestrictedOpBean> list = repository.find(
                ObjectFilters.and(ObjectFilters.lt("dateCreated", before))
        ).toList();

        for(RestrictedOpBean o : list){
            repository.remove(o);
        }
    }

    public void deleteAll() {
        List<RestrictedOpBean> list = repository.find().toList();
        for(RestrictedOpBean o : list){
            repository.remove(o);
        }
    }
}
