package com.personalblog.ragbackend.knowledge.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class KnowledgeRequestTraceFilter extends OncePerRequestFilter {
    private static final String TRACE_PATH_PATTERN = "/knowledge-base";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!isKnowledgeRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        System.out.println("[KnowledgeAPI] request start: method=" + request.getMethod()
                + ", uri=" + request.getRequestURI()
                + ", query=" + request.getQueryString()
                + ", authPresent=" + (request.getHeader("Authorization") != null));
        log.info(
                "Knowledge API request start: method={}, uri={}, query={}, authPresent={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                request.getHeader("Authorization") != null
        );
        try {
            filterChain.doFilter(request, response);
        } finally {
            System.out.println("[KnowledgeAPI] request end: method=" + request.getMethod()
                    + ", uri=" + request.getRequestURI()
                    + ", status=" + response.getStatus()
                    + ", elapsedMs=" + (System.currentTimeMillis() - startTime));
            log.info(
                    "Knowledge API request end: method={}, uri={}, status={}, elapsedMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    System.currentTimeMillis() - startTime
            );
        }
    }

    private boolean isKnowledgeRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.contains(TRACE_PATH_PATTERN);
    }
}
