package com.example.keystoreapp.fragment;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.keystoreapp.EntryRecord;
import com.example.keystoreapp.KeystoreItemAdapter;
import com.example.keystoreapp.R;
import com.example.keystoreapp.utils.ImportExportUtils;
import com.example.keystoreapp.utils.KeyStoreUtils;
import com.example.keystoreapp.utils.Utils;

import org.spongycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

/**
 * This class represents main view of the application, where all the items are listed in a list.
 * All items are represented as EtryRecord objects and handled by KeystoreItemAdapter. It also creates
 * context menu for each item.
 *
 * @author Petr konecny
 *
 */
public class KeystoreListFragment extends ListFragment {


    private static final String TAG = "Keystore list fragment";

	private Callbacks callbacks;
    private ArrayList list;

    public static interface Callbacks{

        //Basic getters and setters

        public String getPassword();
        public KeyStore getStore();
        public void setCurrentAlias(String alias);

        /**
         * Implementation of this method should save keystore into file
         */
        public void saveStore();
        /**
         * Implementation of this method should start signing process
         *
         */
        public void sign();
        /**
         * Implementation of this method should start verification process
         */
        public void verify();
        /**
         * Implementation of this method shoud stard export process for the whole keypair
         *
         * @param alias Alias to be exported
         */
        public void exportKeypair(String alias);

    }
	//Overridden methods from ListFragment

	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
        callbacks = (Callbacks) getActivity();
        Log.i(TAG,"store and password null?"+ Boolean.toString(callbacks.getStore() == null)+" "+Boolean.toString(callbacks.getPassword()== null));
        //if (callbacks.getStore()!= null)store = callbacks.getStore();
        //if (callbacks.getPassword() != null)password = callbacks.getPassword();
		registerForContextMenu(getListView());
    }

    @Override
    public void onStart() {
        super.onStart();
        updateList();
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        list = new ArrayList<EntryRecord>();
        setListAdapter(new KeystoreItemAdapter(getActivity(),android.R.layout.simple_list_item_2,list));
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.keystorelist_fragment, container, false);
        return view;
	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.keystore_menu,menu);
    }

    @Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
	    super.onCreateContextMenu(menu, v, menuInfo);
	    AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
	    MenuInflater inflater = getActivity().getMenuInflater();

        menu.setHeaderTitle("Options");

        EntryRecord record = (EntryRecord) getListAdapter().getItem(info.position);
        if (record.isBoth()){
            inflater.inflate(R.menu.context_both, menu);
        }
        else if (record.getIsCertificate()) {
            inflater.inflate(R.menu.context_certificate, menu);
        }
        else if (record.getIsPrivatekey()) {
            inflater.inflate(R.menu.context_private, menu);
        }


        if(!record.getIsRoot()) inflater.inflate(R.menu.context_delete,menu);

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		String alias = ((EntryRecord)getListAdapter().getItem(info.position)).getAlias();
        callbacks.setCurrentAlias(alias);
		switch (item.getItemId()) {
			case R.id.verify:
                callbacks.verify();
				return true;
			case R.id.sign:
                callbacks.sign();
				return true;
			case R.id.delete:
                try {
                    callbacks.getStore().deleteEntry(alias);
                    callbacks.saveStore();
                    Toast.makeText(getActivity(), "Item deleted", Toast.LENGTH_SHORT).show();
                } catch (KeyStoreException e) {
                    exceptionAlert(e);
                }
                updateList();
	        	return true;
	        
	        case R.id.certificate_details:
	        	showSimpleDialog(Html.fromHtml(Utils.formatCertificateInfo(getCertificate(alias))), "Certificate detail",getActivity());
	        	return true;
            case R.id.key_details:
                showSimpleDialog(Html.fromHtml(Utils.formatKeyInfo((PrivateKey) getKey(alias))), "key detail",getActivity());
                return true;
	        case R.id.export_certificate:
                try {
                    ImportExportUtils.exportAsPem(getCertificate(alias), alias + ".crt",Utils.getAppFolder());
                    Toast.makeText(getActivity(), "Item exported into /Keystoreapp folder in external storage", Toast.LENGTH_SHORT).show();

                } catch (IOException e) {
                    exceptionAlert(e);
                } catch (OperatorCreationException e) {
                    exceptionAlert(e);
                }
                return true;
            case R.id.export_key:
                try {
                    Object o = ImportExportUtils.PrivateKeyToPKCS8((PrivateKey) getKey(alias), callbacks.getPassword());
                    ImportExportUtils.exportAsPem(o, alias + ".pem",Utils.getAppFolder());
                    Toast.makeText(getActivity(), "Item exported into /Keystoreapp folder in external storage", Toast.LENGTH_SHORT).show();

                } catch (OperatorCreationException e) {
                    exceptionAlert(e);
                } catch (IOException e) {
                    exceptionAlert(e);
                }
                return true;
            case R.id.export_both:
                callbacks.exportKeypair(alias);
                return true;
            default:
	            return super.onContextItemSelected(item);
	    }
	}

	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		 l.showContextMenuForChild(v);   
	}
	

		
	
	//Methods specific for this fragment

    /**
     * Updates title in the action bar to reflect number of items in the store
     */
	public void updateActionBar() {
		try {
            ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle((((Integer)callbacks.getStore().size()).toString()));
		} catch (KeyStoreException e) {
			exceptionAlert(e);
		}
		
	}

    /**
     * Gets certificate from the store
     *
     * @param alias Name of the certificate
     * @return X509Certificate from the store, @null if it wasn't there
     */
    public X509Certificate getCertificate(String alias){
        try {
            return (X509Certificate)callbacks.getStore().getCertificate(alias);
        } catch (KeyStoreException e) {
            exceptionAlert(e);
        }
        return null;
    }

    /**
     * Gets key from the store
     *
     * @param alias Name of the key
     * @return Key from the store, @null if it wasn't there
     */
    public Key getKey(String alias){
        try {
            return callbacks.getStore().getKey(alias, null);
        } catch (KeyStoreException e) {
            exceptionAlert(e);
        } catch (UnrecoverableKeyException e) {
            exceptionAlert(e);
        } catch (NoSuchAlgorithmException e) {
            exceptionAlert(e);
        }
        return null;
    }
    /**
     * Displays exception in the new dialog and prints it trace
     *
     * @param e Exception to be displayed
     */
    /**
     * Shows dialog with exception message to the user and prints trace of the exception
     *
     * @param e exception to get message from
     */
    public void exceptionAlert(Exception e) {
        Log.d("keyStore App EXCEPTION: " , e.getMessage());
        e.printStackTrace();
        showSimpleDialog(e.getMessage(),"Exception thrown",getActivity());
    }
    /**
     * Shows simple dialog fragment with message and title
     *
     * @param message message of the dialog
     * @param title title of the dialog
     * @param activity activity for getting FragmentManager
     */
    public void showSimpleDialog(CharSequence message, CharSequence title, FragmentActivity activity){
        DialogFragment dialog = new SimpleDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putCharSequence("message", message);
        bundle.putCharSequence("title", title);
        dialog.setArguments(bundle);
        dialog.show(activity.getSupportFragmentManager(),"DIALOG");
    }

    /**
     * Clears the list and updates it with data from the store file, notifies adapter to process the list
     * again
     */
	public void updateList ()  {
		Log.i(TAG,"store is null:" + Boolean.toString(callbacks.getStore() == null) );
        list.clear();
		try {
            EntryRecord item;
            for(String string : KeyStoreUtils.showStore(callbacks.getStore())) {
                item = new EntryRecord();
                item.setAlias(string);
                fillOutEntry(string, item);
                list.add(item);
            }
		} catch (KeyStoreException e) {
			exceptionAlert(e);
		}
		updateActionBar();
        ((ArrayAdapter)getListAdapter()).notifyDataSetChanged();
	}

    /**
     * Adds information about item from the store to the EntryRecord instance
     *
     * @param alias Name of the item in the store
     * @param record Record to be filled out
     *
     */
    public void fillOutEntry(String alias,EntryRecord record){
        try {
            if(alias.equals("root_key") || alias.equals("root_certificate")) record.setIsRoot(true);
            if(callbacks.getStore().isKeyEntry(alias)) {
                record.setIsPrivatekey(true);
                record.setDescription("private key");
                if(callbacks.getStore().getCertificate(alias) != null){
                    record.setDescription("private key with certificate");
                    record.setIsCertificate(true);
                }
            }
            else if(callbacks.getStore().isCertificateEntry(alias)) {
                record.setIsCertificate(true);
                record.setDescription("certificate");
            }

        } catch (KeyStoreException e) {
            exceptionAlert(e);
        }
    }

}