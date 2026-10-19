package com.financeiro.financeiro.dto;

import com.financeiro.financeiro.model.FormaPagamento;
import com.financeiro.financeiro.model.TipoTransacao;
import com.financeiro.financeiro.model.Transacao;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoResponseDTO(Long id, LocalDate data, BigDecimal valor, TipoTransacao tipo, Long idCategoria,
                                   String categoria, FormaPagamento formaPagamento) {

    public static TransacaoResponseDTO de(Transacao transacao) {
        return new TransacaoResponseDTO(transacao.getId(), transacao.getData(), transacao.getValor(),
                transacao.getTipo(), transacao.getCategoria().getId(), transacao.getCategoria().getNome(),
                transacao.getFormaPagamento());
    }
}
