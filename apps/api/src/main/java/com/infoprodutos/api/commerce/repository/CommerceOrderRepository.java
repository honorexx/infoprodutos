package com.infoprodutos.api.commerce.repository;

import com.infoprodutos.api.commerce.domain.CommerceOrder;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface CommerceOrderRepository extends JpaRepository<CommerceOrder, UUID> {

    Optional<CommerceOrder> findByIdempotencyKey(String idempotencyKey);

    Optional<CommerceOrder> findByMpPaymentId(String mpPaymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CommerceOrder o where o.id = :id")
    Optional<CommerceOrder> findLockedById(@Param("id") UUID id);

    @Query("""
            select distinct o from CommerceOrder o
            join fetch o.buyer
            left join fetch o.items i
            left join fetch i.course
            where o.id = :id
            """)
    Optional<CommerceOrder> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
            select distinct o from CommerceOrder o
            join fetch o.buyer
            left join fetch o.items i
            left join fetch i.course
            where o.mpPreferenceId = :preferenceId
            """)
    Optional<CommerceOrder> findByMpPreferenceIdWithDetails(@Param("preferenceId") String preferenceId);

    @Query("""
            select distinct o from CommerceOrder o
            join fetch o.buyer
            left join fetch o.items i
            left join fetch i.course
            where o.id = :id and o.buyer.id = :buyerId
            """)
    Optional<CommerceOrder> findByIdAndBuyerIdWithDetails(
            @Param("id") UUID id, @Param("buyerId") UUID buyerId);

    @Query("""
            select distinct o from CommerceOrder o
            join fetch o.buyer
            left join fetch o.items i
            left join fetch i.course
            where o.buyer.id = :buyerId and o.status = com.infoprodutos.api.commerce.domain.OrderStatus.PENDING
            """)
    java.util.List<CommerceOrder> findPendingByBuyerIdWithDetails(@Param("buyerId") UUID buyerId);
}
