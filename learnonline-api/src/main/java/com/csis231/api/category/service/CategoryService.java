package com.csis231.api.category.service;

import com.csis231.api.category.dto.CategoryDto;
import com.csis231.api.category.dto.CategoryRequest;
import com.csis231.api.category.mapper.CategoryMapper;
import com.csis231.api.category.model.Category;
import com.csis231.api.category.repository.CategoryRepository;
import com.csis231.api.common.dto.PagedResponse;
import com.csis231.api.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public PagedResponse<CategoryDto> list(Pageable pageable) {
        return PagedResponse.fromPage(categoryRepository.findAll(pageable).map(CategoryMapper::toDto));
    }

    @Transactional(readOnly = true)
    public CategoryDto get(Long id) {
        return CategoryMapper.toDto(categoryRepository.getOrThrow(id));
    }

    @Transactional
    public CategoryDto create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByName(name)) {
            throw new ConflictException("Duplicate category");
        }
        return CategoryMapper.toDto(categoryRepository.save(Category.builder().name(name).build()));
    }

    @Transactional
    public CategoryDto update(Long id, CategoryRequest request) {
        Category category = categoryRepository.getOrThrow(id);
        String name = request.name().trim();
        if (!name.equalsIgnoreCase(category.getName()) && categoryRepository.existsByName(name)) {
            throw new ConflictException("Duplicate category");
        }
        category.setName(name);
        return CategoryMapper.toDto(category);
    }

    @Transactional
    public void delete(Long id) {
        categoryRepository.delete(categoryRepository.getOrThrow(id));
    }
}
