package com.example.keystoreapp.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.example.keystoreapp.fragment.SimpleDialogFragment;
import com.google.common.io.ByteStreams;

import org.spongycastle.asn1.x500.X500Name;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAKey;
import java.security.interfaces.RSAKey;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

/**
 * This class contains various utility methods for different purposes, that didn't fit in ImportExport
 * or Keystore category. It contains various methods for file handling, string handling and formatting of
 * different messages.
 *
 * @author Petr Konecny
 */
public final class Utils {

    /**
     * Method to determine if the certificate and private key matches
     *
     * @param cert Certificate to match
     * @param key  Private key to match
     * @return True if certificate and key matches, false if it doesn't or keys are unreadable
     */
    public static boolean verifyMatch(Certificate cert, PrivateKey key) throws NoSuchAlgorithmException, NoSuchProviderException, SignatureException {
        String string = key.getAlgorithm();
        if (!string.equals(cert.getPublicKey().getAlgorithm())) return false;
        Signature s = Signature.getInstance("SHA1with" + string, "BC");
        try {
            s.initSign(key);
        } catch (InvalidKeyException e) {
            return false;
        }
        s.update("ABCDEFGHIJKLMN".getBytes());
        byte[] b = s.sign();
        try {
            s.initVerify(cert);
        } catch (InvalidKeyException e) {
            return false;
        }
        s.update("ABCDEFGHIJKLMN".getBytes());
        return s.verify(b);
    }

    /**
     * Saves byte array to the ouptput file
     *
     * @param input      Byte array to save
     * @param name       Name of the file
     * @param context    Context to save with
     * @param outputFile File to save to
     */
    public static void saveData(byte[] input, String name, Context context, File outputFile) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outputFile, false);
            fos.write(input);
            fos.close();
        } finally {
            if (fos != null) {
                fos.close();
            }
        }

    }

    /**
     * creates byte array from the file
     *
     * @param inputFile File to load
     * @return bytes of the file in form of byte array
     */
    public static byte[] loadFile(File inputFile) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(inputFile);
            return ByteStreams.toByteArray(fis);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    /**
     * method finds matching certificate for the private key
     *
     * @param key   Private key to search for
     * @param store store to search
     * @return Certificate matching this key, @null if no match find
     */
    public static Certificate findMatching(PrivateKey key, KeyStore store) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException, KeyStoreException {
        Enumeration<String> enumeration = store.aliases();

        while (enumeration.hasMoreElements()) {
            String s = enumeration.nextElement();
            Certificate cert = store.getCertificate(s);
            if (store.isCertificateEntry(s) && verifyMatch(cert, key)) {
                return cert;
            }
        }
        return null;
    }

    /**
     * simple method to separate one large string into bunch of smaller ones
     *
     * @param string string to process
     * @param split  string represetation of the key to split with
     * @return array of pieces of the original string
     */
    public static String[] separateString(String string, String split) {

        String[] strings = string.split(split);
        List<String> list = new ArrayList<String>();
        for (String string1 : strings) {

            if (string1.length() > 2) {
                list.add(string1.trim());
            }
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * simple method to find some substring in the array of strings, then cutting it out
     *
     * @param strings Array of strings to search in
     * @param name    substring to search for
     * @return String with removed substring, or not listed if wasn't found
     */
    public static String getCertName(String[] strings, String name) {

        for (String string : strings) {
            if (string.contains(name)) {
                string = string.replace(name, "");
                string = string.trim();
                return string;
            }
        }
        return "not listed";
    }

    /**
     * Method that gets various data from X509Certificate objects and transforms them into html coded String
     *
     * @param certificate certificate to process
     * @return String representing html code of the output data
     */
    public static String formatCertificateInfo(X509Certificate certificate) {
        StringBuilder builder = new StringBuilder();
        builder.append("<b>Certificate:</b><br><br>")
                .append("Version:<br>").append(certificate.getVersion()).append("<br><br>")
                .append("Type:<br>").append(certificate.getType()).append("<br><br>")
                .append("Signing algorythm:<br>").append(certificate.getSigAlgName()).append("<br><br>");

        String string = (certificate.getSubjectX500Principal().getName());
        String[] strings = separateString(string, ",");

        builder.append("<b>Subject info:</b><br><br>")
                .append("common name: <br>").append(getCertName(strings, "CN=")).append("<br><br>")
                .append("organization: <br>").append(getCertName(strings, "O=")).append("<br><br>")
                .append("organization unit: <br>").append(getCertName(strings, "OU=")).append("<br><br>");

        string = (certificate.getIssuerX500Principal().getName());
        strings = separateString(string, ",");

        builder.append("<b>Issuer info:</b><br><br>")
                .append("common name: <br>").append(getCertName(strings, "CN=")).append("<br><br>")
                .append("organization: <br>").append(getCertName(strings, "O=")).append("<br><br>")
                .append("organization unit: <br>").append(getCertName(strings, "OU=")).append("<br><br>")

                .append("<b>Validity:</b><br><br>")
                .append("from: <br>").append(certificate.getNotBefore().toString()).append("<br><br>")
                .append("to: <br>").append(certificate.getNotAfter().toString()).append("<br><br>");

        return builder.toString();
    }

    public static String formatKeyInfo(PrivateKey key) {
        StringBuilder builder = new StringBuilder();
        builder.append("<b>Private Key:</b><br><br>")
                .append("Algorithm:<br>").append(key.getAlgorithm()).append("<br><br>");

        if(key.getAlgorithm().equals("RSA")) {
            RSAKey rsaKey = (RSAKey) key;
            builder.append("Length:<br>").append(rsaKey.getModulus().bitLength()).append("<br><br>");
            builder.append("Modulus:<br>").append(rsaKey.getModulus()).append("<br><br>");
        }
        if(key.getAlgorithm().equals("DSA")) {
            DSAKey dsaKey = (DSAKey) key;
            builder.append("Length:<br>").append(dsaKey.getParams().getG().bitLength()).append("<br><br>");
            builder.append("G:<br>").append(dsaKey.getParams().getG()).append("<br><br>");
            builder.append("P:<br>").append(dsaKey.getParams().getP()).append("<br><br>");
            builder.append("Q:<br>").append(dsaKey.getParams().getQ()).append("<br><br>");

        }
        return builder.toString();
    }

    public static String getHelp () {
        return ("This is a key store app, application created for demonstration of PKI in OS Android, supported from version 2.3." +
                "It can generate a self-signed certificate and private key and verify and sign files stored on the device as well as import items" +
                "in PKCS12, PKCS8, DER or PEM encoding" +
                " <br><br>To generate a new certificate and it's matchning private key, select generate key pair from the  menu" +
                " <br><br>To import key or certificate select import key or cert in menu, if you are importing a private key, you must import it's matching certificate first" +
                " <br><br>To sign a file, select either imported or generated private key from the list, select sign in context menu and choose a file you want to sign" +
                "Signature file will be saved with same name as original file but .sgn extension in the same folder as the original file" +
                " <br><br>To verify a file, select certificate from the list and select verify");
    }

    /**
     * Method creates new X500Name object used for building certificates
     *
     * @param commonName       Common name of the entity
     * @param organization     Organization of the entity
     * @param organizationUnit Organization unit of the entity
     */

    public static X500Name setNameInfo(String commonName, String organization, String organizationUnit) {
        return new X500Name("CN=" + commonName + "," + "O=" + organization + "," + "OU=" + organizationUnit);

    }

    /**
     * Simple class to show some message and it's title as dialog fragment
     *
     * @param message Message to show in the dialog
     * @param title   Title of the dialog
     * @oaram activity FragmentActivity which FragmentManager is used to display this dialog
     */
    public static void showSimpleDialog(CharSequence message, CharSequence title, FragmentActivity activity) {

        DialogFragment dialog = new SimpleDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putCharSequence("message", message);
        bundle.putCharSequence("title", title);
        dialog.setArguments(bundle);
        dialog.show(activity.getSupportFragmentManager(), "DIALOG");
    }

    /**
     * Method lists all security providers and all their supported algorithms and writes them into log
     */
    public static void listSecurityProviders() {
        Provider[] providers = Security.getProviders();
        for (Provider provider : providers) {
            Log.i("CRYPTO", "provider: " + provider.getName());
            Set<Provider.Service> services = provider.getServices();
            for (Provider.Service service : services) {
                Log.i("CRYPTO", "  algorithm: " + service.getAlgorithm());
            }
        }
    }

    /**
     * Method creates intent for requesting a file
     * @return Intent that can be used
     */
    public static Intent createGetContentIntent() {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        return intent;
    }

    /**
     * Method gets file frome uri identificator
     * @return File found or null if nothing was
     */
    public static File getFile(Uri uri) {
        if (uri != null) {
            String filepath = uri.getPath();
            if (filepath != null) {
                return new File(filepath);
            }
        }
        return null;
    }

    /**
     * Method creates app folder in external storage
     * @return File representing a folder
     */
    public static File getAppFolder() {
        File folder = new File(Environment.getExternalStorageDirectory() + "/Keystoreapp");
        if (!folder.exists()) folder.mkdir();
        return folder;
    }
}
