package com.example.keystoreapp.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
/**
 * This class represends simple dialog. It is better to use DialogFragment instead of simple dialog,
 * because simple dialog would close if configuration change occured (turning the screen). Therefore
 * it is better to wrap dialog in a fragment, that handles configuration changes automatically.
 *
 * @author Petr konecny
 *
 */
public class SimpleDialogFragment extends DialogFragment  {

    //Methods overridden from DialogFragment class

    @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setNeutralButton("OK", null);
        builder.setMessage(getArguments().getCharSequence("message"));
        builder.setTitle(getArguments().getCharSequence("title"));
        return builder.create();
	}
}
