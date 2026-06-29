package org.intern.shopeefoodclone.restaurant;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class RestaurantService {

    RestaurantRepository restaurantRepository;

    public Restaurant create(Restaurant restaurant) {
        return restaurantRepository.save(restaurant);
    }

    @Transactional(readOnly = true)
    public List<Restaurant> findAll() {
        return restaurantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Restaurant findById(UUID id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
    }

    public Restaurant update(UUID id, Restaurant updated) {
        Restaurant existing = findById(id);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setLogoUrl(updated.getLogoUrl());
        existing.setBannerUrl(updated.getBannerUrl());
        existing.setIsOpen(updated.getIsOpen());
        // we might not update owner or address here, or maybe we do
        if (updated.getOwner() != null) existing.setOwner(updated.getOwner());
        if (updated.getAddress() != null) existing.setAddress(updated.getAddress());
        return restaurantRepository.save(existing);
    }

    public void delete(UUID id) {
        Restaurant existing = findById(id);
        restaurantRepository.delete(existing);
    }
}
