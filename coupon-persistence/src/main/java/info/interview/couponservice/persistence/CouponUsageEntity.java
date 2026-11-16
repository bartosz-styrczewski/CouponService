package info.interview.couponservice.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupon_usage")
public class CouponUsageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID couponId;

    private String userId;

    private Instant usedAt;

    protected CouponUsageEntity() {
    }

    public CouponUsageEntity(UUID couponId, String userId, Instant usedAt) {
        this.couponId = couponId;
        this.userId = userId;
        this.usedAt = usedAt;
    }

    public UUID getCouponId() {
        return couponId;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}
