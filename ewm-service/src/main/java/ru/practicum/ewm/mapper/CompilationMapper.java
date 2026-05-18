package ru.practicum.ewm.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.NewCompilationDto;
import ru.practicum.ewm.model.Compilation;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CompilationMapper {

    private final EventMapper eventMapper;

    public Compilation toCompilation(NewCompilationDto dto) {
        if (dto == null) return null;
        return Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.getPinned() != null ? dto.getPinned() : false)
                .build();
    }

    public CompilationDto toCompilationDto(Compilation compilation) {
        if (compilation == null) return null;
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(compilation.getEvents().stream()
                        .map(eventMapper::toShortDto)
                        .collect(Collectors.toList()))
                .build();
    }
}