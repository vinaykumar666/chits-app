package com.ygc.security;

import com.ygc.util.LoggingUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * HTTP Request/Response logging filter for observability
 * Logs all incoming requests and outgoing responses
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RequestResponseLoggingFilter implements Filter {

    private final LoggingUtil loggingUtil;
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_ID_ATTRIBUTE = "requestId";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("RequestResponseLoggingFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Generate or retrieve request ID
        String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        httpRequest.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        httpResponse.setHeader(REQUEST_ID_HEADER, requestId);

        long startTime = System.currentTimeMillis();

        try {
            // Log incoming request
            loggingUtil.apiCall(
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    requestId
            );

            if (log.isDebugEnabled()) {
                log.debug("[{}] Query String: {}", requestId, httpRequest.getQueryString());
                log.debug("[{}] Content Type: {}", requestId, httpRequest.getContentType());
                log.debug("[{}] Remote Address: {}", requestId, httpRequest.getRemoteAddr());
            }

            // Continue with the request
            chain.doFilter(request, response);

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Log response
            loggingUtil.apiResponse(
                    httpRequest.getRequestURI(),
                    httpResponse.getStatus(),
                    requestId
            );

            if (log.isDebugEnabled()) {
                log.debug("[{}] Response Time: {} ms", requestId, duration);
            }

            // Log performance metric if request took significant time
            if (duration > 1000) {
                loggingUtil.performanceMetric(
                        httpRequest.getMethod() + " " + httpRequest.getRequestURI(),
                        duration,
                        requestId
                );
            }
        }
    }

    @Override
    public void destroy() {
        log.info("RequestResponseLoggingFilter destroyed");
    }
}

