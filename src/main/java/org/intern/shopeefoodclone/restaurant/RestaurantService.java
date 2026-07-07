package org.intern.shopeefoodclone.restaurant;

import io.github.perplexhub.rsql.RSQLJPASupport;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.intern.shopeefoodclone.restaurant.menu.category.MenuCategory;
import org.intern.shopeefoodclone.restaurant.menu.category.MenuCategoryRepository;
import org.intern.shopeefoodclone.restaurant.menu.category.MenuCategoryResponse;
import org.intern.shopeefoodclone.restaurant.menu.category.MenuCategoryService;
import org.intern.shopeefoodclone.shared.api.PageResponse;
import org.intern.shopeefoodclone.shared.exception.AppException;
import org.intern.shopeefoodclone.shared.exception.ErrorCode;
import org.intern.shopeefoodclone.shared.utils.PaginationUtils;
import org.intern.shopeefoodclone.user.Address;
import org.intern.shopeefoodclone.user.AddressRepository;
import org.intern.shopeefoodclone.user.User;
import org.intern.shopeefoodclone.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class RestaurantService {

    RestaurantRepository restaurantRepository;
    UserRepository userRepository;
    AddressRepository addressRepository;
    MenuCategoryRepository menuCategoryRepository;
    RestaurantMapper restaurantMapper;
    MenuCategoryService menuCategoryService;

    @Transactional
    public RestaurantResponse create(RestaurantCreateRequest request) {
        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Owner user not found with id: " + request.ownerId()));

        Restaurant restaurant = restaurantMapper.toEntity(request);
        restaurant.setOwner(owner);
        if (restaurant.getRating() == null) {
            restaurant.setRating(BigDecimal.ZERO);
        }
        if (request.isOpen() == null) {
            restaurant.setIsOpen(true);
        }

        return restaurantMapper.toResponse(restaurantRepository.save(restaurant));
    }

    @Transactional(readOnly = true)
    public PageResponse<RestaurantResponse> findAll(String filter, Pageable pageable) {
        Pageable boundedPageable = PaginationUtils.validateAndBound(pageable);
        Page<Restaurant> page = StringUtils.hasText(filter)
                ? restaurantRepository.findAll(RSQLJPASupport.toSpecification(filter), boundedPageable)
                : restaurantRepository.findAll(boundedPageable);
        return PaginationUtils.toPageResponse(page, restaurantMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RestaurantDetailResponse getById(UUID id) {
        Restaurant restaurant = findRestaurantById(id);
        List<MenuCategoryResponse> menu = menuCategoryService.findByRestaurantId(id);
        return restaurantMapper.toDetailResponse(restaurant, menu);
    }

    @Transactional
    public RestaurantResponse update(UUID id, RestaurantUpdateRequest request) {
        Restaurant restaurant = findRestaurantById(id);

        if (request.addressId() != null && (restaurant.getAddress() == null || !request.addressId().equals(restaurant.getAddress().getId()))) {
            Address newAddress = addressRepository.findById(request.addressId())
                    .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND, "Address not found with id: " + request.addressId()));
            restaurant.setAddress(newAddress);
        }

        restaurantMapper.update(restaurant, request);
        return restaurantMapper.toResponse(restaurantRepository.save(restaurant));
    }

    @Transactional
    public void delete(UUID id) {
        Restaurant restaurant = findRestaurantById(id);
        List<MenuCategory> categories = menuCategoryRepository.findByRestaurantIdOrderBySortOrderAsc(id);
        for (MenuCategory cat : categories) {
            menuCategoryService.delete(cat.getId());
        }
        restaurantRepository.delete(restaurant);
    }

    private Restaurant findRestaurantById(UUID id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESTAURANT_NOT_FOUND, "Restaurant not found with id: " + id));
    }
}
