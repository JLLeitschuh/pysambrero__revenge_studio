package com.ninjaflip.androidrevenge.core.apktool.updater;

/**
 * Created by Solitario on 01/11/2017.
 *
 * Renaming object that contains the canonical class name(i.e. com.package.name.ClassName)
 * and the newClassSimpleName, so at the end we will rename :
 * all occurrences of 'com.package.name.ClassName' by 'com.package.name.newClassSimpleName'
 * and all occurrences of 'com/package/name/ClassName' by 'com/package/name/newClassSimpleName'
 * and all occurrences of 'com$package$name$ClassName' by 'com$package$name$newClassSimpleName'
 */
public class RenamingObject {
    private String canonicalClassName;
    private String newClassSimpleName;

    public RenamingObject(String canonicalClassName, String newClassSimpleName) {
        this.canonicalClassName = canonicalClassName;
        this.newClassSimpleName = newClassSimpleName;
    }

    public String getCanonicalClassName() {
        return canonicalClassName;
    }

    public String getNewClassSimpleName() {
        return newClassSimpleName;
    }

    public String getOriginalClassSimpleName(){
        String[] split = canonicalClassName.split("\\.");
        return split[split.length -1];
    }
}
