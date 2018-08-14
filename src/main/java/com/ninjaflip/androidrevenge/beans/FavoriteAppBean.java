package com.ninjaflip.androidrevenge.beans;

import org.dizitart.no2.objects.Id;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Created by Solitario on 12/11/2017.
 * <p>
 * This class represents user's favorite apps
 */
public class FavoriteAppBean implements Serializable {
    @Id
    private String uuid;
    private String appId;
    private String name;
    private String developer;
    private String shortDesc;
    private String icon;
    private Float rating;
    private String price;
    private boolean free;
    private UserBean owner;
    private Date dateCreated;

    public FavoriteAppBean() {
    }

    public FavoriteAppBean(String appId, String name, String developer, String shortDesc, String icon, float rating, String price,
                           boolean free, UserBean owner) {
        this.uuid = UUID.randomUUID().toString();
        this.appId = appId;
        this.name = name;
        this.developer = developer;
        this.shortDesc = shortDesc;
        this.icon = icon;
        this.rating = rating;
        this.price = price;
        this.free = free;
        this.owner = owner;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public void setShortDesc(String shortDesc) {
        this.shortDesc = shortDesc;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Float getRating() {
        return rating;
    }

    public void setRating(Float rating) {
        this.rating = rating;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
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

    public String getFormattedDateCreatedAsString() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        return fmt.format(dateCreated);
    }

    public void hideSensibleData(){
        owner = null;
    }


    @Override
    public String toString() {
        return "FavoriteAppBean{" +
                "uuid='" + uuid + '\'' +
                ", appId='" + appId + '\'' +
                ", name='" + name + '\'' +
                ", developer='" + developer + '\'' +
                ", shortDesc='" + shortDesc + '\'' +
                ", icon='" + icon+ '\'' +
                ", rating=" + rating +
                ", price='" + price + '\'' +
                ", free=" + free +
                ", owner=" + owner.getUuid() +
                ", dateCreated=" + dateCreated +
                '}';
    }
}
