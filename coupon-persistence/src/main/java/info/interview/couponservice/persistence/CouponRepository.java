package info.interview.couponservice.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<CouponEntity, UUID> {

    Optional<CouponEntity> findByCode(String code);

    /**
     * Increments the number of uses in a single statement, but only while it is lower than the limit,
     * so parallel requests can never exceed the limit.
     *
     * @return 1 if the use was counted, 0 if the coupon has no uses left
     */
    @Modifying
    @Transactional
    @Query("update CouponEntity c set c.currentUses = c.currentUses + 1 where c.id = :id and c.currentUses < c.maxUses")
    int incrementCurrentUsesIfAvailable(UUID id);
}
