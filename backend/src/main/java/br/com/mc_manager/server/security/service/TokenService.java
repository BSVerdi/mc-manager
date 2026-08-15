package br.com.mc_manager.server.security.service;

import br.com.mc_manager.server.model.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

  @Value("${mcmanager.secret-key}")
  private String secret;

  public String generateToken(User user) {
    try {
      Algorithm algorithm = Algorithm.HMAC256(secret);

      return JWT.create()
              .withIssuer("Mc-manager")
              .withSubject(user.getUsername())
              .withExpiresAt(expiration(30))
              .sign(algorithm);
    } catch (JWTCreationException ex) {
      throw new RuntimeException("Erro ao gerar token JWT");
    }
  }

  public String generateRefreshToken(User user) {
    try {
      Algorithm algorithm = Algorithm.HMAC256(secret);

      return JWT.create()
              .withIssuer("Mc-manager")
              .withSubject(user.getId().toString())
              .withExpiresAt(expiration(120))
              .sign(algorithm);
    } catch (JWTCreationException ex) {
      throw new RuntimeException("Erro ao gerar token JWT");
    }
  }

  public String verifyToken(String token) {
    DecodedJWT decodedJWT;

    try {
      Algorithm algorithm = Algorithm.HMAC256(secret);

      JWTVerifier verifier = JWT.require(algorithm)
              .withIssuer("Mc-manager")
              .build();

      decodedJWT = verifier.verify(token);

      return decodedJWT.getSubject();
    } catch (JWTCreationException ex) {
      throw new RuntimeException("Erro ao verificar token JWT");
    }
  }

  public Instant expiration(Integer minutes) {
    return LocalDateTime.now().plusMinutes(minutes).toInstant(ZoneOffset.of("-03:00"));
  }

}

