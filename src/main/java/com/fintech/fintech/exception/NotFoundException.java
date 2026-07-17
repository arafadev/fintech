package com.fintech.fintech.exception;

public class NotFoundException extends  RuntimeException{

    public NotFoundException(String error){
        super(error);
    }
}
