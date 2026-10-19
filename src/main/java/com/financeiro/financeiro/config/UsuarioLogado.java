package com.financeiro.financeiro.config;

import org.springframework.security.oauth2.jwt.Jwt;

public final class UsuarioLogado {

    private UsuarioLogado(){}

    public static Long id(Jwt jwt){
        return Long.valueOf(jwt.getSubject());
    }
}
