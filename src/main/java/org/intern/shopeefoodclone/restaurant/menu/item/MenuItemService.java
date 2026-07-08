package org.intern.shopeefoodclone.restaurant.menu.item;

import io.github.perplexhub.rsql.RSQLJPASupport;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.intern.shopeefoodclone.restaurant.menu.category.MenuCategory;
import org.intern.shopeefoodclone.restaurant.menu.category.MenuCategoryRepository;
import org.intern.shopeefoodclone.shared.api.PageResponse;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.intern.shopeefoodclone.shared.utils.PaginationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class MenuItemService {

    MenuItemRepository menuItemRepository;
    MenuCategoryRepository menuCategoryRepository;
    MenuItemMapper menuItemMapper;

    @Transactional
    public MenuItemResponse create(UUID categoryId, MenuItemCreateRequest request) {
        MenuCategory category = menuCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_CATEGORY_NOT_FOUND, "Menu category not found with id: " + categoryId));

        if (menuItemRepository.existsByCategoryIdAndName(categoryId, request.name())) {
            throw new AppException(ErrorCode.MENU_ITEM_ALREADY_EXISTS, "Menu item already exists with name: " + request.name());
        }

        MenuItem item = menuItemMapper.toEntity(request);
        item.setCategory(category);
        if (request.isAvailable() == null) {
            item.setIsAvailable(true);
        }

        return menuItemMapper.toResponse(menuItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> findByCategoryId(UUID categoryId) {

        validateCategoryExists(categoryId);

        return menuItemMapper.toResponseList(menuItemRepository.findByCategoryId(categoryId));
    }

    @Transactional(readOnly = true)
    public PageResponse<MenuItemResponse> findByCategoryId(UUID categoryId, String filter, Pageable pageable) {

        validateCategoryExists(categoryId);

        Pageable boundedPageable = PaginationUtils.validateAndBound(pageable);
        Specification<MenuItem> parentSpec = (root, query, cb) ->
                cb.equal(root.get("category").get("id"), categoryId);
        Specification<MenuItem> spec = StringUtils.hasText(filter)
                ? parentSpec.and(RSQLJPASupport.toSpecification(filter))
                : parentSpec;
        Page<MenuItem> page = menuItemRepository.findAll(spec, boundedPageable);
        return PaginationUtils.toPageResponse(page, menuItemMapper::toResponse);
    }

    public MenuItemResponse getById(UUID id) {
        MenuItem item = findItemById(id);
        return menuItemMapper.toResponse(item);
    }

    @Transactional
    public MenuItemResponse update(UUID id, MenuItemUpdateRequest request) {
        MenuItem item = findItemById(id);

    if (request.name() != null && menuItemRepository.existsByCategoryIdAndName(item.getCategory().getId(), request.name())) {
            throw new AppException(ErrorCode.MENU_ITEM_ALREADY_EXISTS, "Menu item already exists with name: " + request.name());
        }

        menuItemMapper.update(item, request);
        return menuItemMapper.toResponse(menuItemRepository.save(item));
    }

    @Transactional
    public void delete(UUID id) {
        MenuItem item = findItemById(id);
        menuItemRepository.delete(item);
    }

    private MenuItem findItemById(UUID id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_ITEM_NOT_FOUND, "Menu item not found with id: " + id));
    }

    private void validateCategoryExists(UUID categoryId) {
        if (!menuCategoryRepository.existsById(categoryId)) {
            throw new AppException(ErrorCode.MENU_CATEGORY_NOT_FOUND, "Menu category not found with id: " + categoryId);
        }
    }
}
