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
                                Планировщик с режимом фокуса на основе модели **"умный человек — обезьянка"**.

                                ## Концепция

                                **Умный человек (Planner mode):**
                                1. Создаёт проекты (`POST /api/projects`) и задачи в них (`POST /api/tasks`)
                                2. Заметки из инбокса (`POST /api/inbox`) конвертирует в проекты / задачи / Текучку
                                3. Добавляет **1 задачу из каждого проекта** в текущий спринт (`POST /api/sprints/current/tasks/{taskId}`)
                                4. Если в Текучке есть задачи — добавляет 1 из них тоже
                                5. Запускает спринт когда инбокс пуст (`PATCH /api/sprints/current/start`)

                                **Обезьянка (Focus mode):**
                                1. Смотрит задачи спринта (`GET /api/sprints/current/focus`)
                                2. Берёт любую задачу в работу (`PATCH /api/tasks/{id}/take`) → `IN_PROGRESS`
                                3. Завершает задачу (`PATCH /api/tasks/{id}/complete`) → `COMPLETED`
                                4. Повторяет до конца спринта

                                **Завершение:**
                                Когда все задачи `COMPLETED` → `PATCH /api/sprints/current/complete`.
                                Автоматически создаётся следующий спринт.

                                ## Текучка

                                Задачи без проекта (`projectId = null`) — это Текучка.
                                Фильтр: `GET /api/tasks?routine=true`.
                                Если в Текучке есть задачи, одна обязана попасть в спринт.

                                ## Календарь блокировок

                                Запись в календаре содержит причину и дату, блокирует одну или несколько задач.
                                Заблокированные задачи не видны при выборе задач для спринта.
                                Список отсортирован по дате: первая запись — ближайшая к разблокировке.
                                `GET /api/sprints/current/available-tasks` — задачи доступные для спринта (без заблокированных).

                                ## Инбокс

                                Заметки (`POST /api/inbox`) нужно обработать до запуска спринта.
                                Конвертация заметки: `POST /api/inbox/{id}/convert` → создаёт проект / задачу / Текучку.

                                ## Статусы задачи

                                | Статус | Описание |
                                |--------|----------|
                                | `ACTIVE` | Задача готова к выполнению |
                                | `IN_PROGRESS` | Обезьянка взяла задачу в работу |
                                | `COMPLETED` | Задача выполнена |
                                | `BLOCKED` | Заблокирована до `dueDate` |

                                ## Статусы спринта

                                | Статус | Описание |
                                |--------|----------|
                                | `PLANNING` | Подготовка: добавление задач в спринт |
                                | `ACTIVE` | Режим фокуса активен |
                                | `COMPLETED` | Спринт завершён |

                                ---
                                Передавайте JWT-токен через кнопку **Authorize**.
                                """)
                        .version("3.0.0")
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
