package ru.practicum.stats.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class StatsClient {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${stats-server.url:http://localhost:9090}")
    private String serverUrl;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void hit(EndpointHit hit) {
        try {
            String url = serverUrl + "/hit";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<EndpointHit> request = new HttpEntity<>(hit, headers);
            restTemplate.postForEntity(url, request, Void.class);
            log.info("Sent hit: {}", hit);
        } catch (Exception e) {
            log.error("Failed to send hit: {}", e.getMessage(), e);
        }
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        try {
            String startStr = start.format(FORMATTER);
            String endStr = end.format(FORMATTER);
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serverUrl + "/stats")
                    .queryParam("start", startStr)
                    .queryParam("end", endStr)
                    .queryParam("unique", unique);
            if (uris != null && !uris.isEmpty()) {
                uris.forEach(uri -> builder.queryParam("uris", uri));
            }
            String url = builder.build().toUriString();
            ViewStats[] response = restTemplate.getForObject(url, ViewStats[].class);
            return response != null ? Arrays.asList(response) : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to get stats: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}