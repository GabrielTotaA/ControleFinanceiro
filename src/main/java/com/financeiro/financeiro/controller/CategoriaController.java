package com.financeiro.financeiro.controller;

import com.financeiro.financeiro.config.UsuarioLogado;
import com.financeiro.financeiro.dto.CategoriaAtualizacaoDTO;
import com.financeiro.financeiro.dto.CategoriaRequestDTO;
import com.financeiro.financeiro.dto.CategoriaResponseDTO;
import com.financeiro.financeiro.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {
    private final CategoriaService service;
    public CategoriaController(CategoriaService service){
        this.service = service;
    }

    @PostMapping
    public CategoriaResponseDTO criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CategoriaRequestDTO dto){
        return service.criar(dto.nome(), UsuarioLogado.id(jwt));
    }

    @GetMapping
    public List<CategoriaResponseDTO> listarPorUsuario(@AuthenticationPrincipal Jwt jwt){
        return service.listarPorUsuario(UsuarioLogado.id(jwt));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id){
        service.excluir(id, UsuarioLogado.id(jwt));
    }

    @PutMapping("/{id}")
    public CategoriaResponseDTO atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                          @Valid @RequestBody CategoriaAtualizacaoDTO dto){
        return service.atualizar(id, UsuarioLogado.id(jwt), dto.nome());
    }
}
