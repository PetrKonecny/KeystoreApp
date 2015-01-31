package com.example.keystoreapp;
/**
 * This class represents all static fields used in different parts of application. Having them in one
 * class makes it much easier to change them.
 *
 * @author Petr Konecny
 */
public final class SecurityConstants {

	public static final String SHA1RSA = "SHA1withRSA";
	public static final String SHA1DSA = "SHA1withDSA";
	public static final String KEYSTORE_DEFAULT = "BKS";
	public static final String PROVIDER_BC = "BC";
	public static final String PROVIDER_SC = "SC";
	public static final String AES = "AES";
	public static final String RSA = "RSA";
	public static final String DSA = "DSA";
	public static final String KEYSTORE_UBER = "UBER";
    public static final String KEYSTORE_NAME = "keystore";
    public static final String TESTKEYSTORE_NAME = "testkeystore";
	
	public static final int GENERATE = 4;
	public static final int SIGN = 1;
	public static final int VERIFY = 2;
	public static final int IMPORT = 7;
    public static final int IMPORT_STORE = 11;
    public static final int GENERATE_ROOT = 8;
    public static final int MATCH = 9;
    public static final int MISSMATCH = 10;
    public static final int UNLOCK = 3;
    public static final int IMPORT_KEY = 5;
    public static final int SIGN_ANDROID = 12;


    public static final String CLEARED = "Key store cleared";
	public static final String PASSWORD = "PASSWORD";
	public static final String GENERATING ="generating keypair";
	public static final String SGN = "signing file";
	public static final String VRF = "verifying file";
	public static final String GENERATED = "keypair generated";
	public static final String SIGNED = "File signed";

	public static final String OPEN_SSL = "AndroidOpenSSL";
}
