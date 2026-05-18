package ru.practicum.ewm.controller.public_api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.EventFullDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.service.EventService;
import ru.practicum.ewm.service.StatsIntegrationService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {
    private final EventService eventService;
    private final StatsIntegrationService statsService;

    @GetMapping
    public List<EventShortDto> getEvents(@RequestParam(required = false) String text, @RequestParam(required = false) List<Long> categories, @RequestParam(required = false) Boolean paid, @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart, @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd, @RequestParam(defaultValue = "false") Boolean onlyAvailable, @RequestParam(required = false) String sort, @RequestParam(defaultValue = "0") int from, @RequestParam(defaultValue = "10") int size, HttpServletRequest request) {
        // Отправка статистики
        statsService.saveHit(request);
        return eventService.getPublicEvents(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);
    }

    @GetMapping("/{id}")
    public EventFullDto getEvent(@PathVariable Long id, HttpServletRequest request) {
        statsService.saveHit(request);
        return eventService.getPublicEventById(id);
    }
}