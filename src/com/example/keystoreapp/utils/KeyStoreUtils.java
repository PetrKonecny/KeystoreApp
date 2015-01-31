package com.example.keystoreapp.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.example.keystoreapp.KeystoreAppException;
import com.example.keystoreapp.SecurityConstants;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.BasicConstraints;
import org.spongycastle.cert.CertIOException;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * This class contains lower-level static methods for operations on the Keystore object and handles creation
 * of the new certificate.
 *
 * @author Petr Konecny
 */

public final class KeyStoreUtils {

    /**
     * This method creates new keystore of predefined type
     *
     * @return empty Keystore instance
     */
	public static KeyStore createKeyStore() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException, NoSuchProviderException {

		KeyStore store = KeyStore.getInstance(SecurityConstants.KEYSTORE_UBER,SecurityConstants.PROVIDER_BC);
		store.load(null, null);
		return store;
	}

    /**
     * This method generates KeyPair composed of private and public key
     *
     * @param length Length of generated private key, should be between 512-2048 and multiple of 512
     * @param algorithm Algorithm used for generation
     * @param provider JCA security provider used for generation
     * @return KeyPair instance with private and public key
     */
	public static KeyPair generateKeyPair ( Integer length, String algorithm, String provider) throws NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, KeystoreAppException {

        if(length > 1024 && provider.equals("BC") && algorithm.equals("DSA"))
            throw new KeystoreAppException("bouncy castle supports only 512 or 1024 DSA key strength");
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm,provider);
		keyGen.initialize(length);
        return keyGen.generateKeyPair();
		
	}

    /**
     * This metod creates X509 compatible certificate
     *
     * @param toSign Key to sign certificate with
     * @param publicKey Key that is carried in certificate
     * @param provider JCA security provider used for generating signature
     * @param issuer Info about issuer of the certificate
     * @param subject Info about subject of the certificate
     * @param ca Value of basicConstraints extension
     * @param serial Serial number of the certificate
     * @return new X509Certificate
     */
	public static X509Certificate createCertificate (String hash,PrivateKey toSign, PublicKey publicKey, String provider,X500Name issuer, X500Name subject, boolean ca, int serial) throws OperatorCreationException, CertIOException, CertificateException {
		
		Calendar start = new GregorianCalendar();
	    Calendar end = new GregorianCalendar();
	    end.add(1, Calendar.YEAR);

        ContentSigner sigGen = new JcaContentSignerBuilder(hash +"with" + toSign.getAlgorithm())
                .setProvider(provider).build(toSign);
        X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(
				issuer,
				new BigInteger(String.valueOf(serial)),
		        start.getTime(), end.getTime(), 
		        subject, 
		        publicKey);
		certGen.addExtension(new ASN1ObjectIdentifier("2.5.29.19"), false,
                new BasicConstraints(ca));
		X509CertificateHolder certHolder = certGen.build(sigGen);

		JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        return converter.getCertificate(certHolder);
		
	}

	public static String getSigAlg(String algorithm,String hash){
		
		return hash + "with" + algorithm;
	}

    /**
     * This method adds private key with public key certificate to the keystore
     *
     * @param rootCert root CA certificate to add to the certificate chain
     * @param cert certificate to add to the keystore
     * @param store keystore used to saving keypair
     * @param password password for the keystore
     * @param aliasKey name, under which the private key will be saved
     */
	
	public static void saveKeyPair (X509Certificate rootCert,X509Certificate cert, KeyStore store, KeyPair pair,char[] password,String aliasKey) throws KeyStoreException, OperatorCreationException, CertificateException, IOException {
		
		if(store == null || pair == null) return;
        int length = 2;
        if(rootCert == null) length = 1;
		java.security.cert.X509Certificate[] certChain = new java.security.cert.X509Certificate[length];
		certChain[0] = cert;
		if(rootCert != null) certChain[1] = cert;
		store.setKeyEntry(aliasKey,pair.getPrivate(), password, certChain);
	}

    /**
     * This method creates formated name for the certificate based on its algorithm and serial
     *
     * @param serial Serial of the certificate
     * @param algorithm Algorithm of the public key in this certificate
     * @return String certificate_serial_algorithm
     */

    public static String createDefAliasCert(int serial,String algorithm){
        return "certificate" +"_"+  serial + "_" +algorithm ;
    }

    /**
     * This method creates formated name for the private key based on its algorithm and serial
     *
     * @param serial Serial of the certificate matching this key
     * @param algorithm Algorithm of the key
     * @return String private_key_serial_algorithm
     */
    public static String createDefAliasKey(int serial,String algorithm){
        return "private_key" +"_"+  serial + "_" +algorithm ;
    }

    /**
     * This method saves the keystore into the private data of the application as a file
     *
     * @param store Keystore to save
     * @param context Context used for saving the store
     * @param password Password for the keystore
     * @param alias Name of the file
     */
	
	public static void saveStore (KeyStore store, Context context, String password, String alias) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeystoreAppException {

        Log.i("Utils","Is store null: "+Boolean.toString(store == null));
        Log.i("Utils","Is context null: "+Boolean.toString(context == null));
        Log.i("Utils","Is password null: "+Boolean.toString(password == null));
        if(store == null || context == null || password == null) throw new KeystoreAppException("Illegal parameters");
        OutputStream os  = null;
        try{
            os = context.openFileOutput(alias, Context.MODE_PRIVATE);
            store.store(os, password.toCharArray());
        }finally{
            if (os != null) {
                os.close();
            }
        }
    }

    public static void exportStore (KeyStore store, Context context, String password, String alias) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeystoreAppException {
        if(store == null || context == null || password == null) throw new KeystoreAppException("Illegal parameters");
        OutputStream os  = null;
        try{
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),alias);
            FileOutputStream fos = new FileOutputStream(file, false);
            store.store(fos, password.toCharArray());
        }finally{
            if (os != null) {
                os.close();
            }
        }
    }

    /**
     * This method gets aliases of all entries from the keystore
     *
     * @param store Keystore to get aliases from
     * @return List of all aliases as String
     */
	
	public static Collection <String> showStore (KeyStore store) throws KeyStoreException {
		
		if(store == null) return null;
		
		List <String >list = java.util.Collections.list(store.aliases());
		Collections.sort(list);
		return list;
	}

    /**
     * This method creates Keystore object from the file saved in the private storage of the application
     * and decrypts it with the given password
     *
     * @param context Context used to load file
     * @param password password of the keystore
     * @param alias Name of the keystore file
     * @return Keystore object with previously saved data
     */
	
	public static KeyStore loadStore (Context context, char[] password, String alias) throws NoSuchProviderException, NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException  {
		
		InputStream is = null;
		try{
		if (context == null) return null;
		//else if (password.length() < 1) return null;
		KeyStore store = KeyStore.getInstance(SecurityConstants.KEYSTORE_UBER,SecurityConstants.PROVIDER_BC);
		is = context.openFileInput(alias);
		store.load(is, password);
		return store;
		}finally{
            if (is != null) {
                is.close();
            }
        }
	}

    /**
     * This method deletes keystore file from the private storage of the application
     *
     * @param context Context used to delete file
     * @return Value true if file was deleted, false otherwise
     */
	
	public static boolean deleteStore (Context context)  {
        return context != null && context.deleteFile("keystore");
    }

    /**
     * Checks if keystore file exists, doesn't do any decryption, so the operation is fairly quick
     *
     * @param context Context used to load file
     * @return Value true if file exists, false otherwise
     */
	
	public static boolean checkStore (Context context) {
		try {
			if (context == null) return false;
			context.openFileInput("keystore");
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}


}



