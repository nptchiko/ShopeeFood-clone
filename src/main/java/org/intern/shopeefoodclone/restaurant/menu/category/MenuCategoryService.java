package org.intern.shopeefoodclone.restaurant.menu.category;

import io.github.perplexhub.rsql.RSQLJPASupport;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.intern.shopeefoodclone.restaurant.*;
import org.intern.shopeefoodclone.restaurant.menu.item.MenuItem;
import org.intern.shopeefoodclone.restaurant.menu.item.MenuItemMapper;
import org.intern.shopeefoodclone.restaurant.menu.item.MenuItemRepository;
import org.intern.shopeefoodclone.restaurant.menu.item.MenuItemResponse;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class MenuCategoryService {

    MenuCategoryRepository menuCategoryRepository;
    RestaurantRepository restaurantRepository;
    MenuItemRepository menuItemRepository;
    MenuCategoryMapper menuCategoryMapper;
    MenuItemMapper menuItemMapper;

    @Transactional
    public MenuCategoryResponse create(UUID restaurantId, MenuCategoryCreateRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new AppException(ErrorCode.RESTAURANT_NOT_FOUND, "Restaurant not found with id: " + restaurantId));

        if (menuCategoryRepository.existsByRestaurantIdAndName(restaurantId, request.name())) {
            throw new AppException(ErrorCode.MENU_CATEGORY_ALREADY_EXISTS, "Menu category already exists with name: " + request.name());
        }

        MenuCategory category = menuCategoryMapper.toEntity(request);
        category.setRestaurant(restaurant);
        if (request.sortOrder() == null) {
            category.setSortOrder(0);
        }

        MenuCategory saved = menuCategoryRepository.save(category);
        return menuCategoryMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> findByRestaurantId(UUID restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new AppException(ErrorCode.RESTAURANT_NOT_FOUND, "Restaurant not found with id: " + restaurantId);
        }

        List<MenuCategory> categories = menuCategoryRepository.findByRestaurantIdOrderBySortOrderAsc(restaurantId);
        List<UUID> categoryIds = categories.stream().map(MenuCategory::getId).toList();
        List<MenuItem> items = menuItemRepository.findByCategoryIdIn(categoryIds);

        Map<UUID, List<MenuItem>> itemsByCategoryId = items.
                stream().collect(Collectors.groupingBy(item -> item.getCategory().getId()));


        return categories.stream()
                .map(cat -> {
                    List<MenuItemResponse> itemResponses = itemsByCategoryId.getOrDefault(cat.getId(), List.of())
                            .stream()
                            .map(menuItemMapper::toResponse)
                            .toList();
                    return menuCategoryMapper.toResponseWithItems(cat, itemResponses);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<MenuCategoryResponse> findByRestaurantId(UUID restaurantId, String filter, Pageable pageable) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new AppException(ErrorCode.RESTAURANT_NOT_FOUND, "Restaurant not found with id: " + restaurantId);
        }

        Pageable boundedPageable = PaginationUtils.validateAndBound(pageable);
        Specification<MenuCategory> parentSpec = (root, query, cb) ->
                cb.equal(root.get("restaurant").get("id"), restaurantId);
        Specification<MenuCategory> spec = StringUtils.hasText(filter)
                ? parentSpec.and(RSQLJPASupport.toSpecification(filter))
                : parentSpec;
        Page<MenuCategory> page = menuCategoryRepository.findAll(spec, boundedPageable);
        return PaginationUtils.toPageResponse(page, cat -> {
            List<MenuItemResponse> items = menuItemMapper.toResponseList(menuItemRepository.findByCategoryId(cat.getId()));
            return menuCategoryMapper.toResponseWithItems(cat, items);
        });
    }

    @Transactional(readOnly = true)
    public MenuCategoryResponse getById(UUID id) {
        MenuCategory category = findCategoryById(id);
        List<MenuItemResponse> items = menuItemMapper.toResponseList(menuItemRepository.findByCategoryId(id));
        return menuCategoryMapper.toResponseWithItems(category, items);
    }

    @Transactional
    public MenuCategoryResponse update(UUID id, MenuCategoryUpdateRequest request) {
        MenuCategory category = findCategoryById(id);

    if (request.name() != null && menuCategoryRepository
            .existsByRestaurantIdAndName(category.getRestaurant().getId(), request.name())) {
            throw new AppException(ErrorCode.MENU_CATEGORY_ALREADY_EXISTS, "Menu category already exists with name: " + request.name());
        }


        menuCategoryMapper.update(category, request);
        MenuCategory saved = menuCategoryRepository.save(category);
        List<MenuItemResponse> items = menuItemMapper.toResponseList(menuItemRepository.findByCategoryId(id));
        return menuCategoryMapper.toResponseWithItems(saved, items);
    }

    @Transactional
    public void delete(UUID id) {
        MenuCategory category = findCategoryById(id);
        List<MenuItem> items = menuItemRepository.findByCategoryId(id);
        if (!items.isEmpty()) {
            menuItemRepository.deleteAll(items);
        }
        menuCategoryRepository.delete(category);
    }

    private MenuCategory findCategoryById(UUID id) {
        return menuCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MENU_CATEGORY_NOT_FOUND, "Menu category not found with id: " + id));
    }
}
