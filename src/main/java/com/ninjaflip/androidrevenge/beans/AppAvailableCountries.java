package com.ninjaflip.androidrevenge.beans;


import org.dizitart.no2.objects.Id;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class AppAvailableCountries implements Serializable {
    @Id
    private String uuid;
    private String appId;
    private String title;
    private String developer;
    private String icon;
    private List<String> nonAvailableCountries;
    private UserBean owner;
    private Date dateCreated;
    private double duration;

    public AppAvailableCountries(){
    }

    public AppAvailableCountries(String appId, String title, String developer, String icon,
                                 List<String> nonAvailableCountries, UserBean owner) {
        this.uuid = UUID.randomUUID().toString();
        this.appId = appId;
        this.title = title;
        this.developer = developer;
        this.icon = icon;
        this.owner = owner;
        this.nonAvailableCountries = nonAvailableCountries;
        this.dateCreated = new Date();
    }

    public String getUuid() {
        return uuid;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public List<String> getNonAvailableCountries() {
        return nonAvailableCountries;
    }

    public void setNonAvailableCountries(List<String> nonAvailableCountries) {
        this.nonAvailableCountries = nonAvailableCountries;
    }

    public UserBean getOwner() {
        return owner;
    }

    public void setOwner(UserBean owner) {
        this.owner = owner;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "AppAvailableCountries{" +
                "uuid='" + uuid + '\'' +
                ", appId='" + appId + '\'' +
                ", title='" + title + '\'' +
                ", developer='" + developer + '\'' +
                ", icon='" + icon + '\'' +
                ", nonAvailableCountries=" + nonAvailableCountries +
                ", owner=" + owner.getUuid() +
                ", dateCreated=" + dateCreated +
                ", duration=" + duration +
                '}';
    }
}
