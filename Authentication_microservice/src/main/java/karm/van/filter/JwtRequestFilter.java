package karm.van.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import karm.van.service.JwtService;
import karm.van.service.MyUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final MyUserDetailsService myUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;

        // Извлекаем токен из заголовка Authorization
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwtToken = authorizationHeader.substring(7); // Убираем "Bearer"
            try {
                username = jwtService.extractUsername(jwtToken); // Извлекаем имя пользователя из Access Token
            } catch (Exception e) {
                // Логируем и обрабатываем ошибку извлечения имени пользователя
                logger.error("Не удалось извлечь имя пользователя из токена", e);
                filterChain.doFilter(request, response);
                return;
            }
        }

        // Проверяем, что имя пользователя извлечено и что аутентификация еще не установлена
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = myUserDetailsService.loadUserByUsername(username);

                // Проверяем валидность Access Token
                if (jwtService.validateAccessToken(jwtToken, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Устанавливаем информацию об аутентификации в SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    request.setAttribute("jwtToken", jwtToken);
                }
            } catch (UsernameNotFoundException e) {
                logger.error("Пользователь не найден: " + username);
            } catch (DisabledException e) {
                logger.error("Пользователь отключен: " + username);
            }
        }

        // Пропускаем запрос дальше по цепочке фильтров
        filterChain.doFilter(request, response);
    }
}