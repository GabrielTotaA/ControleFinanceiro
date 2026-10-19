package com.financeiro.financeiro.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler (UsuarioNaoEncontradoException.class)
    public ResponseEntity<String> tratarUsuarioNaoEncontrado(UsuarioNaoEncontradoException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler (CategoriaNaoEncontradaException.class)
    public ResponseEntity<String> tratarCategoriaNaoEncontradaException(CategoriaNaoEncontradaException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler (TransacaoNaoEncontradaException.class)
    public ResponseEntity<String> tratarTransacaoNaoEncontrada(TransacaoNaoEncontradaException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler (EmailJaCadastradoException.class)
    public ResponseEntity<String> tratarEmailJaCadastrado(EmailJaCadastradoException ex){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler (CredenciaisInvalidasException.class)
    public ResponseEntity<String> tratarCredenciaisInvalidas(CredenciaisInvalidasException ex){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    @ExceptionHandler (DadosInvalidosException.class)
    public ResponseEntity<String> tratarDadosInvalidos(DadosInvalidosException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler (ValorInvalidoException.class)
    public ResponseEntity<String> tratarValorInvalido(ValorInvalidoException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler (PeriodoInvalidoException.class)
    public ResponseEntity<String> tratarPeriodoInvalido(PeriodoInvalidoException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler (CategoriaNaoPertenceAoUsuarioException.class)
    public ResponseEntity<String> tratarCategoriaNaoPertenceAoUsuarioException(CategoriaNaoPertenceAoUsuarioException ex){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
}
