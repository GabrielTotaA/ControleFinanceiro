package com.financeiro.financeiro;

import com.financeiro.financeiro.model.Categoria;
import com.financeiro.financeiro.repository.CategoriaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sobe a aplicação inteira (segurança, JPA e banco H2 em memória) e exercita a API por HTTP.
 * Cada chamada usa um IP diferente para não esbarrar no limite de tentativas por IP.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FluxoCompletoIntegracaoTest {

    private static final AtomicInteger CONTADOR_IP = new AtomicInteger();

    @Autowired private MockMvc mvc;
    @Autowired private CategoriaRepository categoriaRepository;

    // ---------- helpers ----------

    private MockHttpServletRequestBuilder comIpProprio(MockHttpServletRequestBuilder requisicao){
        int n = CONTADOR_IP.incrementAndGet();
        return requisicao.with(r -> {
            r.setRemoteAddr("10." + (n / 65536 % 256) + "." + (n / 256 % 256) + "." + (n % 256));
            return r;
        });
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requisicao, String corpo){
        return requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    private String emailUnico(){
        return "u" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";
    }

    private void registrar(String nome, String email, String senha) throws Exception {
        mvc.perform(comIpProprio(json(post("/auth/registro"),
                        "{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}")))
                .andExpect(status().isOk());
    }

    private String login(String email, String senha) throws Exception {
        MvcResult resultado = mvc.perform(comIpProprio(json(post("/auth/login"),
                        "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}")))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
    }

    private String novoUsuarioComToken() throws Exception {
        String email = emailUnico();
        registrar("Usuario", email, "senha1234");
        return login(email, "senha1234");
    }

    private String bearer(String token){
        return "Bearer " + token;
    }

    private long criarCategoria(String token, String nome) throws Exception {
        MvcResult resultado = mvc.perform(json(post("/categorias"), "{\"nome\":\"" + nome + "\"}")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String corpoTransacao(String valor, String tipo, long idCategoria){
        return "{\"valor\":" + valor + ",\"data\":\"2026-10-05\",\"tipo\":\"" + tipo
                + "\",\"idCategoria\":" + idCategoria + ",\"formaPagamento\":\"PIX\"}";
    }

    private long criarTransacao(String token, String valor, String tipo, long idCategoria) throws Exception {
        MvcResult resultado = mvc.perform(json(post("/transacoes"), corpoTransacao(valor, tipo, idCategoria))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.id")).longValue();
    }

    // ---------- fluxo feliz ----------

    @Test
    void fluxoCompletoCadastroLoginCategoriaTransacaoEResumo() throws Exception {
        String token = novoUsuarioComToken();
        long salario = criarCategoria(token, "Salario");
        long mercado = criarCategoria(token, "Mercado");
        criarTransacao(token, "5000.00", "ENTRADA", salario);
        criarTransacao(token, "600.50", "SAIDA", mercado);

        mvc.perform(get("/transacoes/resumo").param("inicio", "2026-10-01").param("fim", "2026-10-31")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntradas").value(5000.0))
                .andExpect(jsonPath("$.totalSaidas").value(600.5))
                .andExpect(jsonPath("$.saldo").value(4399.5))
                .andExpect(jsonPath("$.porCategoria.length()").value(2));

        mvc.perform(get("/transacoes").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].categoria").isNotEmpty());

        // filtra pelo nome porque categorias compartilhadas (de outros testes) também aparecem na lista
        mvc.perform(get("/categorias").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nome == 'Salario')].totalTransacoes").value(1))
                .andExpect(jsonPath("$[?(@.nome == 'Mercado')].totalTransacoes").value(1));
    }

    @Test
    void meDevolveOUsuarioLogadoSemExporHash() throws Exception {
        String token = novoUsuarioComToken();

        mvc.perform(get("/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Usuario"))
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void editarEExcluirTransacaoDoProprioUsuario() throws Exception {
        String token = novoUsuarioComToken();
        long categoria = criarCategoria(token, "Geral");
        long transacao = criarTransacao(token, "100.00", "SAIDA", categoria);

        mvc.perform(json(put("/transacoes/" + transacao), corpoTransacao("250.00", "ENTRADA", categoria))
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(250.0))
                .andExpect(jsonPath("$.tipo").value("ENTRADA"));

        mvc.perform(delete("/transacoes/" + transacao).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/transacoes").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void renomearECategoria() throws Exception {
        String token = novoUsuarioComToken();
        long categoria = criarCategoria(token, "Antigo");

        mvc.perform(json(put("/categorias/" + categoria), "{\"nome\":\"Novo\"}").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Novo"));
    }

    // ---------- autenticação ----------

    @Test
    void rotasProtegidasExigemToken() throws Exception {
        mvc.perform(get("/transacoes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/categorias")).andExpect(status().isUnauthorized());
        mvc.perform(get("/transacoes/resumo").param("inicio", "2026-10-01").param("fim", "2026-10-31"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/categorias"), "{\"nome\":\"X\"}")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/transacoes/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoEhRejeitado() throws Exception {
        mvc.perform(get("/transacoes").header("Authorization", "Bearer isto.nao.eh.um.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pingContinuaPublico() throws Exception {
        mvc.perform(get("/ping")).andExpect(status().isOk());
    }

    @Test
    void emailRepetidoDa409MesmoComCaixaDiferente() throws Exception {
        String email = emailUnico();
        registrar("Primeiro", email, "senha1234");

        mvc.perform(comIpProprio(json(post("/auth/registro"),
                        "{\"nome\":\"Segundo\",\"email\":\"" + email.toUpperCase() + "\",\"senha\":\"senha1234\"}")))
                .andExpect(status().isConflict());
    }

    @Test
    void loginComSenhaErradaDa401() throws Exception {
        String email = emailUnico();
        registrar("Usuario", email, "senha1234");

        mvc.perform(comIpProprio(json(post("/auth/login"), "{\"email\":\"" + email + "\",\"senha\":\"errada123\"}")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sextaFalhaDeLoginDoMesmoEmailDa429() throws Exception {
        String email = emailUnico();
        registrar("Usuario", email, "senha1234");

        for (int i = 0; i < 5; i++) {
            mvc.perform(comIpProprio(json(post("/auth/login"), "{\"email\":\"" + email + "\",\"senha\":\"errada123\"}")))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(comIpProprio(json(post("/auth/login"), "{\"email\":\"" + email + "\",\"senha\":\"senha1234\"}")))
                .andExpect(status().isTooManyRequests());
    }

    // ---------- validação ----------

    @Test
    void cadastroComDadosInvalidosDa400() throws Exception {
        mvc.perform(comIpProprio(json(post("/auth/registro"), "{\"nome\":\"A\",\"email\":\"nao-eh-email\",\"senha\":\"senha1234\"}")))
                .andExpect(status().isBadRequest());
        mvc.perform(comIpProprio(json(post("/auth/registro"), "{\"nome\":\"A\",\"email\":\"a@teste.com\",\"senha\":\"curta\"}")))
                .andExpect(status().isBadRequest());
        mvc.perform(comIpProprio(json(post("/auth/registro"), "{\"nome\":\"\",\"email\":\"a@teste.com\",\"senha\":\"senha1234\"}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transacaoComDadosInvalidosDa400() throws Exception {
        String token = novoUsuarioComToken();
        long categoria = criarCategoria(token, "Geral");

        // sem valor
        mvc.perform(json(post("/transacoes"),
                        "{\"data\":\"2026-10-05\",\"tipo\":\"SAIDA\",\"idCategoria\":" + categoria + ",\"formaPagamento\":\"PIX\"}")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
        // valor negativo
        mvc.perform(json(post("/transacoes"), corpoTransacao("-5", "SAIDA", categoria)).header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
        // mais de 2 casas decimais
        mvc.perform(json(post("/transacoes"), corpoTransacao("10.999", "SAIDA", categoria)).header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
        // tipo inexistente
        mvc.perform(json(post("/transacoes"), corpoTransacao("10", "QUALQUER", categoria)).header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
        // JSON quebrado
        mvc.perform(json(post("/transacoes"), "{valor:").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void categoriaSemNomeDa400() throws Exception {
        String token = novoUsuarioComToken();

        mvc.perform(json(post("/categorias"), "{\"nome\":\"  \"}").header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resumoComPeriodoInvertidoDa400() throws Exception {
        String token = novoUsuarioComToken();

        mvc.perform(get("/transacoes/resumo").param("inicio", "2026-10-31").param("fim", "2026-10-01")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    // ---------- isolamento entre usuários ----------

    @Test
    void usuarioNaoVeNemAlteraDadosDeOutroUsuario() throws Exception {
        String tokenA = novoUsuarioComToken();
        String tokenB = novoUsuarioComToken();
        long categoriaA = criarCategoria(tokenA, "DaA");
        long transacaoA = criarTransacao(tokenA, "100.00", "SAIDA", categoriaA);

        // B não enxerga nada de A
        mvc.perform(get("/transacoes").header("Authorization", bearer(tokenB)))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/categorias").header("Authorization", bearer(tokenB)))
                .andExpect(jsonPath("$[?(@.nome == 'DaA')]").isEmpty());
        mvc.perform(get("/transacoes/resumo").param("inicio", "2026-10-01").param("fim", "2026-10-31")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(jsonPath("$.totalSaidas").value(0))
                .andExpect(jsonPath("$.porCategoria.length()").value(0));

        // B não edita nem exclui o que é de A (404, sem revelar que existe)
        mvc.perform(json(put("/transacoes/" + transacaoA), corpoTransacao("1.00", "SAIDA", categoriaA))
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/transacoes/" + transacaoA).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        mvc.perform(json(put("/categorias/" + categoriaA), "{\"nome\":\"Invadida\"}").header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/categorias/" + categoriaA).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());

        // B não usa a categoria de A numa transação própria
        mvc.perform(json(post("/transacoes"), corpoTransacao("10.00", "SAIDA", categoriaA))
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isForbidden());

        // e A continua com tudo intacto
        mvc.perform(get("/transacoes").header("Authorization", bearer(tokenA)))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/categorias").header("Authorization", bearer(tokenA)))
                .andExpect(jsonPath("$[?(@.nome == 'DaA')]").isNotEmpty());
    }

    // ---------- exclusão de categoria ----------

    @Test
    void categoriaComTransacoesNaoPodeSerExcluida() throws Exception {
        String token = novoUsuarioComToken();
        long categoria = criarCategoria(token, "Usada");
        long transacao = criarTransacao(token, "10.00", "SAIDA", categoria);

        mvc.perform(delete("/categorias/" + categoria).header("Authorization", bearer(token)))
                .andExpect(status().isConflict());

        mvc.perform(delete("/transacoes/" + transacao).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/categorias/" + categoria).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    // ---------- categoria compartilhada ----------

    @Test
    void categoriaCompartilhadaApareceParaTodosMasNaoPodeSerAlterada() throws Exception {
        Categoria compartilhada = categoriaRepository.save(new Categoria("Compartilhada " + UUID.randomUUID(), null));
        String token = novoUsuarioComToken();

        mvc.perform(get("/categorias").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + compartilhada.getId() + ")].totalTransacoes").value(0));

        mvc.perform(json(put("/categorias/" + compartilhada.getId()), "{\"nome\":\"Mudou\"}").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/categorias/" + compartilhada.getId()).header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());

        // mas dá para lançar transações nela
        criarTransacao(token, "20.00", "SAIDA", compartilhada.getId());
        mvc.perform(get("/categorias").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$[?(@.id == " + compartilhada.getId() + ")].totalTransacoes").value(1));
    }

    // ---------- CORS ----------

    @Test
    void preflightDoFrontEAceito() throws Exception {
        mvc.perform(options("/transacoes")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void preflightDeOrigemDesconhecidaEhRecusado() throws Exception {
        mvc.perform(options("/transacoes")
                        .header("Origin", "http://site-malicioso.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
