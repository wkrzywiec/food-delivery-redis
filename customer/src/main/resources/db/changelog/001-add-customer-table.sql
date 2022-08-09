-- liquibase formatted sql

-- changeset wojtek:1658565832000-1
-- comment create customer table
CREATE TABLE public.customer
(
    id          VARCHAR(255)    NOT NULL,
    first_name  VARCHAR(255),
    last_name   VARCHAR(255),
    email       VARCHAR(255)    NOT NULL,
    phone       VARCHAR(255),

    CONSTRAINT "customerPK" PRIMARY KEY (id)
);