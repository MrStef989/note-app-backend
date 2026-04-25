-- liquibase formatted sql

-- changeset yaobezyana:10
ALTER TABLE projects DROP COLUMN sprint_id;
ALTER TABLE projects ADD COLUMN description TEXT;

-- changeset yaobezyana:11
ALTER TABLE sprints DROP COLUMN title;
ALTER TABLE sprints DROP COLUMN description;
ALTER TABLE sprints DROP COLUMN goals;
ALTER TABLE sprints ADD COLUMN number INT NOT NULL DEFAULT 1;

-- changeset yaobezyana:12
ALTER TABLE tasks ADD COLUMN sprint_id BIGINT REFERENCES sprints(id) ON DELETE SET NULL;
ALTER TABLE tasks DROP COLUMN is_inbox;
CREATE INDEX idx_tasks_sprint_id ON tasks(sprint_id);

-- changeset yaobezyana:13
CREATE TABLE inbox_notes (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content    TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_inbox_notes_user_id ON inbox_notes(user_id);
