--liquibase formatted sql

--changeset bartosz:001-create-coupon-table
CREATE TABLE coupon
(
    id           UUID        NOT NULL,
    code         VARCHAR(32) NOT NULL,
    country      VARCHAR(2)  NOT NULL,
    max_uses     INTEGER     NOT NULL,
    current_uses INTEGER     NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_coupon PRIMARY KEY (id),
    CONSTRAINT uq_coupon_code UNIQUE (code)
);
--rollback DROP TABLE coupon;
