package com.example.keystoreapp.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.keystoreapp.R;
/**
 * This class serves as simple setup menu for a new password. When a new valid password is entered,
 * it tells MainActivity to create new KeystoreFile
 *
 * @author Petr konecny
 *
 */
public class SetupFragment extends Fragment implements OnClickListener, TextWatcher {

	private Callbacks callbacks;
	
	public interface Callbacks{
        /**
         * Implementation of this method should create new keystore file in the storage

         * @param password Password for the keystore, this password should meet basic security needs
\         */
		public void createStore(String password);
	}

    //Methods overridden from Fragment class

	@Override
	public void onClick(View arg0) {
		callbacks.createStore(getPassword());
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
		callbacks = (Callbacks)getActivity();
	}
	
	
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState){
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.setup_fragment,container,false);
		view.findViewById(R.id.button1).setOnClickListener(this);
		view.findViewById(R.id.button1).setEnabled(false);
		((EditText)view.findViewById(R.id.editText1)).addTextChangedListener(this);
		((EditText)view.findViewById(R.id.editText2)).addTextChangedListener(this);
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle("Set keystore password");
		return view;
	}

    // Methods implemented from TextWatcher interface

    @Override
    public void afterTextChanged(Editable arg0) {

    }

    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                  int arg3) {

    }

    @Override
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        EditText edit1 = (EditText) getView().findViewById(R.id.editText1);
        EditText edit2 = (EditText) getView().findViewById(R.id.editText2);
        TextView text1 = (TextView) getView().findViewById(R.id.textView1);
        TextView text2 = (TextView) getView().findViewById(R.id.textView2);
        Button button = (Button) getView().findViewById(R.id.button1);

        button.setEnabled(checkEditBoxes(edit1,edit2,text1,text2));
    }

    //Fragment specific methods

    /**
     * Checks if edit boxes are fill out correctly, that means text in both of them matches and
     * is longer than four characters
     *
     * @param edit First edit box to get text from
     * @param edit2 Second edit box to get text from
     * @param text first text to display
     * @param text2 second text to display
     * @return True if all conditions were met, false otherwise
     */
	public boolean checkEditBoxes(EditText edit, EditText edit2, TextView text, TextView text2 ){
		
		 Boolean pass = false;
		 Boolean pass2 = false;
		 if(edit.getText().toString().length() < 4){
			 text.setText("password has to be at least 4 characters long");
			 text.setTextColor(Color.RED);
		 }else{
			 text.setText("password is OK");
			 text.setTextColor(Color.GREEN);
			 pass = true;
			 if(!edit.getText().toString().equals(edit2.getText().toString())){
					 text2.setText("passwords don't match");
					 text2.setTextColor(Color.RED);
				 }else{
					 pass2 = true;
					 text2.setText("passwords match");
					 text2.setTextColor(Color.GREEN);
				}
		 }

        return pass && pass2;


    }

    /**
     * Gets filled out password from the edit text field
     *
     * @return String from the edit text box
     */
	public String getPassword(){
		EditText edit = (EditText) getView().findViewById(R.id.editText1);
		return edit.getText().toString();
	}
}
