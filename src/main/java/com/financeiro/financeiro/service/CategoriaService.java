package com.financeiro.financeiro.service;


import com.financeiro.financeiro.exception.UsuarioNaoEncontradoException;
import com.financeiro.financeiro.model.Categoria;
import com.financeiro.financeiro.model.Usuario;
import com.financeiro.financeiro.repository.CategoriaRepository;
import com.financeiro.financeiro.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {


    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository repository;
    public CategoriaService(CategoriaRepository repository, UsuarioRepository usuarioRepository){
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    public Categoria criar(String nome, Long idUsuario){
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow(() -> new UsuarioNaoEncontradoException("Erro ao cadastrar categoria"));
        Categoria categoria = new Categoria(nome,usuario);
        return repository.save(categoria);
    }

    public List<Categoria> listarTodos(){
        return repository.findAll();
    }

    public Optional<Categoria> buscarPorId(Long id){
        return repository.findById(id);
    }
}
