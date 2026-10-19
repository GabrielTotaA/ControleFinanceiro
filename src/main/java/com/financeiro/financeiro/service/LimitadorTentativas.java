package com.financeiro.financeiro.service;

import com.financeiro.financeiro.exception.MuitasTentativasException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita quantas tentativas uma chave (IP ou e-mail) pode fazer dentro de uma janela de tempo.
 * Fica em memória: reiniciar a aplicação zera os contadores.
 */
public class LimitadorTentativas {

    private static final int TAMANHO_MAXIMO_MAPA = 10_000;

    private record Janela(int tentativas, Instant inicio){}

    private final int maximo;
    private final Duration duracao;
    private final Clock relogio;
    private final ConcurrentHashMap<String, Janela> janelas = new ConcurrentHashMap<>();

    public LimitadorTentativas(int maximo, Duration duracao){
        this(maximo, duracao, Clock.systemUTC());
    }

    public LimitadorTentativas(int maximo, Duration duracao, Clock relogio){
        this.maximo = maximo;
        this.duracao = duracao;
        this.relogio = relogio;
    }

    public void verificar(String chave){
        Janela janela = janelas.get(chave);
        if (janela != null && !expirada(janela, relogio.instant()) && janela.tentativas() >= maximo){
            throw new MuitasTentativasException("Muitas tentativas. Tente novamente em alguns minutos");
        }
    }

    public void registrar(String chave){
        Instant agora = relogio.instant();
        janelas.compute(chave, (k, janela) -> janela == null || expirada(janela, agora)
                ? new Janela(1, agora)
                : new Janela(janela.tentativas() + 1, janela.inicio()));
        if (janelas.size() > TAMANHO_MAXIMO_MAPA){
            janelas.entrySet().removeIf(entrada -> expirada(entrada.getValue(), agora));
        }
    }

    public void limpar(String chave){
        janelas.remove(chave);
    }

    private boolean expirada(Janela janela, Instant agora){
        return janela.inicio().plus(duracao).isBefore(agora);
    }
}
