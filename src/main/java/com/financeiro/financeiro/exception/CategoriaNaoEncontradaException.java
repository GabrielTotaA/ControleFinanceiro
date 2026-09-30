package com.financeiro.financeiro.exception;

public class CategoriaNaoEncontradaException extends RuntimeException{
    public CategoriaNaoEncontradaException(String mensagem){
        super (mensagem);
    }
}
