package com.example.keystoreapp.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.example.keystoreapp.R;
import com.example.keystoreapp.SecurityConstants;
/**
 * This class represents dialog with three entry fields for entering information about subject for which the
 * certificate is being issued.
 *
 * @author Petr konecny
 *
 */
public class SubjectDialogFragment extends DialogFragment implements OnClickListener{

    EditText edit1;
	EditText edit2;
	EditText edit3;
    private Callbacks callbacks;

    public static interface Callbacks{
        /**
         * Implementation of this method should generate new certificate and it's private key
         *
         * @param commonName Common name used for the certificate
         * @param organization Organization used for the certificate
         * @param organization Organization unit used for the certificate
         * @param which Identificator of the operation
         */
        public void  generateCertificate(String commonName,String organization,String organizationUnit,int which);
    }

    //Methods overridden from DialogRFragment class

	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
		LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = inflater.inflate(R.layout.dialog_subject, null);
        edit1 = (EditText) view.findViewById(R.id.editText1);
        edit2 = (EditText) view.findViewById(R.id.editText2);
        edit3 = (EditText) view.findViewById(R.id.editText3);
        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("Generate", this);
        builder.setView(view);
        builder.setTitle("Fill out subject info");
        return builder.create();
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        callbacks = (Callbacks) getActivity();
    }

	@Override
	public void onClick(DialogInterface dialog, int which) {
        callbacks.generateCertificate(edit1.getText().toString(), edit2.getText().toString(), edit3.getText().toString(), SecurityConstants.GENERATE);
		this.dismiss();
	}

}
