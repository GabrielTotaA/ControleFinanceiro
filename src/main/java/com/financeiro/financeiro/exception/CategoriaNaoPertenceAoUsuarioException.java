package com.financeiro.financeiro.exception;

public class CategoriaNaoPertenceAoUsuarioException extends RuntimeException{
    public CategoriaNaoPertenceAoUsuarioException(String mensagem){
        super (mensagem);
    }
}
