package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.NewCompilationDto;
import ru.practicum.ewm.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {
    CompilationDto saveCompilation(NewCompilationDto dto);
    void deleteCompilation(Long compId);
    CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request);
    List<CompilationDto> getCompilations(Boolean pinned, int from, int size);
    CompilationDto getCompilation(Long compId);
}