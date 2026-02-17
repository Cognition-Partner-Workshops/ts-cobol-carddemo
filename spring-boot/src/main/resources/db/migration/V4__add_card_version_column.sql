-- V4: Add version column to card table for optimistic locking
ALTER TABLE card ADD COLUMN version BIGINT DEFAULT 0;
