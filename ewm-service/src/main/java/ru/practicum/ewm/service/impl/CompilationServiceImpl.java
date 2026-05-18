package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.NewCompilationDto;
import ru.practicum.ewm.dto.UpdateCompilationRequest;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.service.CompilationService;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CompilationDto saveCompilation(NewCompilationDto dto) {
        try {
            log.info("Creating compilation: title={}, pinned={}, eventsCount={}",
                    dto.getTitle(), dto.getPinned(), dto.getEvents() == null ? 0 : dto.getEvents().size());

            Compilation compilation = new Compilation();
            compilation.setTitle(dto.getTitle());
            compilation.setPinned(dto.getPinned() != null ? dto.getPinned() : false);
            if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
                List<Event> events = eventRepository.findAllById(dto.getEvents());
                compilation.setEvents(new HashSet<>(events));
            }
            Compilation saved = compilationRepository.save(compilation);
            return toDto(saved);
        } catch (DataIntegrityViolationException e) {
            log.error("Duplicate title: {}", dto.getTitle(), e);
            throw new ConflictException("Compilation title already exists: " + dto.getTitle());
        } catch (Exception e) {
            log.error("Unexpected error while creating compilation", e);
            throw new RuntimeException("Failed to create compilation: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        Compilation compilation = getCompilationEntity(compId);
        compilationRepository.delete(compilation);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request) {
        Compilation compilation = getCompilationEntity(compId);
        if (request.getTitle() != null) compilation.setTitle(request.getTitle());
        if (request.getPinned() != null) compilation.setPinned(request.getPinned());
        if (request.getEvents() != null) {
            List<Event> events = eventRepository.findAllById(request.getEvents());
            compilation.setEvents(new HashSet<>(events));
        }
        return toDto(compilationRepository.save(compilation));
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        if (pinned != null) {
            return compilationRepository.findByPinned(pinned, page).stream()
                    .map(this::toDto).collect(Collectors.toList());
        } else {
            return compilationRepository.findAll(page).stream()
                    .map(this::toDto).collect(Collectors.toList());
        }
    }

    @Override
    public CompilationDto getCompilation(Long compId) {
        return toDto(getCompilationEntity(compId));
    }

    private Compilation getCompilationEntity(Long id) {
        return compilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + id + " was not found"));
    }

    private CompilationDto toDto(Compilation compilation) {
        CompilationDto dto = new CompilationDto();
        dto.setId(compilation.getId());
        dto.setTitle(compilation.getTitle());
        dto.setPinned(compilation.getPinned());
        // events mapping omitted here – add if needed
        return dto;
    }
}