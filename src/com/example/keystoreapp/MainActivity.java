package com.example.keystoreapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.keystoreapp.fragment.AsyncTaskFragment;
import com.example.keystoreapp.fragment.KeystoreListFragment;
import com.example.keystoreapp.fragment.PasswordDialogFragment;
import com.example.keystoreapp.fragment.ProgressDialogFragment;
import com.example.keystoreapp.fragment.ResetDialogFragment;
import com.example.keystoreapp.fragment.RetainedFragment;
import com.example.keystoreapp.fragment.SetupFragment;
import com.example.keystoreapp.fragment.SimpleDialogFragment;
import com.example.keystoreapp.fragment.SubjectDialogFragment;
import com.example.keystoreapp.utils.ImportExportUtils;
import com.example.keystoreapp.utils.KeyStoreUtils;
import com.example.keystoreapp.utils.Utils;

import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.openssl.PEMException;
import org.spongycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.spongycastle.pkcs.PKCSException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.TimeUnit;

/**
 * This class represents the main activity of the application. It is doing or facilitating most of the processing and
 * provides communication channels between application fragments through interfaces. It is designed this way
 * because fragments shouldn't reference or interact with each other directly. It also handles clicks on menu items in
 * different fragments and is designed in a way that most of the data is preserved after it's recreation.
 * Fragment-heavy design was chosen because all tasks basicaly need the same set of attributes. It isn't possible to pass
 * some of them in Intent to another activity and even if it was, it would result in very chaotic and hard to read code.
 *
 * @author Petr Konecny
 *
*/
public class MainActivity extends ActionBarActivity implements AsyncTaskFragment.AsyncFragmentCallbacks,
ProgressDialogFragment.DialogCallbacks, KeystoreListFragment.Callbacks,
PasswordDialogFragment.Callbacks, SetupFragment.Callbacks, ResetDialogFragment.Callbacks, SubjectDialogFragment.Callbacks,
PopupMenu.OnMenuItemClickListener {

    //Attributes that generally work as activity-wide storage and are accessed through implemented interfaces
    private String currentAlias;
    private File currentFile;
	private String password;
	private KeyStore store;
	private ProgressDialogFragment fragment;
    private Integer returningWithResult;
    private Boolean showDialogs;

    // Tag used in logs
    private static final String TAG = "Main activity";



    // Overridden methods from ActionBarActivity class

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        //adds spongycastle as security provider
	    Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        setContentView(R.layout.activity_main);
	    if (savedInstanceState == null) {
		    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
	        Fragment fragment;
	        if(KeyStoreUtils.checkStore(this)){
	        	if(password == null ) {
	        		DialogFragment dialog = new PasswordDialogFragment();
	        		Bundle bundle = new Bundle();
	        		bundle.putInt("which", SecurityConstants.UNLOCK); 
	        		dialog.setArguments(bundle);
	        		dialog.show(getSupportFragmentManager(), "PASSWORD");
	        	}else{
	        		fragment = new KeystoreListFragment();
	    	        transaction.add(R.id.fragment_container, fragment, "KEYSTORE").commit();
	        	}
	        }else{
                //is Called if keystore file doesn't exist so it needs to be recreated
	        	fragment = new SetupFragment();
    	        transaction.add(R.id.fragment_container, fragment).commit();
	        }
	    }else{
            //is Called if application was closed and opened again
            Log.i(TAG,"reconstructing activity");
			fragment = (ProgressDialogFragment) getSupportFragmentManager()
                    .findFragmentByTag("PROGRESS");
            RetainedFragment retained = (RetainedFragment)getSupportFragmentManager()
                    .findFragmentByTag("RETAINED");
            store = retained.getStore();
            currentFile = retained.getFile();
            password = savedInstanceState.getString("PASSWORD");
            if(store == null) {
                Log.i(TAG, "Retained fragment not found, trying to reload keystore");
                if(password != null) store = loadStore(password);
            }
        }
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        Intent intent;
        switch (item.getItemId()) {
            case R.id.clear_keystore:
                try {
                    setStore(KeyStoreUtils.createKeyStore());
                    saveStore();

                } catch (NoSuchAlgorithmException e) {
                    exceptionAlert(e);
                } catch (CertificateException e) {
                    exceptionAlert(e);
                } catch (KeyStoreException e) {
                    exceptionAlert(e);
                } catch (NoSuchProviderException e) {
                    exceptionAlert(e);
                } catch (IOException e) {
                    exceptionAlert(e);
                }
                updateListFragment();
                Toast.makeText(this, SecurityConstants.CLEARED, Toast.LENGTH_SHORT).show();
                return true;
            case R.id.reset:
                DialogFragment resetDialog = new ResetDialogFragment();
                resetDialog.show(getSupportFragmentManager(),"DIALOG");
                return true;

            case R.id.add:
                View view = findViewById(R.id.add);
                showPopup(view);
                return true;

            case R.id.settings:
                intent = new Intent(this,SettingsActivity.class);
                startActivity(intent);
                return true;

            case R.id.import_file:
                startActivityForResult(getFileIntent(), SecurityConstants.IMPORT);
                return true;
            case R.id.help:
                showSimpleDialog(Html.fromHtml(Utils.getHelp()),"Help",this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPostResume() {
        /* Because this method is called every time after onActivityResult, it is a good place to handle
        results from another activities. Standard implementation in onActivityResult() creates
        compatibility errors with Fragment support package.
        */
        super.onPostResume();
        if (returningWithResult == null) return;
        Log.i(TAG,returningWithResult.toString());
        AsyncTaskFragment fragment = new AsyncTaskFragment();

        switch (returningWithResult) {
            case SecurityConstants.IMPORT:

                fragment.setWhich(SecurityConstants.IMPORT);
                break;
            case SecurityConstants.IMPORT_STORE:
                PasswordDialogFragment dialog = new PasswordDialogFragment();
                Bundle bundle = new Bundle();
                bundle.putInt("which",SecurityConstants.IMPORT_STORE);
                dialog.setArguments(bundle);
                dialog.show(getSupportFragmentManager(),"PASSWORD");
                returningWithResult = null;
                return;
            case SecurityConstants.SIGN:

                fragment.setWhich(SecurityConstants.SIGN);

                break;

            case SecurityConstants.VERIFY:

                try {
                    File fileDirectory = currentFile.getAbsoluteFile().getParentFile();
                    File inputFile = new File (fileDirectory, currentFile.getName() + ".sgn");
                    fragment.setSignature(Utils.loadFile(inputFile));
                    fragment.setWhich(SecurityConstants.VERIFY);
                } catch (IOException e) {
                    exceptionAlert(e);
                }
                break;
        }
        getSupportFragmentManager().beginTransaction().add(fragment, "ASYNC").commitAllowingStateLoss();
        //flag has to be returned to null after processing
        returningWithResult = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        RetainedFragment fragment = (RetainedFragment) getSupportFragmentManager().findFragmentByTag("RETAINED");

        //try to find retained fragment, if it doesn't exist, create a new one
        if(fragment  == null) {
            fragment = new RetainedFragment();
            fragment.setStore(store);
            fragment.setFile(currentFile);
            getSupportFragmentManager().beginTransaction().add(fragment,"RETAINED").commit();
        }else{
            fragment.setStore(store);
            fragment.setFile(currentFile);
        }

        outState.putString("PASSWORD", password);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        showDialogs = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        showDialogs = true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            final Uri uri = data.getData();
            currentFile = Utils.getFile(uri);

            //just sets flag for further processing in onPostResume(), processing isn't happening here
            // because it creates compatibility issues with support library
            returningWithResult = requestCode;
        }
    }

    // Method implemented from OnMenuItemClickListener for handling clicks from the popup menu
    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.generate_new:
                try {
                    if (store.getKey("root_key",null) == null){
                        generateRoot();
                        return true;
                    }
                } catch (KeyStoreException e) {
                    e.printStackTrace();
                } catch (UnrecoverableKeyException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                SubjectDialogFragment dialog = new SubjectDialogFragment();
                dialog.show(getSupportFragmentManager(), "DIALOG");
                return true;
            case R.id.import_file:
                Intent getContentIntent = Utils.createGetContentIntent();
                Intent intent = Intent.createChooser(getContentIntent, "Select a file chooser");
                startActivityForResult(intent, SecurityConstants.IMPORT);
                return true;
            case R.id.import_store:
                Intent getContentIntent2 = Utils.createGetContentIntent();
                Intent intent2 = Intent.createChooser(getContentIntent2, "Select a file chooser");
                startActivityForResult(intent2, SecurityConstants.IMPORT_STORE);
                return true;
            default:
                return false;
        }
    }
    // FRAGMENT CALLBACKS IMPLEMENTED METHODS

    //AsyncTaskFragment Callbacks implementation


    @Override
    public void onPreExecute(int which) {
        // Starts new async task fragment for background processing
        fragment = new ProgressDialogFragment();
        fragment.setWhich(which);
        fragment.show(getSupportFragmentManager(), "PROGRESS");
    }

    @Override
    public void onProgressUpdate(Integer percent,long time) {
        // Updates progres dialog fragment
        fragment.updateTime (time);
        fragment.update(percent);
    }


    @Override
    public void onPostExecute(int which, Exception ex, Object o, long time,Fragment asyncFragment) {
        String message = null;
        String string = null;
        double fileLength = 0;
        double seconds = 0;
        if(ex == null) {
            if(which == SecurityConstants.SIGN || which == SecurityConstants.MATCH || which == SecurityConstants.MISSMATCH){
                fileLength = Math.round(currentFile.length() / 10.24) / 100.0;
                seconds = TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS);
                seconds /= 1000;
                string = "<b>File name: </b>" + currentFile.getName() + "<br><br>" + "<b>File size: </b>" + fileLength + " Kb<br>" +
                        "<br>" + "Operation finished in " + seconds + " seconds ";
            }
            switch (which) {
                case SecurityConstants.SIGN:
                    showSimpleDialog(Html.fromHtml(string), "File signed", this);
                    message = "File signed";
                    break;
                case SecurityConstants.MATCH:
                    showSimpleDialog(Html.fromHtml(string), "Signature matches", this);
                    message = "Signature matches private key";
                    break;
                case SecurityConstants.MISSMATCH:
                    showSimpleDialog(Html.fromHtml(string), "Signature doesn't match", this);
                    message = "Signature doesn't match private key";
                    break;
                case SecurityConstants.IMPORT:
                    recognizeImport(o);
                    message = null;
                    break;
                case SecurityConstants.IMPORT_STORE:
                    saveStore();
                    message = "Import complete";
                    break;
                case SecurityConstants.GENERATE_ROOT:
                    SubjectDialogFragment dialog = new SubjectDialogFragment();
                    dialog.show(getSupportFragmentManager(), "DIALOG");
                    message = "Root generated";
                    break;
                case SecurityConstants.GENERATE:
                    message = "Keypair generated";
                    break;
            }
        }else{
            exceptionAlert(ex);
            message = "Exception thrown";
        }

        updateListFragment();
        fragment.dismiss();
        if(message != null)
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
        //Retained fragments with over-ridden onCreate method have to be removed from the backstack, or they will
        //recreate when activity is restored and cause errors
        getSupportFragmentManager().beginTransaction().remove(asyncFragment).commitAllowingStateLoss();
    }

    public void onCanceled(Fragment asyncFragment){
        getSupportFragmentManager().beginTransaction().remove(asyncFragment).commit();
        Toast.makeText(this,"Operation canceled",Toast.LENGTH_SHORT).show();
    }

    //KeystoreListFragment callbacks implementation

    @Override
    public void sign() {
        startActivityForResult(getFileIntent(), SecurityConstants.SIGN );
    }

    @Override
    public void verify() {
        startActivityForResult(getFileIntent(), SecurityConstants.VERIFY );
    }

    @Override
    public void setCurrentAlias(String alias) {
        //sets current alias of the processed item, so other fragments can access it
        this.currentAlias = alias;
    }

    //PasswordDialogFragment callbacks implementation

    @Override
    public void unlockStore(String password,DialogFragment dialog) {
        store = loadStore(password);
        this.password = password;
        if (store == null) return;
        dialog.dismiss();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new KeystoreListFragment(), "KEYSTORE").commit();
    }

    public void importKey(char[] password,DialogFragment dialog) {
        dialog.dismiss();
        importPrivateKey(password);
    }

    public void importStore(String password, DialogFragment dialog) {
        AsyncTaskFragment fragment = new AsyncTaskFragment();
        fragment.setWhich(SecurityConstants.IMPORT_STORE);
        fragment.setPassword(password);
        dialog.dismiss();
        getSupportFragmentManager().beginTransaction().add(fragment,"ASYNC").commitAllowingStateLoss();
    }

    //ProgressDialogFragment callbacks implementation

    @Override
    public void cancel() {
        AsyncTaskFragment fragment = (AsyncTaskFragment) getSupportFragmentManager().findFragmentByTag("ASYNC");
        fragment.cancel();
    }

    //ResetDialogFragment callbacks implementation

    @Override
    public void resetKeystore() {
        KeyStoreUtils.deleteStore(this);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SetupFragment()).commit();
    }

    //SetupDialogFragment callbacks implementation

    @Override
    public void createStore(String password) {
        try {
            KeyStoreUtils.saveStore(KeyStoreUtils.createKeyStore(),this,password,SecurityConstants.KEYSTORE_NAME);
            store = loadStore(password);
            this.password = password;
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new KeystoreListFragment(),"KEYSTORE").commit();

        } catch (KeyStoreException e) {
            exceptionAlert(e);
        } catch (NoSuchAlgorithmException e) {
            exceptionAlert(e);
        } catch (CertificateException e) {
            exceptionAlert(e);
        } catch (NoSuchProviderException e) {
            exceptionAlert(e);
        } catch (IOException e) {
            exceptionAlert(e);
        } catch (KeystoreAppException e) {
            exceptionAlert(e);        }
    }

    //SubjectDialogFragment callbacks implementation

    @Override
    public void generateCertificate(String commonName, String organization, String organizationUnit,int which) {
        AsyncTaskFragment fragment = new AsyncTaskFragment();
        fragment.setWhich(which);
        fragment.setSubject(Utils.setNameInfo(commonName, organization, organizationUnit));
        getSupportFragmentManager().beginTransaction().add(fragment, "ASYNC").commit();
    }

    public void setStore(KeyStore store){
        this.store = store;
    }

    public Intent getFileIntent(){
        Intent getContentIntent = Utils.createGetContentIntent();
        Intent intent = Intent.createChooser(getContentIntent, "Select app to choose file");
        return intent;
    }

    //Methods shared in multiple interfaces

    @Override
    public String getPassword() {
		return password;
	}

	@Override
	public KeyStore getStore() {
        return store;
	}

    @Override
    public File getFile(){
        return currentFile;
    }
    @Override
    public String getAlias(){
        return currentAlias;
    }

    //Activity-specific methods

    /**
     * This method wraps KeystoreUtils.loadStore method for displaying proper error dialogs
     *
     * @param password keystore password
     * @return Loaded keystore
     */

    private KeyStore loadStore(String password) {
		try {
			return KeyStoreUtils.loadStore(this,password.toCharArray(),SecurityConstants.KEYSTORE_NAME);
		} catch (NoSuchAlgorithmException e) {
            exceptionAlert(e);
        } catch (CertificateException e) {
            exceptionAlert(e);
        } catch (KeyStoreException e) {
            exceptionAlert(e);
        } catch (IOException e) {
			Utils.showSimpleDialog("Wrong password, please try again", "Wrong password", this);
		} catch (NoSuchProviderException e) {
            exceptionAlert(e);

        }
		return null;
	}

    /**
     * This method wraps KeystoreUtils.saveStore method for displaying proper error dialogs
     */
    public void saveStore(){
        try {
            KeyStoreUtils.saveStore(getStore(), this, getPassword(), SecurityConstants.KEYSTORE_NAME);
        } catch (KeyStoreException e) {
            exceptionAlert(e);
        } catch (NoSuchAlgorithmException e) {
            exceptionAlert(e);
        } catch (CertificateException e) {
            exceptionAlert(e);
        } catch (IOException e) {
            exceptionAlert(e);
        } catch (KeystoreAppException e) {
            exceptionAlert(e);
        }
    }
    /**
     * This method recognizes if the object passed to it is enrypted private key, unecrypted private key,
     * X.509 Certificate or something else and displays appropriate UI element.
     *
     * @param object Object to process
     */

    public void recognizeImport(Object object){
        if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
            DialogFragment fragment = new PasswordDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("which", SecurityConstants.IMPORT_KEY);
            fragment.setArguments(bundle);
            fragment.show(getSupportFragmentManager(), "DIALOG");
        }else if(object instanceof PrivateKeyInfo){
            importPrivateKey(null);
        }else if(object instanceof X509CertificateHolder){
            try {
                Certificate cert = ImportExportUtils.importCertificate(currentFile);
                getStore().setCertificateEntry(currentFile.getName(), cert);
                saveStore();
                updateListFragment();
                Toast.makeText(this,"Import complete",Toast.LENGTH_SHORT).show();

            } catch (CertificateException e) {
                exceptionAlert(e);
            } catch (FileNotFoundException e) {
                exceptionAlert(e);
            } catch (KeyStoreException e) {
                exceptionAlert(e);
            }
        }else{
            try {
                throw new KeystoreAppException("File is not a supported certificate or private key");
            } catch (KeystoreAppException e) {
                exceptionAlert(e);
                ;
            }
        }
    }

    /**
     * This method imports private key to the store, if it doesn't find matching certificate to this key,
     * dialog is shown and the import process is aborted
     *
     * @param password password for the key being imported
     */

    public void importPrivateKey(char[] password) {
        Certificate certChain [] = new Certificate [1];
        Certificate cert;
        try{
            Key key  = ImportExportUtils.getPrivateKeyFromPem(ImportExportUtils.convertPemObject(currentFile), password);
            if ((cert = Utils.findMatching((PrivateKey) key, getStore()))!= null){
                certChain [0] = cert;
            }else{
                showSimpleDialog("No certificate matching this key found, please import matching certificate to the store", "No match found", this);
                return;
            }
            getStore().setKeyEntry(currentFile.getName(), key, null, certChain);
            saveStore();
            updateListFragment();
            Toast.makeText(this,"Import complete",Toast.LENGTH_SHORT).show();

        } catch (InvalidKeyException e) {
            exceptionAlert(e);
        } catch (NoSuchAlgorithmException e) {
            exceptionAlert(e);
        } catch (NoSuchProviderException e) {
            exceptionAlert(e);
        } catch (SignatureException e) {
            exceptionAlert(e);
        } catch (KeyStoreException e) {
            exceptionAlert(e);
        } catch (PEMException e) {
            exceptionAlert(e);
        } catch (InvalidKeySpecException e) {
            exceptionAlert(e);
        } catch (PKCSException e) {
            exceptionAlert(e);
        } catch (IOException e) {
            exceptionAlert(e);
        }
    }

    /**
     * This method generates root keypair, that is used for signing all other keys, that way you need to just import
     * root and every other certificate in the store is trusted by that app
     */

    public void generateRoot(){

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        generateCertificate(sharedPref.getString("IssuerCommonName", null),sharedPref.getString("IssuerOrganization", null),
                sharedPref.getString("IssuerOrganizationUnit", null),SecurityConstants.GENERATE_ROOT);
    }

    /**
     * Shows simple popup menu
     * @param v view to show popup menu on
     */
    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.popup);
        popup.show();
    }

    /**
     * Forces refresh of the list in KeystoreListFragment, if it exists
     */
    public void updateListFragment(){
        KeystoreListFragment fragment = (KeystoreListFragment) getSupportFragmentManager().findFragmentByTag("KEYSTORE");
        if(fragment == null) return;
        fragment.updateList();
    }


    /**
     * Shows dialog with exception message to the user and prints trace of the exception
     *
     * @param e exception to get message from
     */
    public void exceptionAlert(Exception e) {
        Log.d("keyStore App EXCEPTION: " , e.getMessage());
        e.printStackTrace();
        showSimpleDialog(e.getMessage(),"Exception thrown",this);
    }
    /**
     * Shows simple dialog fragment with message and title
     *
     * @param message message of the dialog
     * @param title title of the dialog
     * @param activity activity for getting FragmentManager
     */
    public void showSimpleDialog(CharSequence message, CharSequence title, FragmentActivity activity){
        if (!showDialogs) return;
        DialogFragment dialog = new SimpleDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putCharSequence("message", message);
        bundle.putCharSequence("title", title);
        dialog.setArguments(bundle);
        dialog.show(activity.getSupportFragmentManager(),"DIALOG");
    }

    /**
     * Wrapper for KeystoreUtils exportKezPairAsPKCS12 method
     *
     * @param alias of item to export
     *
     */
    public void exportKeypair(String alias){
        try {
            Certificate[] certChain =  store.getCertificateChain(alias);
            PrivateKey key = (PrivateKey) store.getKey(alias,null);
            Log.i(TAG,"Certificate chain length :"+ Integer.toString(certChain.length));
            ImportExportUtils.exportKeyPairAsPKCS12(key, certChain, alias, Utils.getAppFolder(), alias + ".pfx", password.toCharArray());
            Toast.makeText(this, "Item exported into /Keystoreapp folder in external storage", Toast.LENGTH_SHORT).show();

        } catch (KeyStoreException e) {
            exceptionAlert(e);
        } catch (NoSuchAlgorithmException e) {
            exceptionAlert(e);
        } catch (UnrecoverableKeyException e) {
            exceptionAlert(e);
        } catch (CertificateException e) {
            exceptionAlert(e);
        } catch (NoSuchProviderException e) {
            exceptionAlert(e);
        } catch (IOException e) {
            exceptionAlert(e);
        }

    }

}
