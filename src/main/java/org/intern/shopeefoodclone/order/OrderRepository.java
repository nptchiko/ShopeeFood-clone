package org.intern.shopeefoodclone.order;

import org.springframework.beans.factory.BeanRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
    Page<Order> findAllByUserId(UUID userId, Specification<Order> spec, Pageable pageable);
    Page<Order> findAllByUserId(UUID userId, Pageable pageable);
}