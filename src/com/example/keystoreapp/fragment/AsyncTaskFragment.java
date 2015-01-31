package com.example.keystoreapp.fragment;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;

import com.example.keystoreapp.KeystoreAppException;
import com.example.keystoreapp.SecurityConstants;
import com.example.keystoreapp.utils.ImportExportUtils;
import com.example.keystoreapp.utils.KeyStoreUtils;
import com.example.keystoreapp.utils.Utils;

import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.pkcs.PKCSException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

/**
 * This class represents fragment wrapper around AsyncTask class. It is necessary to wrap async task
 * in fragment or class for interactions with MainActivity and other fragments throuh it.
 *
 * @author Petr konecny
 *
 */
public class AsyncTaskFragment extends Fragment {
	

    private static final String TAG = "asynctaskFragment";

    //Attributes necessary for proper function of async task
	private OperationsAsyncTask mTask;
    private Exception ex;
    private String password;
    private int which;
    private byte[] signature;
    private X500Name subject;
	private AsyncFragmentCallbacks callbacks;

    public static interface AsyncFragmentCallbacks {
        // Basic getters and setters
        String getPassword();
        KeyStore getStore();
        File getFile();
        String getAlias();
        /**
         * Implementation of this method should do code usually done in preExecute method of the asynctask
         *
         * @param which Code of the action currently processed
         */
        void onPreExecute(int which);
        /**
         * Implementation of this method should handle actions depending on progress update, like
         * showing progress in progress dialog etc.
         *
         * @param percent Number describing current progress state
         * @oaran time Estimated time remaining
         */
        void onProgressUpdate(Integer percent, long time);
        /**
         * Implementation of this method should handle results of the async task processing
         *
         * @param which Code of action that has been processed
         * @oaran ex Exception that has been thrown during processing or null if nothing was thrown
         * @param o Object returned from async task
         * @param time Total time that the operation was running
         * @param fragment Fragment that should be removed after processing
         */
        void onPostExecute(int which, Exception ex, Object o, long time,Fragment fragment);
        /**
         * Implementation of this method should handle state, when the async task was canceled by user
         *
         * @param fragment Fragment that should be removed after processing
         */
        void onCanceled(Fragment fragment);
    }

    //Basic getters and setters

    public void setPassword(String password){
        this.password = password;
    }

    public String getPassword() {
        return callbacks.getPassword();
    }

    public File getFile() {
        return callbacks.getFile();
    }

    public void setSubject(X500Name subject) {
        this.subject = subject;
    }

    public void setWhich(int which) {
        this.which = which;
    }

    public void setSignature(byte[] signature){
        this.signature = signature;
    }


    /**
     * This method gets private key from the store in MainActivity with Alias in MainActivity
     *
     * @return PrivaeKey from the store
     */
    public PrivateKey getKey() {
        try {
            return (PrivateKey) callbacks.getStore().getKey(callbacks.getAlias(),null);
        } catch (KeyStoreException e) {
            ex = e;
        } catch (NoSuchAlgorithmException e) {
            ex = e;
        } catch (UnrecoverableKeyException e) {
            ex = e;
        }
        return null;
    }

    /**
     * This method gets certificate from the store in MainActivity with Alias in MainActivity
     *
     * @return X509Certificate from the store
     */
    public X509Certificate getCertificate(){
        try {
            return (X509Certificate) callbacks.getStore().getCertificate(callbacks.getAlias());
        } catch (KeyStoreException e) {
            ex = e;
        }
        return null;
    }

    //Methods overridden from the Fragment class

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        callbacks = (AsyncFragmentCallbacks) getActivity();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mTask = new OperationsAsyncTask();
        mTask.execute();
    }

    //Methods specific for this fragment

    /**
     * This method cancels async task process
     *
     */

    public void cancel(){
        mTask.cancel(false);
    }

    /**
     * This method writes signature file to the same folder as the original file, with .sgn extension
     *
     * @param b Byte array representing signature
     */
    public void writeSignatureFile(byte[] b){
        if(b == null) return;
        File fileDirectory = getFile().getAbsoluteFile().getParentFile();
        File outputFile = new File(fileDirectory,getFile().getName()+".sgn");
        try {
            Utils.saveData(b, getFile().getName() + ".sgn", getActivity(), outputFile);
        } catch (IOException e) {
            ex = e;
        }
    }

    /**
     * This inner class represents AsyncTask. It does all the hard work in application, becaouse it
     * runs on different thread and therefore doesn't freeze UI. This could be done by a Service too,
     * but because all of it's operations should be done in relatively short amount of time service would only slow them down.
     *
     * @author Petr konecny
     *
     */
    private class OperationsAsyncTask extends AsyncTask<Void,Integer,Object> {
        private long timer;
        private long estimate;
        private Integer total;

        //Methods overridden from the AsyncTask generic class

        @Override
        protected void onProgressUpdate(Integer... progress) {

            if (progress[0].equals(total)) {
                return;
            }
            if(progress[0] == 1){
                estimate = System.nanoTime();
            }
            if(progress[0] == 2){
                estimate = System.nanoTime() - estimate;
            }

            total = progress[0];
            long current = estimate*(100-progress[0]);
            callbacks.onProgressUpdate(progress[0], current);
        }

        @Override
        protected void onPreExecute() {
            callbacks.onPreExecute(which);
        }

        @Override
        protected Object doInBackground(Void... files) {
            timer  = System.nanoTime();
            switch(which) {
                case SecurityConstants.SIGN :
                    writeSignatureFile(sign(getKey()));
                    return null;
                case SecurityConstants.VERIFY:
                    boolean bol = verify();
                    if(bol) which = SecurityConstants.MATCH;
                    else which = SecurityConstants.MISSMATCH;
                    return null;
                case SecurityConstants.GENERATE:
                    generate(false);
                    return null;
                case SecurityConstants.GENERATE_ROOT:
                    generate(true);
                    return null;
                case SecurityConstants.IMPORT:
                    try {
                        return ImportExportUtils.convertPemObject(getFile());
                    } catch (NoSuchAlgorithmException e) {
                        ex = e;
                    } catch (InvalidKeySpecException e) {
                        ex = e;
                    } catch (NoSuchProviderException e) {
                        ex = e;
                    } catch (IOException e) {
                        ex = e;
                    } catch (PKCSException e) {
                        ex = e;
                    }
                    return null;
                case SecurityConstants.IMPORT_STORE:
                    try {
                        ImportExportUtils.importPKCS12File(getFile(),password,callbacks.getStore());
                        return null;
                    } catch (NoSuchProviderException e) {
                        ex = e;
                    } catch (KeyStoreException e) {
                        ex = e;
                    } catch (IOException e) {
                        ex = e;
                    } catch (CertificateException e) {
                        ex = e;
                    } catch (NoSuchAlgorithmException e) {
                        ex = e;
                    } catch (UnrecoverableKeyException e) {
                        ex = e;
                    }
                    return null;
            }

            return null;
            }

        @Override
        protected void onPostExecute (Object o) {
            super.onPostExecute(o);
            timer = System.nanoTime() - timer;
            callbacks.onPostExecute(which,ex,o,timer,AsyncTaskFragment.this);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            callbacks.onCanceled(AsyncTaskFragment.this);
        }

        //Class specific methods

        /**
         * This method creates Signature object, that is used for signing or verification
         *
         * @param o Object, for which the Signature is created, PrivateKey or X509Certificate
         * @return Signature used for signing or verification
         *
         */
        public Signature getSignature(Object o) {
            BufferedInputStream bis = null;
            Signature s = null;
            long total = 0;
            int sizeRead = -1;

            //Creates buffered stream for reading the file, using file in the MainActivity
            try {
                bis = new BufferedInputStream(new FileInputStream(getFile()));
                byte[] buffer = new byte[8192];
                long length = getFile().length();

                //Gets prefered hash value from the preferences
                SharedPreferences sharedPref = PreferenceManager
                        .getDefaultSharedPreferences(getActivity());
                String hash = sharedPref.getString("pref_hash","SHA-1");

                if (o instanceof PrivateKey){
                    //Creates signature object used for signing
                    s = Signature.getInstance(KeyStoreUtils.getSigAlg(getKey().getAlgorithm(),hash),
                            sharedPref.getString("pref_security_provider",null));
                    s.initSign((PrivateKey)o);
                }
                if (o instanceof X509Certificate) {
                    //Creates signature object used for verification
                    s = Signature.getInstance(KeyStoreUtils.getSigAlg(getCertificate().getPublicKey()
                            .getAlgorithm(),hash),sharedPref.getString("pref_security_provider"
                            ,null));
                    s.initVerify((X509Certificate)o);
                }

                // Reads the file into this object, using buffer defined earlier
                while ((sizeRead = bis.read(buffer)) != -1) {
                    total = total + sizeRead;
                    //Calculates progress of the operationd and sends it
                    publishProgress((int) ((total * 100) / length));
                    //method update is used to create hash of the file, filing it with content of the buffer
                    //buffer is necessary, else the file size is limited
                    s.update(buffer, 0, sizeRead);
                    if (isCancelled()) {
                        //if the process was canceled closes stream and aborts process
                        bis.close();
                        return null;
                    }
                }
                return s;
            }catch (NoSuchAlgorithmException e) {
                ex = e;
            } catch (SignatureException e) {
                ex = e;
            } catch (NoSuchProviderException e) {
                ex = e;
            } catch (InvalidKeyException e) {
                ex = e;
            } catch (IOException e) {
                ex = e;
            } finally{
                try {
                    if (bis != null) bis.close();
                } catch (IOException e) {
                    ex = e;
                }
            }
            return null;
        }

        /**
         * This method signs the file, using Signature object previously created
         *
         * @return Byte array representing signature or @null
         */
        public byte[] sign(PrivateKey key){

            try {
                Signature s = getSignature(key);
                if (s == null) return null;
                return s.sign();
            } catch (SignatureException e) {
                ex = e;
            }
            return null;

        }

        /**
         * This method verifies the file, using Signature object previously created
         *
         * @return True if matches, False otherwise
         */
        public boolean verify(){

            try {
                Signature s = getSignature(getCertificate());
                if (s == null) return false;
                return s.verify(signature);
            } catch (SignatureException e) {
                ex=e;
            }
            return false;
        }

        /**
         * This method generates new certificate and its private key
         *
         * @boolean root Determines if the certificate is root or not
         */
        public void generate(boolean root){
            String aliasCert = null;
            String aliasKey = null;
            PrivateKey toSign = null;
            X509Certificate rootCert = null;
            X509Certificate cert;

            try {
                // Gets various preferences

                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String algorithm = sharedPref.getString("pref_algorithm",null);
                String hash = sharedPref.getString("pref_hash",null);
                String provider = sharedPref.getString("pref_security_provider", null);
                Integer serial = sharedPref.getInt("serialNumber", 1);
                Integer size = Integer.parseInt(sharedPref.getString("key_size_values", "1024"));
                boolean ca = sharedPref.getBoolean("IsCACertificate", false);
                X500Name issuer = Utils.setNameInfo(sharedPref.getString("IssuerCommonName", null),
                        sharedPref.getString("IssuerOrganization", null), sharedPref.getString("IssuerOrganizationUnit", null));
                KeyPair pair = KeyStoreUtils.generateKeyPair(size, algorithm, provider);

                if(root){
                    //Root has fixed name and it is always created with CA certificate
                    aliasKey = "root_key";
                    ca = true;
                    toSign = pair.getPrivate();
                }else{
                    try {
                        //If it is not root, root key is retrieved for signing the certificate and root certificate is
                        //retrieved to add to the certificate Chain of the new key
                        toSign = (PrivateKey) callbacks.getStore().getKey("root_key",null);
                        rootCert = (X509Certificate) callbacks.getStore().getCertificate("root_key");
                        aliasKey = KeyStoreUtils.createDefAliasKey(serial, algorithm);
                    } catch (UnrecoverableKeyException e) {
                        ex = e;
                    }

                }

                //Creates new certificate, saves is, saves the whole store file and increments serial number counter
                cert = KeyStoreUtils.createCertificate(hash,toSign,pair.getPublic(),provider,issuer,subject,ca,serial);
                KeyStoreUtils.saveKeyPair(rootCert,cert,callbacks.getStore(),pair, null,aliasKey);
                KeyStoreUtils.saveStore(callbacks.getStore(), getActivity(), callbacks.getPassword(), SecurityConstants.KEYSTORE_NAME);
                sharedPref.edit().putInt("serialNumber", serial+1).apply();

            } catch (KeyStoreException e) {
                ex = e;
            } catch (OperatorCreationException e) {
                ex = e;
            } catch (CertificateException e) {
                ex = e;
            } catch (NoSuchAlgorithmException e) {
                ex = e;
            } catch (IOException e) {
                ex = e;
            } catch (KeystoreAppException e) {
                ex = e;
            } catch (NoSuchProviderException e) {
                ex = e;
            }
        }
    }
}
