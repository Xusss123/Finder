package karm.van.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret-key}")
    private String secret_key;

    private final MyUserDetailsService myUserDetailsService;

    // Генерация Access Token (короткое время жизни, например 15 минут)
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("token_type", "access");
        return createToken(claims, userDetails.getUsername(), 1000 * 60 * 15); // 15 минут
    }

    // Генерация Refresh Token (более длинное время жизни, например 7 дней)
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("token_type", "refresh");
        return createToken(claims, userDetails.getUsername(), 1000 * 60 * 60 * 24 * 7); // 7 дней
    }

    // Создание токена
    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(SignatureAlgorithm.HS256, secret_key)
                .compact();
    }

    // Валидация Access Token
    public Boolean validateAccessToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        try {
            return (isAccessToken(token) && username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        }catch (Exception e){
            return false;
        }
    }

    private boolean isAccessToken(String token) {
        Claims claims = extractAllClaims(token);
        return "access".equals(claims.get("token_type"));
    }

    public Boolean validateRefreshToken(String token) {
        // Дополнительная проверка: является ли токен "refresh"
        if (!isRefreshToken(token)) {
            throw new IllegalArgumentException("Provided token is not a refresh token");
        }
        return !isTokenExpired(token);
    }

    private boolean isRefreshToken(String token) {
        Claims claims = extractAllClaims(token);
        return "refresh".equals(claims.get("token_type"));
    }

    // Метод для обновления Access Token с использованием Refresh Token
    public String refreshAccessToken(String refreshToken) {
        if (validateRefreshToken(refreshToken)) {
            String username = extractUsername(refreshToken);
            UserDetails userDetails = myUserDetailsService.loadUserByUsername(username);
            return generateAccessToken(userDetails); // Генерация нового Access Token
        } else {
            throw new RuntimeException("Refresh token is invalid or expired");
        }
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(secret_key).parseClaimsJws(token).getBody();
    }

}

