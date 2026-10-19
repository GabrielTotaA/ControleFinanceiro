package com.financeiro.financeiro.model;

import com.financeiro.financeiro.exception.ValorInvalidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransacaoTest {

    private static final LocalDate DATA = LocalDate.of(2026, 10, 5);

    private Transacao nova(BigDecimal valor){
        return new Transacao(valor, DATA, null, FormaPagamento.PIX, TipoTransacao.SAIDA, null);
    }

    @Test
    void criaComValorPositivo(){
        assertThat(nova(new BigDecimal("0.01")).getValor()).isEqualByComparingTo("0.01");
    }

    @Test
    void recusaValorZero(){
        assertThatThrownBy(() -> nova(BigDecimal.ZERO)).isInstanceOf(ValorInvalidoException.class);
    }

    @Test
    void recusaValorNegativo(){
        assertThatThrownBy(() -> nova(new BigDecimal("-1"))).isInstanceOf(ValorInvalidoException.class);
    }

    @Test
    void recusaValorNulo(){
        assertThatThrownBy(() -> nova(null)).isInstanceOf(ValorInvalidoException.class);
    }

    @Test
    void atualizarAplicaAMesmaRegraDeValor(){
        Transacao transacao = nova(new BigDecimal("10"));

        assertThatThrownBy(() -> transacao.atualizar(BigDecimal.ZERO, DATA, null, FormaPagamento.PIX, TipoTransacao.SAIDA))
                .isInstanceOf(ValorInvalidoException.class);
        assertThat(transacao.getValor()).isEqualByComparingTo("10");
    }

    @Test
    void atualizarTrocaTodosOsCamposMenosODono(){
        Usuario dono = new Usuario("Dono", "d@teste.com", "HASH");
        Transacao transacao = new Transacao(new BigDecimal("10"), DATA, null, FormaPagamento.PIX, TipoTransacao.SAIDA, dono);

        transacao.atualizar(new BigDecimal("99.90"), DATA.plusDays(2), null, FormaPagamento.CREDITO, TipoTransacao.ENTRADA);

        assertThat(transacao.getValor()).isEqualByComparingTo("99.90");
        assertThat(transacao.getData()).isEqualTo(DATA.plusDays(2));
        assertThat(transacao.getFormaPagamento()).isEqualTo(FormaPagamento.CREDITO);
        assertThat(transacao.getTipo()).isEqualTo(TipoTransacao.ENTRADA);
        assertThat(transacao.getUsuario()).isSameAs(dono);
    }
}
