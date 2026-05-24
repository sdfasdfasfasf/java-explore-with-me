package ru.practicum.ewm.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.NewCategoryDto;
import ru.practicum.ewm.service.CategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryDto> addCategory(@Valid @RequestBody NewCategoryDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.addCategory(dto));
    }

    @DeleteMapping("/{catId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long catId) {
        categoryService.deleteCategory(catId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{catId}")
    public ResponseEntity<CategoryDto> updateCategory(@PathVariable Long catId, @Valid @RequestBody CategoryDto dto) {
        return ResponseEntity.ok(categoryService.updateCategory(catId, dto));
    }
}