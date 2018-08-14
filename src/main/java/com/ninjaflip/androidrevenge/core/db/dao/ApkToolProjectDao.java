package com.ninjaflip.androidrevenge.core.db.dao;


import com.ninjaflip.androidrevenge.beans.ApkToolProjectBean;
import com.ninjaflip.androidrevenge.core.db.DBManager;
import org.dizitart.no2.FindOptions;
import org.dizitart.no2.SortOrder;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;

import java.util.List;

/**
 * Created by Solitario on 22/05/2017.
 *
 * Data Access Object for ApkToolProject class
 */
public class ApkToolProjectDao {
    private static ApkToolProjectDao INSTANCE;
    private ObjectRepository<ApkToolProjectBean> repository;

    private ApkToolProjectDao() {
        repository = DBManager.getInstance().getDb().getRepository(ApkToolProjectBean.class);
    }

    public static ApkToolProjectDao getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ApkToolProjectDao();
        }
        return INSTANCE;
    }

    /**
     * Insert new record into database
     * @param project the object to insert
     */
    public void insert(ApkToolProjectBean project){
        repository.insert(project);
    }

    /**
     * Get all records from the db
     * @return a list of ApkToolProjectBean objects
     */
    public List<ApkToolProjectBean> getAll(){
        return repository.find().toList();
    }

    /**
     * Get all records that belongs to user having uuid equal to userUuid
     * @return a list of ApkToolProjectBean objects
     */
    public List<ApkToolProjectBean> getAll(String userUuid){
        return repository.find(ObjectFilters.eq("owner.uuid",userUuid),
                FindOptions.sort("dateCreated", SortOrder.Descending)).toList();
    }

    /**
     * Update a record
     * @param record to be updated
     */
    public void update(ApkToolProjectBean record){
        repository.update(record, true);
    }

    /**
     * Remove a record
     * @param record to be removed
     */
    public void delete(ApkToolProjectBean record){
        repository.remove(record);
    }

    /**
     * Get a record by its uuid
     * @param uuid id of the record
     * @return ApkToolProjectBean object
     */
    public ApkToolProjectBean getByUuid(String uuid){
        return repository.find(ObjectFilters.eq("uuid",uuid)).firstOrDefault();
    }

    /**
     * Get a record by its name
     * @param name project's name
     * @return ApkToolProjectBean object
     */
    public ApkToolProjectBean getByName(String name){
        return repository.find(ObjectFilters.eq("name",name)).firstOrDefault();
    }



    /**
     * Check whether or not a project with a certain name exists and belongs to user having uuid = userUuid
     * @param userUuid user's uuid
     * @param projectName name of the project
     * @return true if the project exist and belong to that user, false otherwise
     */
    public boolean existProjectWithName(String userUuid, String projectName){
        return repository.find(ObjectFilters.and(ObjectFilters.eq("owner.uuid", userUuid),
                ObjectFilters.eq("name", projectName))).size() != 0;
    }


    /**
     * Check whether or not a project with a certain uuid exists and belongs to user having uuid = userUuid
     * @param userUuid user's uuid
     * @param projectUuid project's uuid
     * @return true if the project exist and belong to that user, false otherwise
     */
    public boolean projectExistsAndBelongsToUser(String projectUuid, String userUuid){
        return repository.find(ObjectFilters.and(ObjectFilters.eq("owner.uuid", userUuid),
                ObjectFilters.eq("uuid", projectUuid))).size() != 0;
    }


    //****************** Tests
    public void scriptInsert() {
        /*ApkToolProjectBean p1 = new ApkToolProjectBean("peppa pig", "", "ERG647678UUID", "5376E2345AGRAPH");
        ApkToolProjectBean p2 = new ApkToolProjectBean("clash gems", "","ERG3448UUID", "537222245AGRAPH");
        ApkToolProjectBean p3 = new ApkToolProjectBean("clash royale", "","ERD238UUID", "53567245AGRAPH");
        ApkToolProjectBean p4 = new ApkToolProjectBean("subway surfers","", "ERG647678UUID", "537765345AGRAPH");
        ApkToolProjectBean p5 = new ApkToolProjectBean("MSP VIP","", "38547678UUID", "5376ETT54GRAPH");

        repository.insert(p1);
        repository.insert(p2);
        repository.insert(p3);
        repository.insert(p4);
        repository.insert(p5);*/
    }

    public void repoFilterTest() {
        List<ApkToolProjectBean> items = repository.find().toList();
        System.out.println("items size : "+ items.size());
        for (ApkToolProjectBean proj : items) {
            // process the document
            System.out.println("project :" + proj.toString());
        }
    }

    public void repoFilterContainsTest(String contains) {
        List<ApkToolProjectBean> items = repository.find(
                ObjectFilters.text("name", contains)).toList();
        System.out.println("items size : "+ items.size());
        for (ApkToolProjectBean proj : items) {
            // process the document
            System.out.println("project :" + proj.toString());
        }
    }
}
