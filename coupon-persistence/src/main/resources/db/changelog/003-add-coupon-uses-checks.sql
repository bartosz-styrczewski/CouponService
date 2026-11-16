--liquibase formatted sql

--changeset bartosz:003-add-coupon-uses-checks
ALTER TABLE coupon
    ADD CONSTRAINT ck_coupon_max_uses_positive CHECK (max_uses > 0),
    ADD CONSTRAINT ck_coupon_current_uses_within_limit CHECK (current_uses >= 0 AND current_uses <= max_uses);
--rollback ALTER TABLE coupon DROP CONSTRAINT ck_coupon_current_uses_within_limit, DROP CONSTRAINT ck_coupon_max_uses_positive;
