package com.ninjaflip.androidrevenge.beans;

import org.dizitart.no2.objects.Id;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by Solitario on 04/06/2017.
 * <p>
 * application user object
 */
public class UserBean implements Serializable {
    @Id
    private String uuid;
    private String userId;
    private String password;
    private String reminderPhrase;

    public UserBean() {
    }

    public UserBean(String userId, String password) {
        this.uuid = UUID.randomUUID().toString();
        this.userId = userId;
        this.password = password;
    }

    public String getUuid() {
        return uuid;
    }

    public String getPassword() {
        return password;
    }

    public void setUPwd(String password) {
        this.password = password;
    }

    public String getMockPwd() {
        String bullet = "\u25CF";// bullet code is \u25CF
        StringBuilder pwdMock = new StringBuilder();
        for (int i = 0; i < this.password.length(); i++) {
            if (i % 2 == 0) {
                pwdMock.append(password.charAt(i));
            } else {
                pwdMock.append(bullet);
            }
        }
        return pwdMock.toString();
    }


    public String getUserId() {
        return userId;
    }

    public void setUId(String userId) {
        this.userId = userId;
    }

    public String getReminderPhrase() {
        return reminderPhrase;
    }

    public void setReminderPhrase(String reminderPhrase) {
        this.reminderPhrase = reminderPhrase;
    }

    @Override
    public String toString() {
        return "UserBean{" +
                "password='*************\'" +
                ", uuid='" + uuid + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserBean)) return false;
        UserBean userBean = (UserBean) o;
        return getUuid().equals(userBean.getUuid());
    }

    @Override
    public int hashCode() {
        int result = getUuid().hashCode();
        result = 31 * result + getUserId().hashCode();
        return result;
    }
}


