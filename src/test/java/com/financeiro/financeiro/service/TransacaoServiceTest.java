package com.financeiro.financeiro.service;

import com.financeiro.financeiro.dto.ResumoCategoriaDTO;
import com.financeiro.financeiro.dto.ResumoDTO;
import com.financeiro.financeiro.dto.TransacaoResponseDTO;
import com.financeiro.financeiro.exception.CategoriaNaoEncontradaException;
import com.financeiro.financeiro.exception.CategoriaNaoPertenceAoUsuarioException;
import com.financeiro.financeiro.exception.PeriodoInvalidoException;
import com.financeiro.financeiro.exception.TransacaoNaoEncontradaException;
import com.financeiro.financeiro.exception.UsuarioNaoEncontradoException;
import com.financeiro.financeiro.exception.ValorInvalidoException;
import com.financeiro.financeiro.model.Categoria;
import com.financeiro.financeiro.model.FormaPagamento;
import com.financeiro.financeiro.model.TipoTransacao;
import com.financeiro.financeiro.model.Transacao;
import com.financeiro.financeiro.model.Usuario;
import com.financeiro.financeiro.repository.CategoriaRepository;
import com.financeiro.financeiro.repository.TransacaoRepository;
import com.financeiro.financeiro.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransacaoServiceTest {

    private static final LocalDate DATA = LocalDate.of(2026, 10, 5);

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TransacaoRepository transacaoRepository;
    @Mock private CategoriaRepository categoriaRepository;

    private TransacaoService service;

    @BeforeEach
    void preparar(){
        service = new TransacaoService(transacaoRepository, usuarioRepository, categoriaRepository);
    }

    private Usuario usuario(Long id){
        Usuario u = new Usuario("Usuario " + id, "u" + id + "@teste.com", "HASH");
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private Categoria categoria(Long id, Usuario dono){
        Categoria c = new Categoria("Cat" + id, dono);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private Transacao transacao(Long id, Usuario dono, Categoria categoria){
        Transacao t = new Transacao(new BigDecimal("100.00"), DATA, categoria, FormaPagamento.PIX, TipoTransacao.SAIDA, dono);
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    // ---------- criar ----------

    @Test
    void criarComCategoriaDoProprioUsuario(){
        Usuario u1 = usuario(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u1));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, u1)));
        when(transacaoRepository.save(any(Transacao.class))).thenAnswer(inv -> inv.getArgument(0));

        TransacaoResponseDTO resposta = service.criar(1L, new BigDecimal("85.00"), DATA, TipoTransacao.SAIDA, 10L, FormaPagamento.PIX);

        assertThat(resposta.valor()).isEqualByComparingTo("85.00");
        assertThat(resposta.categoria()).isEqualTo("Cat10");
    }

    @Test
    void criarComCategoriaCompartilhadaEPermitido(){
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L)));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, null)));
        when(transacaoRepository.save(any(Transacao.class))).thenAnswer(inv -> inv.getArgument(0));

        TransacaoResponseDTO resposta = service.criar(1L, new BigDecimal("10"), DATA, TipoTransacao.ENTRADA, 10L, FormaPagamento.DEBITO);

        assertThat(resposta.tipo()).isEqualTo(TipoTransacao.ENTRADA);
    }

    @Test
    void criarComCategoriaDeOutroUsuarioEProibido(){
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L)));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, usuario(2L))));

        assertThatThrownBy(() -> service.criar(1L, new BigDecimal("10"), DATA, TipoTransacao.SAIDA, 10L, FormaPagamento.PIX))
                .isInstanceOf(CategoriaNaoPertenceAoUsuarioException.class);
        verify(transacaoRepository, never()).save(any());
    }

    @Test
    void criarComCategoriaInexistente(){
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L)));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(1L, new BigDecimal("10"), DATA, TipoTransacao.SAIDA, 10L, FormaPagamento.PIX))
                .isInstanceOf(CategoriaNaoEncontradaException.class);
    }

    @Test
    void criarComUsuarioInexistente(){
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(1L, new BigDecimal("10"), DATA, TipoTransacao.SAIDA, 10L, FormaPagamento.PIX))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
    }

    @Test
    void criarComValorZeroOuNegativoEhRecusado(){
        Usuario u1 = usuario(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u1));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, u1)));

        assertThatThrownBy(() -> service.criar(1L, BigDecimal.ZERO, DATA, TipoTransacao.SAIDA, 10L, FormaPagamento.PIX))
                .isInstanceOf(ValorInvalidoException.class);
        assertThatThrownBy(() -> service.criar(1L, new BigDecimal("-5"), DATA, TipoTransacao.SAIDA, 10L, FormaPagamento.PIX))
                .isInstanceOf(ValorInvalidoException.class);
        verify(transacaoRepository, never()).save(any());
    }

    // ---------- atualizar ----------

    @Test
    void atualizarAlteraOsCampos(){
        Usuario u1 = usuario(1L);
        Categoria antiga = categoria(10L, u1);
        Categoria nova = categoria(11L, u1);
        when(transacaoRepository.findById(5L)).thenReturn(Optional.of(transacao(5L, u1, antiga)));
        when(categoriaRepository.findById(11L)).thenReturn(Optional.of(nova));
        when(transacaoRepository.save(any(Transacao.class))).thenAnswer(inv -> inv.getArgument(0));

        TransacaoResponseDTO resposta = service.atualizar(5L, 1L, new BigDecimal("250.00"), DATA.plusDays(1),
                TipoTransacao.ENTRADA, 11L, FormaPagamento.DEBITO);

        assertThat(resposta.valor()).isEqualByComparingTo("250.00");
        assertThat(resposta.data()).isEqualTo(DATA.plusDays(1));
        assertThat(resposta.tipo()).isEqualTo(TipoTransacao.ENTRADA);
        assertThat(resposta.idCategoria()).isEqualTo(11L);
        assertThat(resposta.formaPagamento()).isEqualTo(FormaPagamento.DEBITO);
    }

    @Test
    void atualizarTransacaoDeOutroUsuarioDaNaoEncontrada(){
        Usuario dono = usuario(2L);
        when(transacaoRepository.findById(5L)).thenReturn(Optional.of(transacao(5L, dono, categoria(10L, dono))));

        assertThatThrownBy(() -> service.atualizar(5L, 1L, new BigDecimal("1"), DATA, TipoTransacao.SAIDA, 10L, FormaPagamento.PIX))
                .isInstanceOf(TransacaoNaoEncontradaException.class);
        verify(transacaoRepository, never()).save(any());
    }

    @Test
    void atualizarTransacaoInexistente(){
        when(transacaoRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(5L, 1L, new BigDecimal("1"), DATA, TipoTransacao.SAIDA, 10L, FormaPagamento.PIX))
                .isInstanceOf(TransacaoNaoEncontradaException.class);
    }

    @Test
    void atualizarComCategoriaDeOutroUsuarioEProibido(){
        Usuario u1 = usuario(1L);
        when(transacaoRepository.findById(5L)).thenReturn(Optional.of(transacao(5L, u1, categoria(10L, u1))));
        when(categoriaRepository.findById(20L)).thenReturn(Optional.of(categoria(20L, usuario(2L))));

        assertThatThrownBy(() -> service.atualizar(5L, 1L, new BigDecimal("1"), DATA, TipoTransacao.SAIDA, 20L, FormaPagamento.PIX))
                .isInstanceOf(CategoriaNaoPertenceAoUsuarioException.class);
        verify(transacaoRepository, never()).save(any());
    }

    @Test
    void atualizarComValorNegativoEhRecusado(){
        Usuario u1 = usuario(1L);
        Categoria cat = categoria(10L, u1);
        when(transacaoRepository.findById(5L)).thenReturn(Optional.of(transacao(5L, u1, cat)));
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(cat));

        assertThatThrownBy(() -> service.atualizar(5L, 1L, new BigDecimal("-1"), DATA, TipoTransacao.SAIDA, 10L, FormaPagamento.PIX))
                .isInstanceOf(ValorInvalidoException.class);
        verify(transacaoRepository, never()).save(any());
    }

    // ---------- excluir ----------

    @Test
    void excluirTransacaoDoProprioUsuario(){
        Usuario u1 = usuario(1L);
        Transacao t = transacao(5L, u1, categoria(10L, u1));
        when(transacaoRepository.findById(5L)).thenReturn(Optional.of(t));

        service.excluir(5L, 1L);

        verify(transacaoRepository).delete(t);
    }

    @Test
    void excluirTransacaoDeOutroUsuarioDaNaoEncontrada(){
        Usuario dono = usuario(2L);
        when(transacaoRepository.findById(5L)).thenReturn(Optional.of(transacao(5L, dono, categoria(10L, dono))));

        assertThatThrownBy(() -> service.excluir(5L, 1L)).isInstanceOf(TransacaoNaoEncontradaException.class);
        verify(transacaoRepository, never()).delete(any(Transacao.class));
    }

    // ---------- resumo ----------

    @Test
    void resumirCalculaEntradasSaidasESaldo(){
        Usuario u1 = usuario(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u1));
        when(transacaoRepository.somarPorCategoria(u1, DATA, DATA.plusDays(30))).thenReturn(List.of(
                new ResumoCategoriaDTO("Salário", TipoTransacao.ENTRADA, new BigDecimal("5000.00")),
                new ResumoCategoriaDTO("Mercado", TipoTransacao.SAIDA, new BigDecimal("600.00")),
                new ResumoCategoriaDTO("Lazer", TipoTransacao.SAIDA, new BigDecimal("250.50"))));

        ResumoDTO resumo = service.resumir(1L, DATA, DATA.plusDays(30));

        assertThat(resumo.totalEntradas()).isEqualByComparingTo("5000.00");
        assertThat(resumo.totalSaidas()).isEqualByComparingTo("850.50");
        assertThat(resumo.saldo()).isEqualByComparingTo("4149.50");
        assertThat(resumo.porCategoria()).hasSize(3);
    }

    @Test
    void resumirSemTransacoesDevolveZeros(){
        Usuario u1 = usuario(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u1));
        when(transacaoRepository.somarPorCategoria(u1, DATA, DATA)).thenReturn(List.of());

        ResumoDTO resumo = service.resumir(1L, DATA, DATA);

        assertThat(resumo.totalEntradas()).isEqualByComparingTo("0");
        assertThat(resumo.totalSaidas()).isEqualByComparingTo("0");
        assertThat(resumo.saldo()).isEqualByComparingTo("0");
        assertThat(resumo.porCategoria()).isEmpty();
    }

    @Test
    void resumirComPeriodoInvertidoEhRecusado(){
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L)));

        assertThatThrownBy(() -> service.resumir(1L, DATA.plusDays(1), DATA))
                .isInstanceOf(PeriodoInvalidoException.class);
        verify(transacaoRepository, never()).somarPorCategoria(any(), any(), any());
    }

    @Test
    void resumirComUsuarioInexistente(){
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resumir(1L, DATA, DATA)).isInstanceOf(UsuarioNaoEncontradoException.class);
    }
}
