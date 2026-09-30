package com.financeiro.financeiro.exception;

public class UsuarioNaoEncontradoException extends RuntimeException{
    public UsuarioNaoEncontradoException(String mensagem){
        super (mensagem);
    }
}
