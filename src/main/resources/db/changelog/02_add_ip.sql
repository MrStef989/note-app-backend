-- liquibase formatted sql

-- changeset yaobezyana:5
ALTER TABLE users ADD COLUMN ip_address VARCHAR(45);
