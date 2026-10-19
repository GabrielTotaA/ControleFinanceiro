package com.financeiro.financeiro.controller;

import com.financeiro.financeiro.config.CorsConfig;
import com.financeiro.financeiro.config.SecurityConfig;
import com.financeiro.financeiro.dto.CategoriaResponseDTO;
import com.financeiro.financeiro.dto.LoginResponseDTO;
import com.financeiro.financeiro.dto.ResumoDTO;
import com.financeiro.financeiro.dto.TransacaoResponseDTO;
import com.financeiro.financeiro.dto.UsuarioResponseDTO;
import com.financeiro.financeiro.exception.CategoriaEmUsoException;
import com.financeiro.financeiro.exception.CategoriaNaoEncontradaException;
import com.financeiro.financeiro.exception.CategoriaNaoPertenceAoUsuarioException;
import com.financeiro.financeiro.exception.CredenciaisInvalidasException;
import com.financeiro.financeiro.exception.DadosInvalidosException;
import com.financeiro.financeiro.exception.EmailJaCadastradoException;
import com.financeiro.financeiro.exception.MuitasTentativasException;
import com.financeiro.financeiro.exception.PeriodoInvalidoException;
import com.financeiro.financeiro.exception.TransacaoNaoEncontradaException;
import com.financeiro.financeiro.exception.UsuarioNaoEncontradoException;
import com.financeiro.financeiro.exception.ValorInvalidoException;
import com.financeiro.financeiro.model.FormaPagamento;
import com.financeiro.financeiro.model.TipoTransacao;
import com.financeiro.financeiro.service.AuthService;
import com.financeiro.financeiro.service.CategoriaService;
import com.financeiro.financeiro.service.TransacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa só a camada web: rotas, segurança, validação e mapeamento de exceções para status HTTP.
 * Os services são substituídos por mocks.
 */
@WebMvcTest({AuthController.class, TransacaoController.class, CategoriaController.class, PingController.class})
@Import({SecurityConfig.class, CorsConfig.class})
class ControllersWebTest {

    private static final LocalDate DATA = LocalDate.of(2026, 10, 5);
    private static final String CORPO_TRANSACAO_VALIDO =
            "{\"valor\":85.00,\"data\":\"2026-10-05\",\"tipo\":\"SAIDA\",\"idCategoria\":3,\"formaPagamento\":\"PIX\"}";

    @Autowired private MockMvc mvc;

    @MockitoBean private AuthService authService;
    @MockitoBean private TransacaoService transacaoService;
    @MockitoBean private CategoriaService categoriaService;

    // ---------- helpers ----------

    private MockHttpServletRequestBuilder logado(MockHttpServletRequestBuilder requisicao){
        return requisicao.with(jwt().jwt(j -> j.subject("7")));
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requisicao, String corpo){
        return requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    private String corpo(ResultActions resultado) throws Exception {
        return resultado.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private TransacaoResponseDTO transacaoExemplo(){
        return new TransacaoResponseDTO(1L, DATA, new BigDecimal("85.00"), TipoTransacao.SAIDA, 3L, "Mercado", FormaPagamento.PIX);
    }

    // ---------- segurança ----------

    @Test
    void rotasProtegidasSemTokenDao401() throws Exception {
        mvc.perform(get("/transacoes")).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/transacoes"), CORPO_TRANSACAO_VALIDO)).andExpect(status().isUnauthorized());
        mvc.perform(json(put("/transacoes/1"), CORPO_TRANSACAO_VALIDO)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/transacoes/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/transacoes/resumo").param("inicio", "2026-10-01").param("fim", "2026-10-31"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/categorias")).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/categorias"), "{\"nome\":\"X\"}")).andExpect(status().isUnauthorized());
        mvc.perform(json(put("/categorias/1"), "{\"nome\":\"X\"}")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/categorias/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());

        verifyNoInteractions(transacaoService, categoriaService);
    }

    @Test
    void loginECadastroSaoPublicos() throws Exception {
        when(authService.login(eq("a@teste.com"), eq("senha1234"), any()))
                .thenReturn(new LoginResponseDTO("TOKEN", new UsuarioResponseDTO(1L, "A")));
        when(authService.registrar(eq("A"), eq("a@teste.com"), eq("senha1234"), any()))
                .thenReturn(new UsuarioResponseDTO(1L, "A"));

        mvc.perform(json(post("/auth/login"), "{\"email\":\"a@teste.com\",\"senha\":\"senha1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("TOKEN"))
                .andExpect(jsonPath("$.usuario.nome").value("A"));
        mvc.perform(json(post("/auth/registro"), "{\"nome\":\"A\",\"email\":\"a@teste.com\",\"senha\":\"senha1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void pingEPublico() throws Exception {
        mvc.perform(get("/ping")).andExpect(status().isOk());
    }

    // ---------- o usuário vem do token, nunca do corpo ----------

    @Test
    void criarTransacaoUsaOIdDoToken() throws Exception {
        when(transacaoService.criar(eq(7L), any(), any(), any(), any(), any())).thenReturn(transacaoExemplo());

        // o cliente tenta se passar por outro usuário no corpo: deve ser ignorado
        String corpoComIdFalso = "{\"idUsuario\":99,\"valor\":85.00,\"data\":\"2026-10-05\",\"tipo\":\"SAIDA\",\"idCategoria\":3,\"formaPagamento\":\"PIX\"}";
        mvc.perform(logado(json(post("/transacoes"), corpoComIdFalso)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria").value("Mercado"));

        verify(transacaoService).criar(eq(7L), eq(new BigDecimal("85.00")), eq(DATA), eq(TipoTransacao.SAIDA), eq(3L), eq(FormaPagamento.PIX));
        verify(transacaoService, never()).criar(eq(99L), any(), any(), any(), any(), any());
    }

    @Test
    void listarEResumirUsamOIdDoToken() throws Exception {
        when(transacaoService.listarPorUsuario(7L)).thenReturn(List.of(transacaoExemplo()));
        when(transacaoService.resumir(7L, DATA, DATA.plusDays(25)))
                .thenReturn(new ResumoDTO(BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("9"), List.of()));

        mvc.perform(logado(get("/transacoes"))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(logado(get("/transacoes/resumo").param("inicio", "2026-10-05").param("fim", "2026-10-30")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(9));
    }

    @Test
    void excluirEAtualizarPassamOIdDoTokenEDoRecurso() throws Exception {
        when(transacaoService.atualizar(eq(5L), eq(7L), any(), any(), any(), any(), any())).thenReturn(transacaoExemplo());
        when(categoriaService.atualizar(4L, 7L, "Novo")).thenReturn(new CategoriaResponseDTO(4L, "Novo", 0L));

        mvc.perform(logado(json(put("/transacoes/5"), CORPO_TRANSACAO_VALIDO))).andExpect(status().isOk());
        mvc.perform(logado(delete("/transacoes/5"))).andExpect(status().isNoContent());
        mvc.perform(logado(json(put("/categorias/4"), "{\"nome\":\"Novo\"}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Novo"));
        mvc.perform(logado(delete("/categorias/4"))).andExpect(status().isNoContent());

        verify(transacaoService).excluir(5L, 7L);
        verify(categoriaService).excluir(4L, 7L);
    }

    @Test
    void listarCategoriasUsaOIdDoToken() throws Exception {
        when(categoriaService.listarPorUsuario(7L)).thenReturn(List.of(new CategoriaResponseDTO(1L, "Mercado", 2L)));
        when(categoriaService.criar("Lazer", 7L)).thenReturn(new CategoriaResponseDTO(2L, "Lazer", 0L));

        mvc.perform(logado(get("/categorias")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalTransacoes").value(2));
        mvc.perform(logado(json(post("/categorias"), "{\"nome\":\"Lazer\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void meUsaOIdDoToken() throws Exception {
        when(authService.buscarUsuario(7L)).thenReturn(new UsuarioResponseDTO(7L, "Gabriel"));

        mvc.perform(logado(get("/auth/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Gabriel"));
    }

    // ---------- exceção -> status HTTP ----------

    @Test
    void usuarioNaoEncontradoDa404ComAMensagem() throws Exception {
        when(transacaoService.resumir(any(), any(), any())).thenThrow(new UsuarioNaoEncontradoException("Usuário não encontrado: 7"));

        ResultActions resultado = mvc.perform(logado(get("/transacoes/resumo").param("inicio", "2026-10-01").param("fim", "2026-10-31")))
                .andExpect(status().isNotFound());
        assertThat(corpo(resultado)).isEqualTo("Usuário não encontrado: 7");
    }

    @Test
    void recursosNaoEncontradosDao404() throws Exception {
        when(categoriaService.atualizar(any(), any(), any())).thenThrow(new CategoriaNaoEncontradaException("Categoria não encontrada: 4"));
        when(transacaoService.atualizar(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new TransacaoNaoEncontradaException("Transação não encontrada: 5"));

        mvc.perform(logado(json(put("/categorias/4"), "{\"nome\":\"X\"}"))).andExpect(status().isNotFound());
        mvc.perform(logado(json(put("/transacoes/5"), CORPO_TRANSACAO_VALIDO))).andExpect(status().isNotFound());
    }

    @Test
    void categoriaDeOutroUsuarioOuCompartilhadaDa403() throws Exception {
        when(transacaoService.criar(any(), any(), any(), any(), any(), any()))
                .thenThrow(new CategoriaNaoPertenceAoUsuarioException("Essa categoria não pertence a este usuário"));
        doThrow(new CategoriaNaoPertenceAoUsuarioException("Categorias compartilhadas não podem ser alteradas"))
                .when(categoriaService).excluir(any(), any());

        mvc.perform(logado(json(post("/transacoes"), CORPO_TRANSACAO_VALIDO))).andExpect(status().isForbidden());
        mvc.perform(logado(delete("/categorias/1"))).andExpect(status().isForbidden());
    }

    @Test
    void conflitosDao409() throws Exception {
        doThrow(new CategoriaEmUsoException("Esta categoria tem transações")).when(categoriaService).excluir(any(), any());
        when(authService.registrar(any(), any(), any(), any())).thenThrow(new EmailJaCadastradoException("Já existe uma conta com esse e-mail"));

        mvc.perform(logado(delete("/categorias/1"))).andExpect(status().isConflict());
        mvc.perform(json(post("/auth/registro"), "{\"nome\":\"A\",\"email\":\"a@teste.com\",\"senha\":\"senha1234\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void credenciaisInvalidasDao401ETentativasDemaisDao429() throws Exception {
        when(authService.login(eq("a@teste.com"), eq("errada123"), any())).thenThrow(new CredenciaisInvalidasException("E-mail ou senha inválidos"));
        when(authService.login(eq("b@teste.com"), eq("errada123"), any())).thenThrow(new MuitasTentativasException("Muitas tentativas"));

        ResultActions invalidas = mvc.perform(json(post("/auth/login"), "{\"email\":\"a@teste.com\",\"senha\":\"errada123\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(corpo(invalidas)).isEqualTo("E-mail ou senha inválidos");
        mvc.perform(json(post("/auth/login"), "{\"email\":\"b@teste.com\",\"senha\":\"errada123\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void errosDeRegraDeNegocioDao400() throws Exception {
        when(transacaoService.criar(any(), any(), any(), any(), any(), any())).thenThrow(new ValorInvalidoException("O valor da transação deve ser maior que zero"));
        when(transacaoService.resumir(any(), any(), any())).thenThrow(new PeriodoInvalidoException("A data inicial deve ser anterior ou igual à data final"));
        when(authService.registrar(any(), any(), any(), any())).thenThrow(new DadosInvalidosException("A senha é muito longa"));

        mvc.perform(logado(json(post("/transacoes"), CORPO_TRANSACAO_VALIDO))).andExpect(status().isBadRequest());
        mvc.perform(logado(get("/transacoes/resumo").param("inicio", "2026-10-31").param("fim", "2026-10-01")))
                .andExpect(status().isBadRequest());
        mvc.perform(json(post("/auth/registro"), "{\"nome\":\"A\",\"email\":\"a@teste.com\",\"senha\":\"senha1234\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- validação de entrada ----------

    @Test
    void transacaoSemCamposObrigatoriosDa400ListandoOsErros() throws Exception {
        ResultActions resultado = mvc.perform(logado(json(post("/transacoes"), "{}")))
                .andExpect(status().isBadRequest());

        assertThat(corpo(resultado))
                .contains("O valor é obrigatório")
                .contains("A data é obrigatória")
                .contains("O tipo é obrigatório")
                .contains("A categoria é obrigatória")
                .contains("A forma de pagamento é obrigatória");
        verifyNoInteractions(transacaoService);
    }

    @Test
    void transacaoComValorForaDasRegrasDa400() throws Exception {
        String negativo = "{\"valor\":-5,\"data\":\"2026-10-05\",\"tipo\":\"SAIDA\",\"idCategoria\":3,\"formaPagamento\":\"PIX\"}";
        String casasDemais = "{\"valor\":10.999,\"data\":\"2026-10-05\",\"tipo\":\"SAIDA\",\"idCategoria\":3,\"formaPagamento\":\"PIX\"}";

        assertThat(corpo(mvc.perform(logado(json(post("/transacoes"), negativo))).andExpect(status().isBadRequest())))
                .contains("O valor deve ser maior que zero");
        assertThat(corpo(mvc.perform(logado(json(put("/transacoes/1"), casasDemais))).andExpect(status().isBadRequest())))
                .contains("O valor deve ter no máximo 2 casas decimais");
        verifyNoInteractions(transacaoService);
    }

    @Test
    void categoriaComNomeEmBrancoOuLongoDemaisDa400() throws Exception {
        assertThat(corpo(mvc.perform(logado(json(post("/categorias"), "{\"nome\":\"   \"}"))).andExpect(status().isBadRequest())))
                .contains("O nome da categoria é obrigatório");
        assertThat(corpo(mvc.perform(logado(json(put("/categorias/1"), "{\"nome\":\"" + "a".repeat(61) + "\"}"))).andExpect(status().isBadRequest())))
                .contains("O nome deve ter no máximo 60 caracteres");
        verifyNoInteractions(categoriaService);
    }

    @Test
    void cadastroComDadosInvalidosDa400() throws Exception {
        assertThat(corpo(mvc.perform(json(post("/auth/registro"), "{\"nome\":\"A\",\"email\":\"nao-eh-email\",\"senha\":\"senha1234\"}"))
                .andExpect(status().isBadRequest()))).contains("E-mail inválido");
        assertThat(corpo(mvc.perform(json(post("/auth/registro"), "{\"nome\":\"A\",\"email\":\"a@teste.com\",\"senha\":\"curta\"}"))
                .andExpect(status().isBadRequest()))).contains("A senha deve ter entre 8 e 72 caracteres");
        assertThat(corpo(mvc.perform(json(post("/auth/registro"), "{\"nome\":\"\",\"email\":\"a@teste.com\",\"senha\":\"senha1234\"}"))
                .andExpect(status().isBadRequest()))).contains("O nome é obrigatório");
        verifyNoInteractions(authService);
    }

    @Test
    void loginSemCamposDa400() throws Exception {
        mvc.perform(json(post("/auth/login"), "{}")).andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }

    @Test
    void jsonQuebradoOuEnumInvalidoDa400ComMensagemGenerica() throws Exception {
        ResultActions quebrado = mvc.perform(logado(json(post("/transacoes"), "{valor:")))
                .andExpect(status().isBadRequest());
        assertThat(corpo(quebrado)).isEqualTo("Corpo da requisição inválido");

        String tipoInvalido = "{\"valor\":10,\"data\":\"2026-10-05\",\"tipo\":\"QUALQUER\",\"idCategoria\":3,\"formaPagamento\":\"PIX\"}";
        ResultActions enumInvalido = mvc.perform(logado(json(post("/transacoes"), tipoInvalido)))
                .andExpect(status().isBadRequest());
        assertThat(corpo(enumInvalido)).isEqualTo("Corpo da requisição inválido");
    }

    @Test
    void resumoSemPeriodoOuComDataMalFormatadaDa400() throws Exception {
        mvc.perform(logado(get("/transacoes/resumo"))).andExpect(status().isBadRequest());
        mvc.perform(logado(get("/transacoes/resumo").param("inicio", "05/10/2026").param("fim", "2026-10-31")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(transacaoService);
    }
}
