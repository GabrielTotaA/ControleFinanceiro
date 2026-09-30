package com.financeiro.financeiro.dto;

import com.financeiro.financeiro.model.FormaPagamento;
import com.financeiro.financeiro.model.TipoTransacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoRequestDTO(Long idUsuario, BigDecimal valor, LocalDate data, TipoTransacao tipo, Long idCategoria,
                                  FormaPagamento formaPagamento) {
}
