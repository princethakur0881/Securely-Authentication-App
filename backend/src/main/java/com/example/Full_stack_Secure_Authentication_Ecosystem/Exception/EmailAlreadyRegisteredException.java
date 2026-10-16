package com.example.Full_stack_Secure_Authentication_Ecosystem.Exception;


public class EmailAlreadyRegisteredException
        extends RuntimeException {

    public EmailAlreadyRegisteredException(String message) {
        super(message);
    }
}