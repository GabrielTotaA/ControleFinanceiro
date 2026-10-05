package com.financeiro.financeiro.service;

import com.financeiro.financeiro.dto.ResumoCategoriaDTO;
import com.financeiro.financeiro.dto.ResumoDTO;
import com.financeiro.financeiro.exception.CategoriaNaoEncontradaException;
import com.financeiro.financeiro.exception.CategoriaNaoPertenceAoUsuarioException;
import com.financeiro.financeiro.exception.PeriodoInvalidoException;
import com.financeiro.financeiro.exception.UsuarioNaoEncontradoException;
import com.financeiro.financeiro.model.*;
import com.financeiro.financeiro.repository.CategoriaRepository;
import com.financeiro.financeiro.repository.TransacaoRepository;
import com.financeiro.financeiro.repository.UsuarioRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransacaoService {

    private final UsuarioRepository usuarioRepository;
    private final TransacaoRepository transacaoRepository;
    private final CategoriaRepository categoriaRepository;
    public TransacaoService(TransacaoRepository transacaoRepository, UsuarioRepository usuarioRepository,CategoriaRepository categoriaRepository){
        this.transacaoRepository = transacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public Transacao criar(Long idUsuario, BigDecimal valor, LocalDate data, TipoTransacao tipo, Long idCategoria,
                           FormaPagamento formaPagamento){
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow(() -> new UsuarioNaoEncontradoException("Erro ao cadastrar transacao, usuario nao encontrado"));
        Categoria categoria = categoriaRepository.findById(idCategoria).orElseThrow(() -> new CategoriaNaoEncontradaException("Erro ao cadastrar transacao, categoria nao encontrada"));
        if (categoria.getUsuario() != null && !categoria.getUsuario().getId().equals(usuario.getId())){
            throw new CategoriaNaoPertenceAoUsuarioException("Essa categoria não pertence a este usuário");
        }
        return transacaoRepository.save(new Transacao(valor,data,categoria, formaPagamento,tipo,usuario));
    }

    public List<Transacao> listarPorUsuario(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado: " + idUsuario));
        return transacaoRepository.findByUsuario(usuario);
    }

    public ResumoDTO resumir(Long idUsuario, LocalDate inicio, LocalDate fim) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado: " + idUsuario));
        if (inicio.isAfter(fim)) {
            throw new PeriodoInvalidoException("A data inicial deve ser anterior ou igual à data final");
        }
        List<ResumoCategoriaDTO> porCategoria = transacaoRepository.somarPorCategoria(usuario, inicio, fim);
        BigDecimal totalEntradas = somarPorTipo(porCategoria, TipoTransacao.ENTRADA);
        BigDecimal totalSaidas = somarPorTipo(porCategoria, TipoTransacao.SAIDA);
        return new ResumoDTO(totalEntradas, totalSaidas, totalEntradas.subtract(totalSaidas), porCategoria);
    }

    private BigDecimal somarPorTipo(List<ResumoCategoriaDTO> linhas, TipoTransacao tipo) {
        return linhas.stream()
                .filter(linha -> linha.tipo() == tipo)
                .map(ResumoCategoriaDTO::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Transacao> listarTodos(){
        return transacaoRepository.findAll();
    }

}
