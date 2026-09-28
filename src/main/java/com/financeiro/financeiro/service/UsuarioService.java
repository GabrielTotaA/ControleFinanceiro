package com.financeiro.financeiro.service;

import com.financeiro.financeiro.model.Usuario;
import com.financeiro.financeiro.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository){
        this.repository = repository;
    }

    public Usuario criar(String nome){
        Usuario usuario = new Usuario(nome);
        return repository.save(usuario);
    }
    
    public Optional<Usuario> buscarPorId(Long id){
        return repository.findById(id);
    }

    public List<Usuario> listarTodos(){
        return repository.findAll();
    }
}
