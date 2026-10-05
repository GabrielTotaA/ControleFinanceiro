package com.financeiro.financeiro.dto;

import com.financeiro.financeiro.model.TipoTransacao;

import java.math.BigDecimal;

public record ResumoCategoriaDTO(String categoria, TipoTransacao tipo, BigDecimal total) {
}
