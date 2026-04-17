package com.yaobezyana.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Я Обезьяна API")
                        .description("""
                                Kanban-доска с режимом фокуса на основе модели **"умный человек — обезьянка"**.

                                ## Концепция

                                **Умный человек (Planner mode):**
                                1. Создаёт спринт (`POST /api/sprints`)
                                2. Создаёт проекты и привязывает их к спринту (`POST /api/projects` с `sprintId`)
                                3. Добавляет задачи в проекты (`POST /api/tasks` с `projectId`)
                                4. Опционально задаёт порядок задач (`PATCH /api/sprints/{id}/projects/{pid}/tasks/reorder`)
                                5. Запускает спринт (`PATCH /api/sprints/{id}/start`)

                                **Обезьянка (Focus mode):**
                                1. Смотрит незавершённые задачи (`GET /api/sprints/{id}/tasks`)
                                2. Берёт любую задачу в работу (`PATCH /api/tasks/{id}/take`) → статус `IN_PROGRESS`
                                3. Выполняет задачу → (`PATCH /api/tasks/{id}/complete`) → статус `COMPLETED`
                                4. Повторяет до конца спринта

                                **Завершение:**
                                Когда все задачи `COMPLETED` → `PATCH /api/sprints/{id}/complete`.
                                Разблокируются все функции планирования.

                                ## Статусы задачи

                                | Статус | Описание |
                                |--------|----------|
                                | `ACTIVE` | Задача готова к выполнению |
                                | `IN_PROGRESS` | Обезьянка взяла задачу в работу |
                                | `COMPLETED` | Задача выполнена |
                                | `BLOCKED` | Задача заблокирована до наступления `dueDate` |

                                ## Статусы спринта

                                | Статус | Описание |
                                |--------|----------|
                                | `PLANNING` | Подготовка: добавление проектов и задач |
                                | `ACTIVE` | Режим фокуса активен, структуру менять нельзя |
                                | `COMPLETED` | Спринт завершён |

                                ---
                                Для запросов к защищённым эндпоинтам передавайте JWT-токен через кнопку **Authorize**.
                                """)
                        .version("2.0.0")
                        .contact(new Contact().name("YaObezYana")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT токен, полученный через POST /api/auth/login")));
    }
}
