package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.NewCategoryDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CategoryMapper;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.service.CategoryService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryMapper mapper;

    @Override
    @Transactional
    public CategoryDto addCategory(NewCategoryDto dto) {
        Category category = mapper.toCategory(dto);
        return mapper.toCategoryDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        Category category = getCategoryEntity(catId);
        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("The category is not empty");
        }
        categoryRepository.delete(category);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto dto) {
        Category category = getCategoryEntity(catId);
        category.setName(dto.getName());
        return mapper.toCategoryDto(categoryRepository.save(category));
    }

    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        return categoryRepository.findAll(PageRequest.of(from / size, size)).stream()
                .map(mapper::toCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategory(Long catId) {
        return mapper.toCategoryDto(getCategoryEntity(catId));
    }

    private Category getCategoryEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category with id=" + id + " was not found"));
    }
}