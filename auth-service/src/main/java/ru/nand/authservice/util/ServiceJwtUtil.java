package ru.nand.authservice.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Component
public class ServiceJwtUtil {
    @Value("${jwt.service.secret}")
    private String serviceSecretKey;
    @Value("${jwt.service.expiration}")
    private long serviceExpiration;
    @Value("${spring.application.name}")
    private String appName;

    private Key getServiceSigningKey(){
        return Keys.hmacShaKeyFor(serviceSecretKey.getBytes());
    }

    /// Генерация сервисного токена
    public String generateServiceToken(){
        Map<String, Object> claims = new HashMap<>();

        claims.put("role", "service"); // Проверять на стороне принимающего
        claims.put("service_name", appName);
        claims.put("token_type", "service_token"); // Проверять на стороне принимающего

        return createServiceToken(claims, serviceExpiration);
    }

    /// Создание сервисного токена
    private String createServiceToken(Map<String, Object> claims, long expirationTime){
        return Jwts.builder()
                .setClaims(claims)
                .setSubject("auth-service")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getServiceSigningKey())
                .compact();
    }

    /// Отсечение
    public String resolveServiceToken(HttpServletRequest request){
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        return null;
    }
}
