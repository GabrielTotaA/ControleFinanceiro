package com.financeiro.financeiro.model;

import com.financeiro.financeiro.exception.ValorInvalidoException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Transacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private BigDecimal valor;
    @Enumerated(EnumType.STRING)
    private TipoTransacao tipo;
    private LocalDate data;

    @ManyToOne
    private Categoria categoria;
    @Enumerated(EnumType.STRING)
    private FormaPagamento formaPagamento;

    public Transacao(){}

    public Transacao(BigDecimal valor, LocalDate data, Categoria categoria, FormaPagamento formaPagamento,TipoTransacao tipo){
        if(valor.compareTo(BigDecimal.ZERO) <= 0){
            throw new ValorInvalidoException("O valor da transação deve ser maior que zero");
        }
        this.valor = valor;
        this.data = data;
        this.categoria = categoria;
        this.formaPagamento = formaPagamento;
        this.tipo = tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public LocalDate getData() {
        return data;
    }

    public Long getId() {
        return id;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }


}
