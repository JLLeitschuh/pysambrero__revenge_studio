package com.ninjaflip.androidrevenge.beans;

import org.dizitart.no2.objects.Id;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Created by Solitario on 22/05/2017.
 *
 * * ApkToolProjectBean object representing an decoded apk work folder
 */


public class ApkToolProjectBean implements Serializable {
    @Id
    private String uuid;
    private String name;
    private String projectFolderNameUuid;
    private String iconBytesAsString;
    private String packageName;
    private UserBean owner;
    private Date dateCreated;


    public ApkToolProjectBean() {
    }

    public ApkToolProjectBean(String name, String projectFolderNameUuid,
                              String iconBytesAsString, String packageName, UserBean owner) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.projectFolderNameUuid = projectFolderNameUuid;
        this.iconBytesAsString = iconBytesAsString;
        this.packageName = packageName;
        this.owner = owner;
        this.dateCreated = new Date();
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProjectFolderNameUuid() {
        return projectFolderNameUuid;
    }

    public void setProjectFolderNameUuid(String projectFolderNameUuid) {
        this.projectFolderNameUuid = projectFolderNameUuid;
    }

    public String getIconBytesAsString() {
        return iconBytesAsString;
    }

    public void setIconBytesAsString(String iconBytesAsString) {
        this.iconBytesAsString = iconBytesAsString;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
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

    @Override
    public String toString() {
        return "ApkToolProjectBean{" +
                "uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", projectFolderNameUuid='" + projectFolderNameUuid + '\'' +
                ", iconBytesAsString='" + iconBytesAsString + '\'' +
                ", packageName='" + packageName + '\'' +
                ", owner=" + owner.getUuid() +
                ", dateCreated=" + dateCreated +
                '}';
    }
}
