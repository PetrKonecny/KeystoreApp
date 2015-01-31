package com.example.keystoreapp;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
/**
 * This class represents an array adapter, which function is to take some class item, in this case EntryRecord
 * and transform that into view, that can be displayed as item in the list
 *
 * @author Petr Konecny
 */
public class KeystoreItemAdapter extends ArrayAdapter<EntryRecord> {

    private ArrayList<EntryRecord> entries;
    private Context context;
    private int layoutResourceId;

    //Constructor
    public KeystoreItemAdapter(Context context, int layoutResourceId,
                               ArrayList<EntryRecord> entries) {
        super(context, layoutResourceId, entries);
        this.entries = entries;
        this.context = context;
        this.layoutResourceId = layoutResourceId;
    }

    //Methods overridden from ArrayAdapter class

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId,parent,false);
        }
        EntryRecord entry = entries.get(position);
        TextView alias = (TextView) row.findViewById(android.R.id.text1);
        TextView type = (TextView) row.findViewById(android.R.id.text2);
        alias.setText(entry.getAlias());
        type.setText(entry.getDescription());
        return row;
    }

}







