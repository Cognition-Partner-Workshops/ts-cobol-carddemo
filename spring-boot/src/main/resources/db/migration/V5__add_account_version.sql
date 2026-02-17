-- V4: Add version column to account table for optimistic locking
ALTER TABLE account ADD COLUMN version BIGINT DEFAULT 0;
