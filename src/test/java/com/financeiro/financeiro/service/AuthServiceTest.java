package com.financeiro.financeiro.service;

import com.financeiro.financeiro.dto.LoginResponseDTO;
import com.financeiro.financeiro.dto.UsuarioResponseDTO;
import com.financeiro.financeiro.exception.CredenciaisInvalidasException;
import com.financeiro.financeiro.exception.DadosInvalidosException;
import com.financeiro.financeiro.exception.EmailJaCadastradoException;
import com.financeiro.financeiro.exception.MuitasTentativasException;
import com.financeiro.financeiro.model.Usuario;
import com.financeiro.financeiro.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository repository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;

    private AuthService service;

    @BeforeEach
    void preparar(){
        service = new AuthService(repository, passwordEncoder, tokenService);
    }

    // ---------- registro ----------

    @Test
    void registrarSalvaComHashEEmailNormalizado(){
        when(repository.existsByEmail("gabriel@teste.com")).thenReturn(false);
        when(passwordEncoder.encode("senha1234")).thenReturn("HASH");
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponseDTO resposta = service.registrar("  Gabriel  ", "  Gabriel@Teste.COM ", "senha1234", "1.1.1.1");

        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(capturado.capture());
        assertThat(capturado.getValue().getEmail()).isEqualTo("gabriel@teste.com");
        assertThat(capturado.getValue().getNome()).isEqualTo("Gabriel");
        assertThat(capturado.getValue().getSenhaHash()).isEqualTo("HASH");
        assertThat(resposta.nome()).isEqualTo("Gabriel");
    }

    @Test
    void registrarRecusaEmailRepetido(){
        when(repository.existsByEmail("a@teste.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar("A", "a@teste.com", "senha1234", "1.1.1.1"))
                .isInstanceOf(EmailJaCadastradoException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void registrarConverteCorridaNoBancoEmEmailJaCadastrado(){
        when(repository.existsByEmail("a@teste.com")).thenReturn(false);
        when(passwordEncoder.encode("senha1234")).thenReturn("HASH");
        when(repository.save(any(Usuario.class))).thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> service.registrar("A", "a@teste.com", "senha1234", "1.1.1.1"))
                .isInstanceOf(EmailJaCadastradoException.class);
    }

    @Test
    void registrarRecusaSenhaCurta(){
        assertThatThrownBy(() -> service.registrar("A", "a@teste.com", "1234567", "1.1.1.1"))
                .isInstanceOf(DadosInvalidosException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void registrarRecusaSenhaAcimaDe72Bytes(){
        assertThatThrownBy(() -> service.registrar("A", "a@teste.com", "a".repeat(73), "1.1.1.1"))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void registrarRecusaSenhaComPoucosCaracteresMasMuitosBytes(){
        // 40 caracteres de 2 bytes = 80 bytes: passa no tamanho em caracteres, mas excede o limite do BCrypt
        assertThatThrownBy(() -> service.registrar("A", "a@teste.com", "ã".repeat(40), "1.1.1.1"))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void registrarRecusaNomeOuEmailEmBranco(){
        assertThatThrownBy(() -> service.registrar("  ", "a@teste.com", "senha1234", "1.1.1.1"))
                .isInstanceOf(DadosInvalidosException.class);
        assertThatThrownBy(() -> service.registrar("A", " ", "senha1234", "1.1.1.1"))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void registrarBloqueiaIpAposDezTentativas(){
        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> service.registrar(" ", " ", "x", "9.9.9.9"))
                    .isInstanceOf(DadosInvalidosException.class);
        }

        assertThatThrownBy(() -> service.registrar("A", "a@teste.com", "senha1234", "9.9.9.9"))
                .isInstanceOf(MuitasTentativasException.class);
        // outro IP não é afetado
        assertThatThrownBy(() -> service.registrar(" ", " ", "x", "8.8.8.8"))
                .isInstanceOf(DadosInvalidosException.class);
    }

    // ---------- login ----------

    private Usuario usuarioComSenha(){
        return new Usuario("Gabriel", "g@teste.com", "HASH");
    }

    @Test
    void loginComCredenciaisCertasDevolveTokenEUsuario(){
        Usuario usuario = usuarioComSenha();
        when(repository.findByEmail("g@teste.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha1234", "HASH")).thenReturn(true);
        when(tokenService.gerar(usuario)).thenReturn("TOKEN");

        LoginResponseDTO resposta = service.login("G@Teste.com", "senha1234", "1.1.1.1");

        assertThat(resposta.token()).isEqualTo("TOKEN");
        assertThat(resposta.usuario().nome()).isEqualTo("Gabriel");
    }

    @Test
    void loginComSenhaErradaFalha(){
        when(repository.findByEmail("g@teste.com")).thenReturn(Optional.of(usuarioComSenha()));
        when(passwordEncoder.matches("errada", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> service.login("g@teste.com", "errada", "1.1.1.1"))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("E-mail ou senha inválidos");
    }

    @Test
    void loginComEmailInexistenteDaAMesmaMensagemDaSenhaErrada(){
        when(repository.findByEmail("x@teste.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("x@teste.com", "qualquer", "1.1.1.1"))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("E-mail ou senha inválidos");
    }

    @Test
    void loginDeUsuarioAntigoSemSenhaFalha(){
        Usuario semSenha = new Usuario("Antigo", "antigo@teste.com", null);
        when(repository.findByEmail("antigo@teste.com")).thenReturn(Optional.of(semSenha));

        assertThatThrownBy(() -> service.login("antigo@teste.com", "senha1234", "1.1.1.1"))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void loginBloqueiaOEmailNaSextaFalha(){
        when(repository.findByEmail("g@teste.com")).thenReturn(Optional.empty());

        for (int i = 0; i < 5; i++) {
            String ip = "10.0.0." + i; // IPs diferentes: só o limite por e-mail entra em ação
            assertThatThrownBy(() -> service.login("g@teste.com", "errada", ip))
                    .isInstanceOf(CredenciaisInvalidasException.class);
        }

        assertThatThrownBy(() -> service.login("g@teste.com", "errada", "10.0.0.99"))
                .isInstanceOf(MuitasTentativasException.class);
    }

    @Test
    void loginBloqueiaOIpApos20Falhas(){
        when(repository.findByEmail(any())).thenReturn(Optional.empty());

        for (int i = 0; i < 20; i++) {
            String email = "u" + i + "@teste.com"; // e-mails diferentes: só o limite por IP entra em ação
            assertThatThrownBy(() -> service.login(email, "errada", "7.7.7.7"))
                    .isInstanceOf(CredenciaisInvalidasException.class);
        }

        assertThatThrownBy(() -> service.login("novo@teste.com", "errada", "7.7.7.7"))
                .isInstanceOf(MuitasTentativasException.class);
    }

    @Test
    void loginComSucessoZeraOContadorDoEmail(){
        Usuario usuario = usuarioComSenha();
        when(repository.findByEmail("g@teste.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "HASH")).thenReturn(false);
        when(passwordEncoder.matches("certa", "HASH")).thenReturn(true);
        when(tokenService.gerar(usuario)).thenReturn("TOKEN");

        for (int i = 0; i < 4; i++) {
            String ip = "10.1.0." + i;
            assertThatThrownBy(() -> service.login("g@teste.com", "errada", ip))
                    .isInstanceOf(CredenciaisInvalidasException.class);
        }
        assertThat(service.login("g@teste.com", "certa", "10.1.0.50").token()).isEqualTo("TOKEN");

        // sem o reset, a 2ª falha já seria bloqueada (4 + 1 = 5)
        for (int i = 0; i < 5; i++) {
            String ip = "10.2.0." + i;
            assertThatThrownBy(() -> service.login("g@teste.com", "errada", ip))
                    .isInstanceOf(CredenciaisInvalidasException.class);
        }
    }
}
