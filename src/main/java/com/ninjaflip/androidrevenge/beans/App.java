package com.ninjaflip.androidrevenge.beans;


import java.io.Serializable;
import java.util.ArrayList;

public class App implements Serializable {
    public String appId = "";
    public ArrayList<String> categories = new ArrayList<>(); // ENUM
    public String name = "";
    public String FreeOrPaid="";

    public String icon = "";
    public Float rating = -1f;
    public String price  = "";
    public boolean free = true;
    public String developer = "";

    public App(){
    }

    @Override
    public String toString() {
        return "App{" +
                "appId='" + appId + '\'' +
                ", categories=" + categories +
                ", name='" + name + '\'' +
                ", FreeOrPaid='" + FreeOrPaid + '\'' +
                ", icon='" + icon + '\'' +
                ", rating=" + rating +
                ", price='" + price + '\'' +
                ", free=" + free +
                ", developer='" + developer + '\'' +
                '}';
    }
}
