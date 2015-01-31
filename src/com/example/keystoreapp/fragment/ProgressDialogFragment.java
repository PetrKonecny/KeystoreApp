package com.example.keystoreapp.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;

import com.example.keystoreapp.SecurityConstants;

import java.io.File;
import java.util.concurrent.TimeUnit;
/**
 * This class represents progress dialog, that is shown, when AsyncTask is running. It displays indeterminate
 * dialog when there is no way to get information about progress and determinate when the information is available
 * It also can stop async operation when the cancel button is available.
 *
 * @author Petr Konecny
 *
 */
public class ProgressDialogFragment extends DialogFragment implements OnClickListener,DialogInterface.OnCancelListener {
	
	private DialogCallbacks callbacks;
	private int which;
    private String message;

	public static interface DialogCallbacks{
        //Basic getters and setters
		public File getFile();

        /**
         * Implementation of this method should stop asynchronous operation
         *
         */
        public void cancel();
	}
	
	public void setWhich(int which){
		this.which = which;
	}

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null){
            message = savedInstanceState.getString("message");
            which = savedInstanceState.getInt("which");
        }
        //setRetainInstance(true);
	}

    @Override
	 public void onAttach(Activity activity) {
		    super.onAttach(activity);
		    callbacks = (DialogCallbacks) activity;
     }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("message",message);
        outState.putInt("which",which);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
		  if (getDialog() != null && getRetainInstance())
		    getDialog().setDismissMessage(null);
		  super.onDestroyView();
	}

	public void dismiss(){
		getDialog().dismiss();
	}

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		 
		final ProgressDialog dialog = new ProgressDialog(getActivity());
		dialog.setCancelable(true);
    	dialog.setIndeterminate(false);
        dialog.setCanceledOnTouchOutside(false);
        if (which == SecurityConstants.SIGN || which == SecurityConstants.VERIFY){
            double fileLength = Math.round(callbacks.getFile().length() / 10.24) / 100.0;
            message = ("<b>File name: </b>" + callbacks.getFile().getName() + "<br><br>") + "<b>File size: </b>" + fileLength + " Kb<br>";
	        dialog.setMax(100);
	        dialog.setButton(DialogInterface.BUTTON_NEUTRAL,"cancel",this);
	        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMessage(Html.fromHtml(message));
        }

        
        if (which == SecurityConstants.GENERATE  || which == SecurityConstants.IMPORT || which == SecurityConstants.IMPORT_STORE) {
	        dialog.setMessage("May take some time, deppending on your device, file size or enryption key length");
	        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        }
        if (which == SecurityConstants.GENERATE_ROOT) {
            dialog.setMessage("Please wait, generating root certificate and key");
        }
		return dialog;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		callbacks.cancel();
		dismiss();
	}

    @Override
    public void onCancel(DialogInterface dialog) {
        callbacks.cancel();
        dismiss();
    }


    // Fragment specific methods
    /**
     * Updates the progress dialog with estimated time
     *
     * @param time Time to be displayed in the dialog
     */
    public void updateTime(Long time){
        ProgressDialog dialog = (ProgressDialog) getDialog();
        if (dialog == null) return;
        double seconds = TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS);
        seconds /= 1000;
        String string = message + "<br><b>Estimated time left: </b>"+seconds+ " s" ;
        dialog.setMessage(Html.fromHtml(string));
    }

    /**
     * Updates the progress dialog with progress number
     *
     * @param progress Number to display as progress
     */
    public void update(Integer progress) {
        ProgressDialog dialog = (ProgressDialog) getDialog();
        if(dialog == null) return;
        dialog.setProgress(progress);
    }

}
