package com.financeiro.financeiro.service;

import com.financeiro.financeiro.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final long expiracaoMinutos;

    public TokenService(JwtEncoder encoder, @Value("${app.jwt.expiracao-minutos}") long expiracaoMinutos){
        this.encoder = encoder;
        this.expiracaoMinutos = expiracaoMinutos;
    }

    public String gerar(Usuario usuario){
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(agora)
                .expiresAt(agora.plus(expiracaoMinutos, ChronoUnit.MINUTES))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
