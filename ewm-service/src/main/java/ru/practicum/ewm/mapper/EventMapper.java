package ru.practicum.ewm.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class EventMapper {

    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final LocationMapper locationMapper;

    public Event toEvent(NewEventDto dto) {
        if (dto == null) return null;
        return Event.builder().annotation(dto.getAnnotation()).description(dto.getDescription()).eventDate(dto.getEventDate()).location(locationMapper.toLocation(dto.getLocation())).paid(dto.getPaid() != null ? dto.getPaid() : false).participantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0).requestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true).title(dto.getTitle()).state(EventState.PENDING).createdOn(LocalDateTime.now()).build();
    }

    public EventShortDto toShortDto(Event event) {
        if (event == null) return null;
        return EventShortDto.builder().id(event.getId()).annotation(event.getAnnotation()).category(categoryMapper.toCategoryDto(event.getCategory())).eventDate(event.getEventDate()).initiator(userMapper.toUserShortDto(event.getInitiator())).paid(event.getPaid()).title(event.getTitle()).build();
    }

    public EventFullDto toFullDto(Event event) {
        if (event == null) return null;
        return EventFullDto.builder().id(event.getId()).annotation(event.getAnnotation()).category(categoryMapper.toCategoryDto(event.getCategory())).createdOn(event.getCreatedOn()).description(event.getDescription()).eventDate(event.getEventDate()).initiator(userMapper.toUserShortDto(event.getInitiator())).location(locationMapper.toLocationDto(event.getLocation())).paid(event.getPaid()).participantLimit(event.getParticipantLimit()).publishedOn(event.getPublishedOn()).requestModeration(event.getRequestModeration()).state(event.getState()).title(event.getTitle()).build();
    }

    public void updateFromUserRequest(UpdateEventUserRequest request, Event event) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
        if (request.getLocation() != null) event.setLocation(locationMapper.toLocation(request.getLocation()));
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
    }

    public void updateFromAdminRequest(UpdateEventAdminRequest request, Event event) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
        if (request.getLocation() != null) event.setLocation(locationMapper.toLocation(request.getLocation()));
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
    }
}