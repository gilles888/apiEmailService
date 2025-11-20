package com.gilmotech.emailservice.config;


import com.gilmotech.emailservice.dto.MailResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitingFilter implements Filter {

    @Value("${app.mail.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.mail.rate-limit.max-per-hour:10}")
    private int maxRequestsPerHour;

    // Map: IP -> List<Timestamp>
    private final Map<String, java.util.Queue<LocalDateTime>> requestCounts =
            new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!rateLimitEnabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Seulement pour les endpoints d'envoi de mail
        if (!httpRequest.getRequestURI().startsWith("/api/mail/send")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(httpRequest);

        if (isRateLimited(clientIp)) {
            log.warn("Rate limit dépassé pour l'IP: {}", clientIp);

            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

            MailResponseDto errorResponse = MailResponseDto.error(
                    "Trop de requêtes. Veuillez réessayer plus tard.",
                    "RATE_LIMIT_EXCEEDED"
            );

            httpResponse.getWriter().write(
                    objectMapper.writeValueAsString(errorResponse)
            );
            return;
        }

        recordRequest(clientIp);
        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private boolean isRateLimited(String clientIp) {
        java.util.Queue<LocalDateTime> requests = requestCounts.get(clientIp);

        if (requests == null) {
            return false;
        }

        // Nettoyer les requêtes de plus d'une heure
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        requests.removeIf(timestamp -> timestamp.isBefore(oneHourAgo));

        return requests.size() >= maxRequestsPerHour;
    }

    private void recordRequest(String clientIp) {
        requestCounts.computeIfAbsent(clientIp, k -> new java.util.LinkedList<>())
                .add(LocalDateTime.now());
    }
}