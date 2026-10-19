package com.financeiro.financeiro.service;

import com.financeiro.financeiro.dto.CategoriaResponseDTO;
import com.financeiro.financeiro.exception.CategoriaEmUsoException;
import com.financeiro.financeiro.exception.CategoriaNaoEncontradaException;
import com.financeiro.financeiro.exception.CategoriaNaoPertenceAoUsuarioException;
import com.financeiro.financeiro.exception.UsuarioNaoEncontradoException;
import com.financeiro.financeiro.model.Categoria;
import com.financeiro.financeiro.model.Usuario;
import com.financeiro.financeiro.repository.CategoriaRepository;
import com.financeiro.financeiro.repository.TransacaoRepository;
import com.financeiro.financeiro.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock private CategoriaRepository repository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TransacaoRepository transacaoRepository;

    private CategoriaService service;

    @BeforeEach
    void preparar(){
        service = new CategoriaService(repository, usuarioRepository, transacaoRepository);
    }

    private Usuario usuario(Long id){
        Usuario u = new Usuario("Usuario " + id, "u" + id + "@teste.com", "HASH");
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private Categoria categoria(Long id, String nome, Usuario dono){
        Categoria c = new Categoria(nome, dono);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    // ---------- criar ----------

    @Test
    void criarDevolveACategoriaComTotalZero(){
        Usuario u1 = usuario(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u1));
        when(repository.save(any(Categoria.class))).thenAnswer(inv -> {
            Categoria c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 7L);
            return c;
        });

        CategoriaResponseDTO resposta = service.criar("Mercado", 1L);

        assertThat(resposta.id()).isEqualTo(7L);
        assertThat(resposta.nome()).isEqualTo("Mercado");
        assertThat(resposta.totalTransacoes()).isZero();
    }

    @Test
    void criarComUsuarioInexistente(){
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar("Mercado", 1L)).isInstanceOf(UsuarioNaoEncontradoException.class);
        verify(repository, never()).save(any());
    }

    // ---------- atualizar ----------

    @Test
    void atualizarRenomeiaECalculaTotal(){
        Categoria categoria = categoria(10L, "Antigo", usuario(1L));
        when(repository.findById(10L)).thenReturn(Optional.of(categoria));
        when(transacaoRepository.countByCategoria(categoria)).thenReturn(3L);

        CategoriaResponseDTO resposta = service.atualizar(10L, 1L, "Novo");

        assertThat(resposta.nome()).isEqualTo("Novo");
        assertThat(resposta.totalTransacoes()).isEqualTo(3L);
        verify(repository).save(categoria);
    }

    @Test
    void atualizarCategoriaCompartilhadaEProibido(){
        when(repository.findById(10L)).thenReturn(Optional.of(categoria(10L, "Geral", null)));

        assertThatThrownBy(() -> service.atualizar(10L, 1L, "Novo"))
                .isInstanceOf(CategoriaNaoPertenceAoUsuarioException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void atualizarCategoriaDeOutroUsuarioDaNaoEncontrada(){
        when(repository.findById(10L)).thenReturn(Optional.of(categoria(10L, "Dela", usuario(2L))));

        assertThatThrownBy(() -> service.atualizar(10L, 1L, "Novo"))
                .isInstanceOf(CategoriaNaoEncontradaException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void atualizarCategoriaInexistente(){
        when(repository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(10L, 1L, "Novo"))
                .isInstanceOf(CategoriaNaoEncontradaException.class);
    }

    // ---------- excluir ----------

    @Test
    void excluirCategoriaSemTransacoes(){
        Categoria categoria = categoria(10L, "Vazia", usuario(1L));
        when(repository.findById(10L)).thenReturn(Optional.of(categoria));
        when(transacaoRepository.countByCategoria(categoria)).thenReturn(0L);

        service.excluir(10L, 1L);

        verify(repository).delete(categoria);
    }

    @Test
    void excluirCategoriaComTransacoesEhBloqueado(){
        Categoria categoria = categoria(10L, "Usada", usuario(1L));
        when(repository.findById(10L)).thenReturn(Optional.of(categoria));
        when(transacaoRepository.countByCategoria(categoria)).thenReturn(12L);

        assertThatThrownBy(() -> service.excluir(10L, 1L)).isInstanceOf(CategoriaEmUsoException.class);
        verify(repository, never()).delete(any(Categoria.class));
    }

    @Test
    void excluirConverteViolacaoDeChaveEstrangeiraEmCategoriaEmUso(){
        Categoria categoria = categoria(10L, "Corrida", usuario(1L));
        when(repository.findById(10L)).thenReturn(Optional.of(categoria));
        when(transacaoRepository.countByCategoria(categoria)).thenReturn(0L);
        doThrow(new DataIntegrityViolationException("fk")).when(repository).delete(categoria);

        assertThatThrownBy(() -> service.excluir(10L, 1L)).isInstanceOf(CategoriaEmUsoException.class);
    }

    @Test
    void excluirCategoriaCompartilhadaEProibido(){
        when(repository.findById(10L)).thenReturn(Optional.of(categoria(10L, "Geral", null)));

        assertThatThrownBy(() -> service.excluir(10L, 1L)).isInstanceOf(CategoriaNaoPertenceAoUsuarioException.class);
        verify(repository, never()).delete(any(Categoria.class));
    }

    @Test
    void excluirCategoriaDeOutroUsuarioDaNaoEncontrada(){
        when(repository.findById(10L)).thenReturn(Optional.of(categoria(10L, "Dela", usuario(2L))));

        assertThatThrownBy(() -> service.excluir(10L, 1L)).isInstanceOf(CategoriaNaoEncontradaException.class);
        verify(repository, never()).delete(any(Categoria.class));
    }
}
