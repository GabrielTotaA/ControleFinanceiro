package com.financeiro.financeiro.controller;

import com.financeiro.financeiro.dto.CategoriaRequestDTO;
import com.financeiro.financeiro.dto.UsuarioRequestDTO;
import com.financeiro.financeiro.model.Categoria;
import com.financeiro.financeiro.model.Usuario;
import com.financeiro.financeiro.service.CategoriaService;
import com.financeiro.financeiro.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categoria")
public class CategoriaController {
    private final CategoriaService service;
    public CategoriaController(CategoriaService service){
        this.service = service;
    }

    @PostMapping
    public Categoria criar(@RequestBody CategoriaRequestDTO dto){
        return  service.criar(dto.nome(),dto.idUsuario());
    }

    @GetMapping
    public List<Categoria> listarTodos(){
        return service.listarTodos();
    }

    @GetMapping
    public ResponseEntity<Categoria> buscarPeloId(@PathVariable Long id){
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
