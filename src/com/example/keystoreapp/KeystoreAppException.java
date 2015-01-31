package com.example.keystoreapp;

/**
 * This class represents basic exception that is thrown in some cases to avoid crashing the application
 *
 * @author Petr Konecny
 */
public class KeystoreAppException extends Exception {

    //Constructor
    public KeystoreAppException (String message){
        super(message);
    }
    //Constructor
    public KeystoreAppException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
