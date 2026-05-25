package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.ForbiddenException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CategoryMapper;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.mapper.LocationMapper;
import ru.practicum.ewm.mapper.UserMapper;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.service.EventService;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;
    private final EventMapper eventMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final LocationMapper locationMapper;
    private final StatsClient statsClient;

    // ==================== ADMIN ====================
    @Override
    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<EventState> states, List<Long> categories, LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        if (rangeStart == null) rangeStart = LocalDateTime.now();
        if (rangeEnd == null) rangeEnd = rangeStart.plusYears(100);
        PageRequest page = PageRequest.of(from / size, size);
        return eventRepository.findForAdmin(users, states, categories, rangeStart, rangeEnd, page).stream().map(this::enrichWithStats).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = getEventById(eventId);
        if (request.getStateAction() != null) {
            if (request.getStateAction().equals("PUBLISH_EVENT")) {
                if (event.getState() != EventState.PENDING)
                    throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (request.getStateAction().equals("REJECT_EVENT")) {
                if (event.getState() == EventState.PUBLISHED)
                    throw new ConflictException("Cannot reject already published event");
                event.setState(EventState.CANCELED);
            }
        }
        updateEventFromAdmin(request, event);
        return enrichWithStats(eventRepository.save(event));
    }

    // ==================== PRIVATE ====================
    @Override
    public List<EventShortDto> getEventsByUser(Long userId, int from, int size) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
        PageRequest page = PageRequest.of(from / size, size);
        return eventRepository.findByInitiatorId(userId, page).stream().map(e -> enrichWithStats(e, true)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto dto) {
        log.info("Adding event for user id={}, category={}", userId, dto.getCategory());
        User initiator = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));
        Category category = categoryRepository.findById(dto.getCategory()).orElseThrow(() -> new NotFoundException("Category with id=" + dto.getCategory() + " not found"));
        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Event date must be at least 2 hours from now");
        }
        Event event = eventMapper.toEvent(dto);
        event.setInitiator(initiator);
        event.setCategory(category);
        event.setLocation(locationMapper.toLocation(dto.getLocation()));
        event.setPaid(dto.getPaid() != null ? dto.getPaid() : false);
        event.setParticipantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0);
        event.setRequestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true);
        event.setState(EventState.PENDING);
        Event saved = eventRepository.save(event);
        log.debug("Event created with id={}, state={}", saved.getId(), saved.getState());
        return enrichWithStats(saved);
    }

    @Override
    public EventFullDto getEventByUser(Long userId, Long eventId) {
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId).orElseThrow(() -> new NotFoundException("Event not found"));
        return enrichWithStats(event);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest request) {
        log.info("User {} updating event {}, action={}", userId, eventId, request.getStateAction());
        Event event = getEventById(eventId);
        if (!event.getInitiator().getId().equals(userId)) throw new ForbiddenException("Not the initiator");
        if (event.getState() == EventState.PUBLISHED)
            throw new ConflictException("Only pending or canceled events can be changed");
        if (request.getStateAction() != null) {
            if (request.getStateAction().equals("SEND_TO_REVIEW")) {
                event.setState(EventState.PENDING);
            } else if (request.getStateAction().equals("CANCEL_REVIEW")) {
                event.setState(EventState.CANCELED);
            }
        }
        if (request.getEventDate() != null && request.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Event date must be at least 2 hours from now");
        }
        updateEventFromUser(request, event);
        log.debug("Event {} updated, new state={}", eventId, event.getState());
        return enrichWithStats(eventRepository.save(event));
    }

    // ==================== PUBLIC ====================
    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid, LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable, String sort, int from, int size) {
        if (rangeStart == null) rangeStart = LocalDateTime.now();
        if (rangeEnd == null) rangeEnd = rangeStart.plusYears(100);
        Sort sortBy = "VIEWS".equals(sort) ? Sort.by("views").descending() : Sort.by("eventDate").ascending();
        PageRequest page = PageRequest.of(from / size, size, sortBy);
        Page<Event> eventsPage;
        if (text == null || text.isBlank()) {
            eventsPage = eventRepository.findForPublicWithoutText(categories, paid, rangeStart, rangeEnd, onlyAvailable, page);
        } else {
            eventsPage = eventRepository.findForPublic(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, page);
        }
        return eventsPage.stream().map(e -> enrichWithStats(e, true)).collect(Collectors.toList());
    }

    @Override
    public EventFullDto getPublicEventById(Long id) {
        Event event = getEventById(id);
        if (event.getState() != EventState.PUBLISHED) throw new NotFoundException("Event not published");
        return enrichWithStats(event);
    }

    // ==================== HELPER METHODS ====================
    private Event getEventById(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));
    }

    private Long getViewsForEvent(Event event) {
        LocalDateTime start = event.getPublishedOn() != null ? event.getPublishedOn() : event.getCreatedOn();
        LocalDateTime end = LocalDateTime.now();
        try {
            List<ViewStats> stats = statsClient.getStats(start, end, List.of("/events/" + event.getId()), true);
            return stats.isEmpty() ? 0L : stats.get(0).getHits();
        } catch (Exception e) {
            log.warn("Failed to get views for event {}", event.getId(), e);
            return 0L;
        }
    }

    private EventFullDto enrichWithStats(Event event) {
        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setConfirmedRequests(requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));
        try {
            dto.setViews(getViewsForEvent(event));
        } catch (Exception e) {
            log.warn("Failed to get views for event {}: {}", event.getId(), e.getMessage());
            dto.setViews(0L);
        }
        return dto;
    }

    private EventShortDto enrichWithStats(Event event, boolean shortVersion) {
        EventShortDto dto = eventMapper.toShortDto(event);
        dto.setConfirmedRequests(requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));
        try {
            dto.setViews(getViewsForEvent(event));
        } catch (Exception e) {
            log.warn("Failed to get views for event {}: {}", event.getId(), e.getMessage());
            dto.setViews(0L);
        }
        return dto;
    }

    private void updateEventFromUser(UpdateEventUserRequest request, Event event) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getCategory() != null) {
            Category cat = categoryRepository.findById(request.getCategory()).orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(cat);
        }
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
        if (request.getLocation() != null) event.setLocation(locationMapper.toLocation(request.getLocation()));
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
    }

    private void updateEventFromAdmin(UpdateEventAdminRequest request, Event event) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getCategory() != null) {
            Category cat = categoryRepository.findById(request.getCategory()).orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(cat);
        }
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
        if (request.getLocation() != null) event.setLocation(locationMapper.toLocation(request.getLocation()));
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
    }
}