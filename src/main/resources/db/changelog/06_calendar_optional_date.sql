-- liquibase formatted sql

-- changeset yaobezyana:17
ALTER TABLE calendar_entries ALTER COLUMN date DROP NOT NULL;
