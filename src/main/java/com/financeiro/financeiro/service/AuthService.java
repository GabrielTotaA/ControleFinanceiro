package com.financeiro.financeiro.service;

import com.financeiro.financeiro.dto.LoginResponseDTO;
import com.financeiro.financeiro.dto.UsuarioResponseDTO;
import com.financeiro.financeiro.exception.CredenciaisInvalidasException;
import com.financeiro.financeiro.exception.DadosInvalidosException;
import com.financeiro.financeiro.exception.EmailJaCadastradoException;
import com.financeiro.financeiro.exception.UsuarioNaoEncontradoException;
import com.financeiro.financeiro.model.Usuario;
import com.financeiro.financeiro.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class AuthService {

    private static final int TAMANHO_MINIMO_SENHA = 8;
    private static final int TAMANHO_MAXIMO_SENHA_BYTES = 72;

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    private final LimitadorTentativas limiteLoginPorIp = new LimitadorTentativas(20, Duration.ofMinutes(15));
    private final LimitadorTentativas limiteLoginPorEmail = new LimitadorTentativas(5, Duration.ofMinutes(15));
    private final LimitadorTentativas limiteRegistroPorIp = new LimitadorTentativas(10, Duration.ofHours(1));

    public AuthService(UsuarioRepository repository, PasswordEncoder passwordEncoder, TokenService tokenService){
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public UsuarioResponseDTO registrar(String nome, String email, String senha, String ip){
        limiteRegistroPorIp.verificar(ip);
        limiteRegistroPorIp.registrar(ip);
        if (nome == null || nome.isBlank() || email == null || email.isBlank()){
            throw new DadosInvalidosException("Nome e e-mail são obrigatórios");
        }
        if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA){
            throw new DadosInvalidosException("A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > TAMANHO_MAXIMO_SENHA_BYTES){
            throw new DadosInvalidosException("A senha é muito longa (máximo de " + TAMANHO_MAXIMO_SENHA_BYTES + " bytes)");
        }
        String emailNormalizado = normalizar(email);
        if (repository.existsByEmail(emailNormalizado)){
            throw new EmailJaCadastradoException("Já existe uma conta com esse e-mail");
        }
        Usuario usuario = new Usuario(nome.trim(), emailNormalizado, passwordEncoder.encode(senha));
        try {
            return UsuarioResponseDTO.de(repository.save(usuario));
        } catch (DataIntegrityViolationException e){
            throw new EmailJaCadastradoException("Já existe uma conta com esse e-mail");
        }
    }

    public LoginResponseDTO login(String email, String senha, String ip){
        String chaveEmail = email == null ? "" : normalizar(email);
        limiteLoginPorIp.verificar(ip);
        limiteLoginPorEmail.verificar(chaveEmail);
        Usuario usuario;
        try {
            usuario = repository.findByEmail(chaveEmail)
                    .filter(u -> senha != null && u.getSenhaHash() != null && passwordEncoder.matches(senha, u.getSenhaHash()))
                    .orElseThrow(() -> new CredenciaisInvalidasException("E-mail ou senha inválidos"));
        } catch (CredenciaisInvalidasException e){
            limiteLoginPorIp.registrar(ip);
            limiteLoginPorEmail.registrar(chaveEmail);
            throw e;
        }
        limiteLoginPorEmail.limpar(chaveEmail);
        return new LoginResponseDTO(tokenService.gerar(usuario), UsuarioResponseDTO.de(usuario));
    }

    public UsuarioResponseDTO buscarUsuario(Long idUsuario){
        return repository.findById(idUsuario)
                .map(UsuarioResponseDTO::de)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado: " + idUsuario));
    }

    private String normalizar(String email){
        return email.trim().toLowerCase();
    }
}
