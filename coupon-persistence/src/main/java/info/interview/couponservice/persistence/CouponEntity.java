package info.interview.couponservice.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupon")
public class CouponEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String code;

    private String country;

    private int maxUses;

    private int currentUses;

    private Instant createdAt;

    protected CouponEntity() {
    }

    public CouponEntity(String code, String country, int maxUses, Instant createdAt) {
        this.code = code;
        this.country = country;
        this.maxUses = maxUses;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getCountry() {
        return country;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public int getCurrentUses() {
        return currentUses;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
