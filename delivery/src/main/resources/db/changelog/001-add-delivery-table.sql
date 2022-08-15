-- liquibase formatted sql

-- changeset wojtek:1659259763000-1
-- comment create delivery table
CREATE TABLE public.delivery
(
    order_id            VARCHAR(255)    NOT NULL,
    customer_id         VARCHAR(255)    NOT NULL,
    restaurant_id       VARCHAR(255)    NOT NULL,
    status              VARCHAR(255)    NOT NULL,
    address             VARCHAR         NOT NULL,
    items               JSONB           NOT NULL,
    delivery_charge     NUMERIC(10, 2)  NOT NULL,
    tip                 NUMERIC(10, 2)  NOT NULL,
    total               NUMERIC(10, 2)  NOT NULL,
    metadata            JSONB,

    CONSTRAINT "deliveryPK" PRIMARY KEY (order_id)
);