package ru.practicum.ewm.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.NewCategoryDto;
import ru.practicum.ewm.exception.BadRequestException;
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
    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryMapper mapper;

    @Override
    @Transactional
    public CategoryDto addCategory(NewCategoryDto dto) {
        log.info("Adding new category: name={}", dto.getName());
        try {
            Category category = mapper.toCategory(dto);
            Category saved = categoryRepository.save(category);
            log.debug("Category saved with id={}", saved.getId());
            return mapper.toCategoryDto(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate category name: {}", dto.getName());
            throw new ConflictException("Category name already exists: " + dto.getName());
        }
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        log.info("Deleting category id={}", catId);
        Category category = getCategoryEntity(catId);
        if (eventRepository.existsByCategoryId(catId)) {
            log.warn("Cannot delete category id={} because it has events", catId);
            throw new ConflictException("The category is not empty");
        }
        categoryRepository.delete(category);
        log.debug("Category deleted: id={}", catId);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto dto) {
        log.info("Updating category id={} with new name={}", catId, dto.getName());
        Category category = getCategoryEntity(catId);
        if (dto.getName() == null || dto.getName().isBlank() || dto.getName().length() > 50) {
            log.warn("Invalid category name length: {}", dto.getName());
            throw new BadRequestException("Category name must be between 1 and 50 characters");
        }
        category.setName(dto.getName());
        Category saved = categoryRepository.save(category);
        log.debug("Category updated: id={}, new name={}", saved.getId(), saved.getName());
        return mapper.toCategoryDto(saved);
    }

    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        log.debug("Getting categories with pagination: from={}, size={}", from, size);
        PageRequest page = PageRequest.of(from / size, size);
        List<CategoryDto> result = categoryRepository.findAll(page).stream()
                .map(mapper::toCategoryDto)
                .collect(Collectors.toList());
        log.debug("Found {} categories", result.size());
        return result;
    }

    @Override
    public CategoryDto getCategory(Long catId) {
        log.debug("Getting category by id={}", catId);
        CategoryDto dto = mapper.toCategoryDto(getCategoryEntity(catId));
        log.debug("Category found: id={}, name={}", dto.getId(), dto.getName());
        return dto;
    }

    private Category getCategoryEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Category not found with id={}", id);
                    return new NotFoundException("Category with id=" + id + " was not found");
                });
    }
}