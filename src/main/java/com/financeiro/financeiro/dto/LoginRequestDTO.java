package com.financeiro.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
        @NotBlank(message = "O e-mail é obrigatório") @Size(max = 254, message = "E-mail inválido") String email,
        @NotBlank(message = "A senha é obrigatória") @Size(max = 128, message = "Senha inválida") String senha) {
}
