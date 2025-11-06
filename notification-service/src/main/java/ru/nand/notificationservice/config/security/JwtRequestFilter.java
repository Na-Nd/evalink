package ru.nand.notificationservice.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.nand.notificationservice.entity.User;
import ru.nand.notificationservice.entity.UserDetailsImpl;
import ru.nand.notificationservice.util.UserJwtUtil;

import java.io.IOException;

@Slf4j
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private final UserJwtUtil userJwtUtil;

    @Autowired
    public JwtRequestFilter(UserJwtUtil userJwtUtil) {
        this.userJwtUtil = userJwtUtil;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = userJwtUtil.resolveUserTokenFromRequest(request);

        try{
            if (token != null) {
                // Если токен истек
                if(!userJwtUtil.validateExpirationUserToken(token)){
                    log.warn("Токен истек");

                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
                    return;
                }

                // Если токен валиден
                if(userJwtUtil.validateUserToken(token)){
                    String username = userJwtUtil.extractUsername(token);
                    String role = userJwtUtil.extractRole(token);

                    if(username != null && SecurityContextHolder.getContext().getAuthentication() == null){
                        UserDetails userDetails = new UserDetailsImpl(
                                new User(username)
                        );
                        log.debug("Пользователь в сессии: {}", userDetails.getUsername());

                        UsernamePasswordAuthenticationToken authenticationToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                        log.debug("Пользователь {} в КБ", userDetails);
                    } else {
                        log.error("Не получилось достать username: {} или в КБ уже есть пользователь: {}", username, SecurityContextHolder.getContext().getAuthentication().getName());
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Token processing error");
                    }
                } else {
                    log.error("Токен не прошел валидацию или сессия неактивна");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token invalid or session is inactive");
                    return;
                }

            } else {
                log.error("Токен не обнаружен в заголовке");

                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token not found");
                return;
            }
        } catch (Exception e){
            log.error("Ошибка обработки токена: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Token processing error");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
