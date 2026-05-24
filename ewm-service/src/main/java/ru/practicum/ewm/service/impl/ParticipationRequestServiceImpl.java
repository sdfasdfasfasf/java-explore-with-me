package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.ParticipationRequestDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.ForbiddenException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.ParticipationRequestMapper;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.UserRepository;
import ru.practicum.ewm.service.ParticipationRequestService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestMapper mapper;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.debug("Getting requests for user id={}", userId);
        userExists(userId);
        List<ParticipationRequestDto> result = requestRepository.findByRequesterId(userId).stream()
                .map(mapper::toDto).collect(Collectors.toList());
        log.debug("Found {} requests for user {}", result.size(), userId);
        return result;
    }

    @Override
    @Transactional
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        log.info("User {} requesting participation in event {}", userId, eventId);
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: id={}", userId);
                    return new NotFoundException("User not found");
                });
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Event not found: id={}", eventId);
                    return new NotFoundException("Event not found");
                });
        if (event.getInitiator().getId().equals(userId)) {
            log.warn("Initiator cannot request own event: userId={}, eventId={}", userId, eventId);
            throw new ConflictException("Initiator cannot request participation");
        }
        if (event.getState() != EventState.PUBLISHED) {
            log.warn("Event not published: eventId={}, state={}", eventId, event.getState());
            throw new ConflictException("Event not published");
        }
        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            log.warn("Duplicate request: eventId={}, userId={}", eventId, userId);
            throw new ConflictException("Request already exists");
        }
        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmed >= event.getParticipantLimit()) {
            log.warn("Participant limit reached for event {}: limit={}", eventId, event.getParticipantLimit());
            throw new ConflictException("Participant limit reached");
        }
        ParticipationRequest request = new ParticipationRequest();
        request.setCreated(LocalDateTime.now());
        request.setRequester(requester);
        request.setEvent(event);
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }
        ParticipationRequest saved = requestRepository.save(request);
        log.debug("Request created: id={}, status={}", saved.getId(), saved.getStatus());
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        request.setStatus(RequestStatus.CANCELED);
        return mapper.toDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException("Only initiator can view participants");
        }
        return requestRepository.findByEventId(eventId).stream()
                .map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest requestDto) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        if (event.getParticipantLimit() == 0 || !event.getRequestModeration())
            throw new ConflictException("Moderation disabled or limit zero");
        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (confirmed >= event.getParticipantLimit())
            throw new ConflictException("Limit already reached");
        List<ParticipationRequest> requests = requestRepository.findAllById(requestDto.getRequestIds());
        List<ParticipationRequestDto> confirmedList = new ArrayList<>();
        List<ParticipationRequestDto> rejectedList = new ArrayList<>();
        for (ParticipationRequest req : requests) {
            if (req.getStatus() != RequestStatus.PENDING)
                throw new ConflictException("Only pending requests can be changed");
            if (requestDto.getStatus() == RequestStatus.CONFIRMED) {
                if (confirmed < event.getParticipantLimit()) {
                    req.setStatus(RequestStatus.CONFIRMED);
                    confirmed++;
                    confirmedList.add(mapper.toDto(req));
                } else {
                    req.setStatus(RequestStatus.REJECTED);
                    rejectedList.add(mapper.toDto(req));
                }
            } else if (requestDto.getStatus() == RequestStatus.REJECTED) {
                req.setStatus(RequestStatus.REJECTED);
                rejectedList.add(mapper.toDto(req));
            }
        }
        requestRepository.saveAll(requests);
        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedList)
                .rejectedRequests(rejectedList)
                .build();
    }

    private void userExists(Long userId) {
        if (!userRepository.existsById(userId))
            throw new NotFoundException("User not found");
    }
}