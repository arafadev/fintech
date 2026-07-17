package com.fintech.fintech.exception;

public class InvalidTransactionException extends  RuntimeException{

    public InvalidTransactionException(String error){
        super(error);
    }

}
