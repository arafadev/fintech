package com.fintech.fintech.exception;

public class InsufficientBalanceException extends  RuntimeException{

    public InsufficientBalanceException(String error){
        super(error);
    }

}
