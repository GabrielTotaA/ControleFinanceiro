package com.financeiro.financeiro.exception;

public class CategoriaEmUsoException extends RuntimeException{
    public CategoriaEmUsoException(String mensagem){
        super (mensagem);
    }
}
