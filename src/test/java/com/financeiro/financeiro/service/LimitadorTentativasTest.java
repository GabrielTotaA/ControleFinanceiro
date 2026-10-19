package com.financeiro.financeiro.service;

import com.financeiro.financeiro.exception.MuitasTentativasException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimitadorTentativasTest {

    static class RelogioMutavel extends Clock {
        private Instant agora = Instant.parse("2026-10-05T12:00:00Z");

        void avancar(Duration duracao){
            agora = agora.plus(duracao);
        }

        @Override public ZoneId getZone(){ return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone){ return this; }
        @Override public Instant instant(){ return agora; }
    }

    private RelogioMutavel relogio;
    private LimitadorTentativas limitador;

    @BeforeEach
    void preparar(){
        relogio = new RelogioMutavel();
        limitador = new LimitadorTentativas(3, Duration.ofMinutes(15), relogio);
    }

    @Test
    void naoBloqueiaEnquantoAbaixoDoMaximo(){
        limitador.registrar("a");
        limitador.registrar("a");

        assertThatCode(() -> limitador.verificar("a")).doesNotThrowAnyException();
    }

    @Test
    void bloqueiaAoAtingirOMaximo(){
        for (int i = 0; i < 3; i++) limitador.registrar("a");

        assertThatThrownBy(() -> limitador.verificar("a")).isInstanceOf(MuitasTentativasException.class);
    }

    @Test
    void chavesDiferentesSaoIndependentes(){
        for (int i = 0; i < 3; i++) limitador.registrar("a");

        assertThatCode(() -> limitador.verificar("b")).doesNotThrowAnyException();
    }

    @Test
    void liberaDepoisQueAJanelaExpira(){
        for (int i = 0; i < 3; i++) limitador.registrar("a");
        relogio.avancar(Duration.ofMinutes(16));

        assertThatCode(() -> limitador.verificar("a")).doesNotThrowAnyException();
    }

    @Test
    void naoLiberaAntesDaJanelaExpirar(){
        for (int i = 0; i < 3; i++) limitador.registrar("a");
        relogio.avancar(Duration.ofMinutes(14));

        assertThatThrownBy(() -> limitador.verificar("a")).isInstanceOf(MuitasTentativasException.class);
    }

    @Test
    void novaTentativaAposExpirarReiniciaAContagem(){
        for (int i = 0; i < 3; i++) limitador.registrar("a");
        relogio.avancar(Duration.ofMinutes(16));
        limitador.registrar("a");

        assertThatCode(() -> limitador.verificar("a")).doesNotThrowAnyException();
    }

    @Test
    void limparRemoveOContador(){
        for (int i = 0; i < 3; i++) limitador.registrar("a");
        limitador.limpar("a");

        assertThatCode(() -> limitador.verificar("a")).doesNotThrowAnyException();
    }
}
