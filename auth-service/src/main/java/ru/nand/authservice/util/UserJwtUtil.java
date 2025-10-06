package ru.nand.authservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.nand.authservice.entity.User;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class UserJwtUtil{
    @Value("${jwt.user.secret}")
    private String userSecretKey;
    @Value("${jwt.user.access.expiration}")
    private long accessTokenExpiration;
    @Value("${jwt.user.refresh.expiration}")
    private long refreshTokenExpiration;

    private Key getUserSigningKey(){
        return Keys.hmacShaKeyFor(userSecretKey.getBytes());
    }

    /// Генерация access токена
    public String generateAccessToken(User user){
        Map<String, Object> claims = new HashMap<>();

        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());
        claims.put("token_type", "access");
        claims.put("user_id", user.getId());

        return createToken(claims, user.getUsername(), accessTokenExpiration);
    }

    /// Генерация refresh токена
    public String generateRefreshToken(User user){
        Map<String, Object> claims = new HashMap<>();

        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());
        claims.put("token_type", "refresh");
        claims.put("user_id", user.getId());

        return createToken(claims, user.getUsername(), refreshTokenExpiration);
    }

    /// Создание пользовательского токена
    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getUserSigningKey())
                .compact();
    }

    /// Валидация истечения пользовательского токена
    public boolean validateExpirationUserToken(String token) {
        try{
            Jwts.parserBuilder()
                    .setSigningKey(getUserSigningKey())
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (ExpiredJwtException e){
            log.error("Ошибка валидации истечения токена: {}", e.getMessage());
            return false;
        }
    }

    /// Валидация токена (ну типа общая)
    public boolean validateUserToken(String token) {
        try{
            Jwts.parserBuilder()
                    .setSigningKey(getUserSigningKey())
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (Exception e){
            log.error("Ошибка валидации токена: {}", e.getMessage());
            return false;
        }
    }


    /// Отсечение
    public String resolveUserToken(HttpServletRequest request){
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        return null;
    }

    /// Получить все данные из пользовательского токена
    private Claims extractAllClaims(String token) {
        try{
            return Jwts.parserBuilder()
                    .setSigningKey(getUserSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e){
            log.warn("Не удалось извлечь данные из токена: {}", e.getMessage());
            return null;
        }
    }

    /// Извлечь конкретные данные из пользовательского токена
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) throws RuntimeException {
        final Claims claims = extractAllClaims(token);
        if(claims == null){
            throw new RuntimeException("Failed to parse JWT");
        }
        return claimsResolver.apply(claims);
    }

    /// Извлечь имя
    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }

    /// Извлечь роль
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /// Извлечь почту
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    /// Хэширование токена по SHA-256
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); // Алгоритм

            byte[] encodedHash = digest.digest(token.getBytes(StandardCharsets.UTF_8)); // Разбиваем троку на байты и прогоняем через sha

            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Ошибка при хэшировании токена: {}", e.getMessage());
            throw new RuntimeException("Token hashing error: " + e.getMessage());
        }
    }
}
