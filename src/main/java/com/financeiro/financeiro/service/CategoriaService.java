package com.financeiro.financeiro.service;


import com.financeiro.financeiro.dto.CategoriaResponseDTO;
import com.financeiro.financeiro.exception.CategoriaNaoEncontradaException;
import com.financeiro.financeiro.exception.CategoriaNaoPertenceAoUsuarioException;
import com.financeiro.financeiro.exception.UsuarioNaoEncontradoException;
import com.financeiro.financeiro.model.Categoria;
import com.financeiro.financeiro.model.Usuario;
import com.financeiro.financeiro.repository.CategoriaRepository;
import com.financeiro.financeiro.repository.TransacaoRepository;
import com.financeiro.financeiro.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaService {


    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository repository;
    private final TransacaoRepository transacaoRepository;
    public CategoriaService(CategoriaRepository repository, UsuarioRepository usuarioRepository, TransacaoRepository transacaoRepository){
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.transacaoRepository = transacaoRepository;
    }

    public CategoriaResponseDTO criar(String nome, Long idUsuario){
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow(() -> new UsuarioNaoEncontradoException("Erro ao cadastrar categoria"));
        Categoria categoria = repository.save(new Categoria(nome,usuario));
        return new CategoriaResponseDTO(categoria.getId(), categoria.getNome(), 0L);
    }

    public List<CategoriaResponseDTO> listarPorUsuario(Long idUsuario){
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado: " + idUsuario));
        return repository.listarComTotalPorUsuario(usuario);
    }

    public CategoriaResponseDTO atualizar(Long id, Long idUsuario, String nome){
        Categoria categoria = repository.findById(id)
                .orElseThrow(() -> new CategoriaNaoEncontradaException("Categoria não encontrada: " + id));
        if (categoria.getUsuario() == null){
            throw new CategoriaNaoPertenceAoUsuarioException("Categorias compartilhadas não podem ser editadas");
        }
        if (!categoria.getUsuario().getId().equals(idUsuario)){
            throw new CategoriaNaoEncontradaException("Categoria não encontrada: " + id);
        }
        categoria.renomear(nome);
        repository.save(categoria);
        return new CategoriaResponseDTO(categoria.getId(), categoria.getNome(), transacaoRepository.countByCategoria(categoria));
    }
}
