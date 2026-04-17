-- liquibase formatted sql

-- changeset yaobezyana:6
CREATE TABLE sprints (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    goals        TEXT,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PLANNING',
    started_at   TIMESTAMP,
    completed_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- changeset yaobezyana:7
CREATE INDEX idx_sprints_user_id ON sprints(user_id);
CREATE INDEX idx_sprints_status  ON sprints(status);

-- changeset yaobezyana:8
ALTER TABLE projects ADD COLUMN sprint_id BIGINT REFERENCES sprints(id) ON DELETE SET NULL;
CREATE INDEX idx_projects_sprint_id ON projects(sprint_id);

-- changeset yaobezyana:9
ALTER TABLE tasks ADD COLUMN position INT NOT NULL DEFAULT 0;
