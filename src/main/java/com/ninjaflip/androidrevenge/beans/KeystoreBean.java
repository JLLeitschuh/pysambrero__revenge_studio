package com.ninjaflip.androidrevenge.beans;

import org.dizitart.no2.objects.Id;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Created by Solitario on 31/05/2017.
 * <p>
 * Keystore object representing a keystore that contains a unique certificate
 */

/*@Indices({
        @Index(value = "description", type = IndexType.Fulltext)
})*/
public class KeystoreBean implements Serializable {
    @Id
    private String uuid;
    private String ksPass;
    private String alias;
    private String keyPass;
    private String description;
    private UserBean owner;
    private Date dateCreated;
    private byte[] blob; // contains the keystore file

    public KeystoreBean() {
    }

    public KeystoreBean(String ksPass, String alias, String keyPass, String description, UserBean owner, byte[] blob) {
        this.uuid = UUID.randomUUID().toString();
        this.ksPass = ksPass;
        this.alias = alias;
        this.keyPass = keyPass;
        this.description = description;
        this.owner = owner;
        this.blob = blob;
        this.dateCreated = new Date();
    }

    public String getUuid() {
        return uuid;
    }

    public String getKsPass() {
        return ksPass;
    }

    public void setKsPass(String ksPass) {
        this.ksPass = ksPass;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getKeyPass() {
        return keyPass;
    }

    public void setKeyPass(String keyPass) {
        this.keyPass = keyPass;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public byte[] getBlob() {
        return blob;
    }

    public void setBlob(byte[] blob) {
        this.blob = blob;
    }

    @Override
    public String toString() {
        return "KeystoreBean{" +
                "uuid='" + uuid + '\'' +
                ", ksPass='" + ksPass + '\'' +
                ", alias='" + alias + '\'' +
                ", keyPass='" + keyPass + '\'' +
                ", description='" + description + '\'' +
                ", owner=" + owner.getUuid() +
                ", dateCreated=" + dateCreated +
                '}';
    }
}

