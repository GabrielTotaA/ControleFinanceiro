package com.financeiro.financeiro.controller;

import com.financeiro.financeiro.dto.UsuarioRequestDTO;
import com.financeiro.financeiro.model.Usuario;
import com.financeiro.financeiro.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {
    private final UsuarioService service;
    public UsuarioController(UsuarioService service){
        this.service = service;
    }

    @PostMapping("/criar")
    public Usuario criar(@RequestBody UsuarioRequestDTO dto){
        return  service.criar(dto.nome());
    }

    @GetMapping("/listar")
    public List<Usuario> listarTodos(){
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> buscarPeloId(@PathVariable Long id){
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
