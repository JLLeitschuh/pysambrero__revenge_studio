package com.ninjaflip.androidrevenge.beans;

import net.minidev.json.JSONObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class ScrappedContentNoDuplicate implements Serializable {


    private List<JSONObject> shortDescriptionsAndTitlesPerLanguage; // [{lang:'fr-FR', lang_name:"French-FRANCE", shortDesc:'blabla', title:'bloblo'},...]
    private String longDescription;
    private Collection<String> associatedLanguageCodes;



    public ScrappedContentNoDuplicate() {
    }

    public Collection<String> getAssociatedLanguageCodes() {
        return associatedLanguageCodes;
    }

    public void setAssociatedLanguageCodes(Collection<String> associatedLanguageCodes) {
        this.associatedLanguageCodes = associatedLanguageCodes;
    }

    public String getLongDescription() {
        return longDescription;
    }

    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
    }

    public List getShortDescriptionsAndTitlesPerLanguage() {
        return shortDescriptionsAndTitlesPerLanguage;
    }

    public void setShortDescriptionsAndTitlesPerLanguage(List<JSONObject> shortDescriptionsAndTitlesPerLanguage) {
        this.shortDescriptionsAndTitlesPerLanguage = shortDescriptionsAndTitlesPerLanguage;
    }
}
