package com.example.keystoreapp.utils;

import com.example.keystoreapp.SecurityConstants;

import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.openssl.PEMException;
import org.spongycastle.openssl.PEMParser;
import org.spongycastle.openssl.PEMWriter;
import org.spongycastle.openssl.PKCS8Generator;
import org.spongycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.spongycastle.openssl.jcajce.JcaPKCS8Generator;
import org.spongycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.spongycastle.operator.InputDecryptorProvider;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.OutputEncryptor;
import org.spongycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.spongycastle.pkcs.PKCSException;
import org.spongycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;
import org.spongycastle.util.io.pem.PemGenerationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Enumeration;

/**
 * This is a class, that provides lower level functionality for import export operations. It contains static
 * methods for importing and exporting certificates, private keys or whole keystores.
 *
 * @author Petr Konecny
 */
public final class ImportExportUtils {
    /**
     * basic method for loading X509Certificate into keystore
     *
     * @param file file representing X.509 certificate
     * @return X509 object created from the file
     */
	public static X509Certificate importCertificate (File file) throws CertificateException, FileNotFoundException {
		return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new FileInputStream(file));
	}

    /**
     * Method that takes one of the many specilized Spongy Castle objects representing PEM data and
     * exports it to the sd card, if available.
     *
     * @param object Object to export, must me valid SpongyCastle PEM file
     * @param alias name of the saved file
     */
	public static void exportAsPem (Object object, String alias,File folder) throws IOException, OperatorCreationException {
        File file = new File(folder,alias);
        FileOutputStream fos = new FileOutputStream(file, false);
	    FileWriter fw = new FileWriter(fos.getFD());
		PEMWriter pw = new PEMWriter(fw);
		pw.writeObject (object);
		pw.close();
	    fos.getFD().sync();
	    fos.close();
	}

    /**
     * This method imports file in PKCS12 format
     *
     * @param file file to import
     * @param password password for the file
     * @param target keystore to import items into
     */
    public static void importPKCS12File (File file, String password,KeyStore target) throws NoSuchProviderException, KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyStore store = KeyStore.getInstance("PKCS12","BC");
        FileInputStream fis = new FileInputStream(file);
        store.load(fis, password.toCharArray());
        Enumeration aliases = store.aliases();
        while (aliases.hasMoreElements()) {
            String s = (String) aliases.nextElement();
            Key key = store.getKey(s,password.toCharArray());
            Certificate cert = store.getCertificate(s);
            if(store.isCertificateEntry(s)) {
                target.setCertificateEntry(s+"_imp", cert);
            }else if(store.isKeyEntry(s)) {
                target.setKeyEntry(s + "_imp", key,null, store.getCertificateChain(s));
            }
        }
    }

    /**
     * This method exports key pair as PKCS12 file
     *
     * @param key key to export
     * @param certChain chain matching this key
     * @param aliasKey alias for the key
     * @param folder folder to save into
     * @oaram alias name of the file
     * @param password password to set for the file
     */
    public static void exportKeyPairAsPKCS12 (PrivateKey key, Certificate[] certChain, String aliasKey, File folder, String alias,char[]password)
            throws NoSuchProviderException, KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore store = KeyStore.getInstance("PKCS12","BC");
        store.load(null, null);
        store.setKeyEntry(aliasKey,key,null,certChain);
        File file = new File(folder,alias);
        FileOutputStream fos = new FileOutputStream(file, false);
        store.store(fos,password);
    }

    /**
     * Method that creates new PKCS8 encoded Spongy Castle object from the standard java private key
     *
     * @param key Private key to process
     * @param password password used for encoding this key
     * @return PKCS8Enrypted private key info, if password wasn't null, PrivateKeyInfo otherwise
     */

	public static Object PrivateKeyToPKCS8(PrivateKey key, String password) throws OperatorCreationException, PemGenerationException {

		JceOpenSSLPKCS8EncryptorBuilder encryptorBuilder =
                new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.AES_256_CBC)
                        .setProvider(SecurityConstants.PROVIDER_SC);
		encryptorBuilder.setPasssword(password.toCharArray());
	    encryptorBuilder.setRandom(new SecureRandom());
	    OutputEncryptor output = encryptorBuilder.build();
	    JcaPKCS8Generator gen = new JcaPKCS8Generator(key,output);
	    return gen.generate();
	}

    /**
     * Converts valid PEM encoded file and transforms it into PEM type Spongy Castle object
     *
     * @param file file to convert
     * @return BouncyCastle representation of the object
     */
	public static Object convertPemObject (File file) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, NoSuchProviderException, PKCSException {
	     PEMParser pemReader = new PEMParser(new FileReader(file));
	     Object object = pemReader.readObject();
	     pemReader.close();
	     return object;
	}

    /**
     * reads private key saved in PEM format and transforms it into standard PrivateKey object
     *
     * @param object Private key to read
     * @param password Password protecting private key
*      @return PrivateKey extracted from the PEM Object
     */
    public static PrivateKey getPrivateKeyFromPem (Object object, char[] password) throws PKCSException, PEMException {

		InputDecryptorProvider decProv = new JcePKCSPBEInputDecryptorProviderBuilder().setProvider("SC").build(password);
	    JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("SC");
	     if(object instanceof PKCS8EncryptedPrivateKeyInfo) {
	    	 PrivateKeyInfo info = ((PKCS8EncryptedPrivateKeyInfo) object).decryptPrivateKeyInfo(decProv);
	    	 return converter.getPrivateKey(info);
	     }
	     if(object instanceof PrivateKeyInfo) {
	    	 return converter.getPrivateKey((PrivateKeyInfo) object);
	     }

	     return null;

	}

}
