-- liquibase formatted sql

-- changeset yaobezyana:14
CREATE TABLE calendar_entries (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    note       TEXT      NOT NULL,
    date       DATE      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_calendar_entries_user_id ON calendar_entries(user_id);
CREATE INDEX idx_calendar_entries_date ON calendar_entries(date);

-- changeset yaobezyana:15
CREATE TABLE calendar_entry_tasks (
    calendar_entry_id BIGINT NOT NULL REFERENCES calendar_entries(id) ON DELETE CASCADE,
    task_id           BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    PRIMARY KEY (calendar_entry_id, task_id)
);
CREATE INDEX idx_calendar_entry_tasks_task_id ON calendar_entry_tasks(task_id);
