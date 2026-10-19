package com.financeiro.financeiro.exception;

public class MuitasTentativasException extends RuntimeException{
    public MuitasTentativasException(String mensagem){
        super (mensagem);
    }
}
