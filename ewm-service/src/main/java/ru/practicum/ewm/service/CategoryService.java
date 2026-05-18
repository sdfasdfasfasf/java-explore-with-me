package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto addCategory(NewCategoryDto dto);
    void deleteCategory(Long catId);
    CategoryDto updateCategory(Long catId, CategoryDto dto);
    List<CategoryDto> getCategories(int from, int size);
    CategoryDto getCategory(Long catId);
}