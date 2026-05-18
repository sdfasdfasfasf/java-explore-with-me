package ru.practicum.ewm.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHit;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsIntegrationService {
    private final StatsClient statsClient;
    @Value("${spring.application.name}")
    private String appName;

    public void saveHit(HttpServletRequest request) {
        EndpointHit hit = EndpointHit.builder()
                .app(appName)
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build();
        statsClient.hit(hit);
    }
}