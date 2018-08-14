package com.ninjaflip.androidrevenge.beans;

import com.ninjaflip.androidrevenge.enums.RestrictedOpType;
import com.ninjaflip.androidrevenge.utils.Utils;
import org.dizitart.no2.objects.Id;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Created by Solitario on 27/12/2017.
 *
 * RestrictedOpBean object represents an operation under Quota
 */
public class RestrictedOpBean implements Serializable {
    @Id
    private String uuid;
    private String type;
    private UserBean owner;
    private Date dateCreated;


    public RestrictedOpBean() {
    }

    public RestrictedOpBean(RestrictedOpType type, UserBean owner) {
        this.uuid = UUID.randomUUID().toString();
        this.type = type.name();
        this.owner = owner;

        Date date = Utils.getNtpTime();
        if(date == null){
            date = new Date();
        }
        this.dateCreated = date;
    }

    public String getUuid() {
        return uuid;
    }

    public String getType() {
        return type;
    }

    public UserBean getOwner() {
        return owner;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    @Override
    public String toString() {
        return "RestrictedOpBean{" +
                "uuid='" + uuid + '\'' +
                ", type='" + type + '\'' +
                ", owner=" + owner.getUuid() +
                ", dateCreated=" + dateCreated +
                '}';
    }
}
