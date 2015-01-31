package com.example.keystoreapp;

import android.util.Log;

/**
 * This class represents basic record displayed it the list. It contains all necessary infomation about
 * the item, that cen be used for displaying various things. It could be expanded to show other info about item
 * in the list.
 *
 * @author Petr Konecny
 */
public class EntryRecord {

    private String alias;
    private String description;
    private Boolean isPrivatekey = false;
    private Boolean isRoot = false;
    private Boolean isCertificate = false;
    private static final String TAG = "Entry record";

    //Basic getters and setters

    public Boolean getIsRoot() {
        return isRoot;
    }

    public void setIsRoot(Boolean isRoot) {
        this.isRoot = isRoot;
    }

    public Boolean getIsCertificate() {
        return isCertificate;
    }

    public void setIsCertificate(Boolean isCertificate) {
        this.isCertificate = isCertificate;
    }

    public Boolean getIsPrivatekey() { return isPrivatekey; }

    public void setIsPrivatekey(Boolean isPrivatekey) {
        this.isPrivatekey = isPrivatekey;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
        Log.i(TAG,"new alias set: " + alias);
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        Log.i(TAG,"new type set: " + description);
    }

    /**
     * Method determines, if this record is certificate with private key
     *
     * @Return True if it is both, false otherwise
     */
    public boolean isBoth(){
        return isCertificate && isPrivatekey;
    }
}
