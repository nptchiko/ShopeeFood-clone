package org.intern.shopeefoodclone.restaurant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {

}
