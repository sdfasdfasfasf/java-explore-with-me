package ru.practicum.ewm.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.NewCategoryDto;
import ru.practicum.ewm.model.Category;

@Component
public class CategoryMapper {

    public Category toCategory(NewCategoryDto dto) {
        if (dto == null) return null;
        return Category.builder()
                .name(dto.getName())
                .build();
    }

    public CategoryDto toCategoryDto(Category category) {
        if (category == null) return null;
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}