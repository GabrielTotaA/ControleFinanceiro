package com.financeiro.financeiro.controller;

import com.financeiro.financeiro.config.UsuarioLogado;
import com.financeiro.financeiro.dto.ResumoDTO;
import com.financeiro.financeiro.dto.TransacaoRequestDTO;
import com.financeiro.financeiro.dto.TransacaoAtualizacaoDTO;
import com.financeiro.financeiro.dto.TransacaoResponseDTO;
import com.financeiro.financeiro.service.TransacaoService;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transacoes")
public class TransacaoController {
    private final TransacaoService service;
    public TransacaoController(TransacaoService service){
        this.service = service;
    }

    @PostMapping
    public TransacaoResponseDTO criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TransacaoRequestDTO dto){
        return service.criar(UsuarioLogado.id(jwt), dto.valor(), dto.data(), dto.tipo(), dto.idCategoria(), dto.formaPagamento());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id){
        service.excluir(id, UsuarioLogado.id(jwt));
    }

    @PutMapping("/{id}")
    public TransacaoResponseDTO atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                          @Valid @RequestBody TransacaoAtualizacaoDTO dto){
        return service.atualizar(id, UsuarioLogado.id(jwt), dto.valor(), dto.data(), dto.tipo(), dto.idCategoria(), dto.formaPagamento());
    }

    @GetMapping
    public List<TransacaoResponseDTO> listarPorUsuario(@AuthenticationPrincipal Jwt jwt){
        return service.listarPorUsuario(UsuarioLogado.id(jwt));
    }

    @GetMapping("/resumo")
    public ResumoDTO resumo(@AuthenticationPrincipal Jwt jwt,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim){
        return service.resumir(UsuarioLogado.id(jwt), inicio, fim);
    }
}
