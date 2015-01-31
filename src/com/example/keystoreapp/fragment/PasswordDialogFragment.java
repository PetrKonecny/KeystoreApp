package com.example.keystoreapp.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.example.keystoreapp.R;
import com.example.keystoreapp.SecurityConstants;
import com.example.keystoreapp.utils.Utils;
/**
 * This class represents basic password dialog. This dialog cannot be closed without hitting cancel button
 * or back arrow and it stops user from entering empty password.
 *
 * @author Petr konecny
 *
 */
public class PasswordDialogFragment extends DialogFragment implements OnClickListener{
	
	private Callbacks callbacks;
	private int which;

    public static interface Callbacks {
        /**
         * Implementation of this method should import private key to the store
         *
         * @param password Password for the item being imported
         * @param dialog Dialog to be closed after succesfull import
         */
		public void importKey(char[] password,DialogFragment dialog);
        /**
         * Implementation of this method should decrypt the keystore file and load it
         *
         * @param password Password for the keystore
         * @param dialog Dialog to be closed after succesfull unlocking
         */
		public void unlockStore(String password, DialogFragment dialog);
        /**
         * Implementation of this method should import keystore in PKCS12 format
         *
         * @param password Password for the keystore
         * @param dialog Dialog to be closed after succesfull import
         */
        public void importStore(String password, DialogFragment dialog);
	}

    //Methods overridden from DialogFragment

	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
		callbacks = (Callbacks) getActivity();
	}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		super.onCreateDialog(savedInstanceState);
		which = getArguments().getInt("which");
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.keypass_fragment, null);
		builder.setView(view);
		builder.setPositiveButton("OK", null);
		
		if(which == SecurityConstants.IMPORT_KEY){
			builder.setNegativeButton("Cancel", null);
			builder.setTitle("Key is password protected");
		}
        if(which == SecurityConstants.IMPORT_STORE){
            builder.setNegativeButton("Cancel", null);
            builder.setTitle("File is password protected");
        }
		if(which == SecurityConstants.UNLOCK){
			builder.setTitle("Keystore is locked");
		}
		
		return builder.create();
		
	}

    @Override
    public void onResume() {
	        super.onResume();
	        AlertDialog dialog = (AlertDialog)getDialog();
	        dialog.setCanceledOnTouchOutside(false);
	        dialog.setCancelable(false);
	        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
	        positiveButton.setOnClickListener(this);
	    }
	
	@Override
	public void onClick(View view) {
		String password = getPassword();
		if(password == null) {
			showFail();
			return;
		}
		
		switch(which){
			case SecurityConstants.IMPORT_KEY:
				callbacks.importKey(getPassword().toCharArray(),this);
				break;
			case SecurityConstants.UNLOCK:
				callbacks.unlockStore(getPassword(),this);
				break;
            case SecurityConstants.IMPORT_STORE:
                callbacks.importStore(getPassword(),this);
		}
	}

    //Fragment specific methods

    /**
     * Shows message if password wasn't filled out
     */
	public void showFail(){
		Utils.showSimpleDialog("You can't enter empty password, please try again","Wrong Password", getActivity());
	}

    /**
     * Gets filled out password from the edit text field
     *
     * @return String from the filed or @null if it was too short
     */
	public String getPassword(){
		EditText edit = (EditText) getDialog().findViewById(R.id.editText1);
		Editable string = edit.getText();
		if(string.length()<1) {
			return null;
		}
		return string.toString();
	}
	
}
