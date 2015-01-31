package com.example.keystoreapp.fragment;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * This class serves as confirmation yes/no dialog, when the option to reset application is selected.
 *
 * @author Petr Konecny
 *
 */
public class ResetDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private Callbacks callbacks;

    public static interface Callbacks{
        /**
         * Implementation of this method should delete keystore file and wipe password
         */
        public void resetKeystore();
    }

    //Methods overridden from DialogFragment class

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        callbacks = (Callbacks) getActivity();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setNegativeButton("No",null);
        builder.setPositiveButton("Yes",this);
        builder.setTitle("Delete keystore");
        builder.setMessage("This action will delete keystore and all its content, do you want to continue?");
        return builder.create();
    }

    //Implementation of onClickListener

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        callbacks.resetKeystore();
    }
}
