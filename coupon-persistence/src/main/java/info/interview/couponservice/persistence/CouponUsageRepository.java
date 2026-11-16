package info.interview.couponservice.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CouponUsageRepository extends JpaRepository<CouponUsageEntity, UUID> {

    boolean existsByCouponIdAndUserId(UUID couponId, String userId);
}
