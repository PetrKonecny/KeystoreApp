package com.example.keystoreapp.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.io.File;
import java.security.KeyStore;

/**
 * This class saves some items, that can't be put into Bundle from destruction. It is set as retained
 * so it is not destroyed along with the MainActivity. But in some cases it can be destroyed anyway.
 *
 * @author Petr konecny
 *
 */
public class RetainedFragment extends Fragment {
    private KeyStore store;
    private File file;

    //Basic getters and setters

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public KeyStore getStore() {
        return store;
    }

    public void setStore(KeyStore store) {
        this.store = store;
    }

    // Methods overridden from Fragment class

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
}
