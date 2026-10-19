package com.financeiro.financeiro.dto;

import com.financeiro.financeiro.model.Usuario;

public record UsuarioResponseDTO(Long id, String nome) {

    public static UsuarioResponseDTO de(Usuario usuario){
        return  new UsuarioResponseDTO(usuario.getId(), usuario.getNome());
    }
}
