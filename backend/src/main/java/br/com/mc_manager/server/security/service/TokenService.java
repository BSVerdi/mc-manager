package br.com.mc_manager.server.security.service;

import br.com.mc_manager.server.model.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {

  private static final String ISSUER = "mc-manager";
  private static final String TYPE_CLAIM = "type";
  @Value("${mcmanager.secret_key}")
  private String secret;

  public String generateAccessToken(User user) {
    return generateToken(user, "access", 30);
  }

  public String generateRefreshToken(User user) {
    return generateToken(user, "refresh", 120);
  }

  public String generateToken(User user, String type, Integer minutes) {
    try {
      Algorithm algorithm = Algorithm.HMAC256(secret);

      return JWT.create()
              .withIssuer(ISSUER)
              .withSubject(user.getUsername())
              .withClaim(TYPE_CLAIM, type)
              .withExpiresAt(expiration(minutes))
              .sign(algorithm);
    } catch (JWTCreationException ex) {
      throw new RuntimeException("Erro ao gerar token JWT");
    }
  }

  public String verifyAccessToken(String token) {
    return verifyToken(token, "access");
  }

  public String verifyRefreshToken(String token) {
    return verifyToken(token, "refresh");
  }

  public String verifyToken(String token, String type) {
    try {
      Algorithm algorithm = Algorithm.HMAC256(secret);
      JWTVerifier verifier = JWT.require(algorithm)
              .withIssuer(ISSUER)
              .build();

      DecodedJWT decodedJWT = verifier.verify(token);


      String actualType = decodedJWT.getClaim(TYPE_CLAIM).asString();

      if (!type.equals(actualType)) {
        throw new JWTVerificationException("Token invalido");
      }

      return decodedJWT.getSubject();
    } catch (JWTVerificationException ex) {
      throw new BadCredentialsException("Token JWT inválido ou expirado: " + ex.getMessage(), ex);
    }
  }

  public Instant expiration(Integer minutes) {
    return Instant.now().plus(minutes, ChronoUnit.MINUTES);
  }

}

