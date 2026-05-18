package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {
    // Admin
    List<EventFullDto> getEventsByAdmin(List<Long> users, List<EventState> states, List<Long> categories,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size);
    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request);

    // Private
    List<EventShortDto> getEventsByUser(Long userId, int from, int size);
    EventFullDto addEvent(Long userId, NewEventDto dto);
    EventFullDto getEventByUser(Long userId, Long eventId);
    EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest request);

    // Public
    List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                        Boolean onlyAvailable, String sort, int from, int size);
    EventFullDto getPublicEventById(Long id);
}