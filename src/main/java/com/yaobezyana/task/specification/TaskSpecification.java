package com.yaobezyana.task.specification;

import com.yaobezyana.task.entity.Task;
import com.yaobezyana.task.entity.TaskStatus;
import org.springframework.data.jpa.domain.Specification;

public class TaskSpecification {

    public static Specification<Task> hasUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Task> hasProject(Long projectId) {
        return (root, query, cb) -> projectId == null
                ? cb.conjunction()
                : cb.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Task> hasNoProject() {
        return (root, query, cb) -> cb.isNull(root.get("project"));
    }

    public static Specification<Task> titleContains(String title) {
        return (root, query, cb) -> (title == null || title.isBlank())
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }
}
