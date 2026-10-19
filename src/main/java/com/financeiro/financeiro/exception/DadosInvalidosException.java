package com.financeiro.financeiro.exception;

public class DadosInvalidosException extends RuntimeException{
    public DadosInvalidosException(String mensagem){
        super (mensagem);
    }
}
