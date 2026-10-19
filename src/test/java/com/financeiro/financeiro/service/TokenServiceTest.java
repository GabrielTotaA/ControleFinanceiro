package com.financeiro.financeiro.service;

import com.financeiro.financeiro.model.Usuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private static SecretKey chave(String segredo){
        return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static final SecretKey CHAVE = chave("chave-de-teste-com-mais-de-32-caracteres-123456");

    private TokenService tokenService;
    private JwtDecoder decoder;
    private Usuario usuario;

    @BeforeEach
    void preparar(){
        tokenService = new TokenService(new NimbusJwtEncoder(new ImmutableSecret<>(CHAVE)), 60);
        decoder = NimbusJwtDecoder.withSecretKey(CHAVE).macAlgorithm(MacAlgorithm.HS256).build();
        usuario = new Usuario("Gabriel", "g@teste.com", "HASH");
        ReflectionTestUtils.setField(usuario, "id", 42L);
    }

    @Test
    void tokenTemOIdDoUsuarioComoSubjectEValidadeDeUmaHora(){
        Jwt jwt = decoder.decode(tokenService.gerar(usuario));

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(60));
    }

    @Test
    void tokenAdulteradoERejeitado(){
        String token = tokenService.gerar(usuario);
        String adulterado = token.substring(0, token.length() - 3) + "abc";

        assertThatThrownBy(() -> decoder.decode(adulterado)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenAssinadoComOutraChaveERejeitado(){
        TokenService outro = new TokenService(
                new NimbusJwtEncoder(new ImmutableSecret<>(chave("outra-chave-totalmente-diferente-com-32-chars"))), 60);

        assertThatThrownBy(() -> decoder.decode(outro.gerar(usuario))).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenExpiradoERejeitado(){
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("42")
                .issuedAt(agora.minus(Duration.ofHours(2)))
                .expiresAt(agora.minus(Duration.ofHours(1)))
                .build();
        String expirado = new NimbusJwtEncoder(new ImmutableSecret<>(CHAVE))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        assertThatThrownBy(() -> decoder.decode(expirado)).isInstanceOf(JwtException.class);
    }
}
