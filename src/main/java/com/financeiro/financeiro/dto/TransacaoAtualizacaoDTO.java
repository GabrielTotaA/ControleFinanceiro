package com.financeiro.financeiro.dto;

import com.financeiro.financeiro.model.FormaPagamento;
import com.financeiro.financeiro.model.TipoTransacao;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoAtualizacaoDTO(
        @NotNull(message = "O valor é obrigatório") @Positive(message = "O valor deve ser maior que zero")
        @Digits(integer = 15, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais") BigDecimal valor,
        @NotNull(message = "A data é obrigatória") LocalDate data,
        @NotNull(message = "O tipo é obrigatório") TipoTransacao tipo,
        @NotNull(message = "A categoria é obrigatória") Long idCategoria,
        @NotNull(message = "A forma de pagamento é obrigatória") FormaPagamento formaPagamento) {
}
