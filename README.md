# Я Обезьяна — Backend API

Kanban-доска с режимом фокуса. Модель: **умный человек** планирует → **обезьянка** выполняет без отвлечений.

## Содержание

- [Концепция](#концепция)
- [Запуск](#запуск)
- [Аутентификация](#аутентификация)
- [Порядок работы](#порядок-работы)
- [Endpoints](#endpoints)
  - [Auth](#auth)
  - [Sprints — Planner mode](#sprints--planner-mode)
  - [Sprints — Focus mode](#sprints--focus-mode)
  - [Projects](#projects)
  - [Tasks](#tasks)
- [Статусы](#статусы)
- [Ошибки](#ошибки)

---

## Концепция

### Умный человек (Planner mode)
Полный доступ: создаёт спринты, добавляет проекты и задачи, задаёт порядок, запускает спринт.

### Обезьянка (Focus mode)
Активируется после запуска спринта. Видит список незавершённых задач по проектам. Сама выбирает любую задачу → берёт в работу → выполняет. Пока задача `IN_PROGRESS` — взять другую нельзя. После выполнения всех задач → спринт завершается → все функции разблокируются.

### Иерархия
```
User → Sprint → Project → Task
```

---

## Запуск

### Docker Compose (рекомендуется)

```bash
docker-compose up --build
```

| Сервис    | URL                              |
|-----------|----------------------------------|
| API       | http://localhost:8080            |
| Swagger   | http://localhost:8080/swagger-ui |
| pgAdmin   | http://localhost:5050            |

**pgAdmin:** email `admin@yaobezyana.local`, пароль `admin`.

### Maven + локальная БД (для разработки)

Spring Boot автоматически поднимает PostgreSQL через `compose.yaml` при старте.

**Требования:** Docker, Java 17, Maven.

```bash
./mvnw spring-boot:run
```

### Переменные окружения

| Переменная       | По умолчанию                                      | Описание                    |
|------------------|---------------------------------------------------|-----------------------------|
| `DB_URL`         | `jdbc:postgresql://localhost:5432/yaobezyana`     | URL базы данных             |
| `DB_USERNAME`    | `postgres`                                        | Пользователь БД             |
| `DB_PASSWORD`    | `postgres`                                        | Пароль БД                   |
| `JWT_SECRET`     | `dev-secret-key-please-change-in-production-32ch` | Секрет JWT (мин. 32 символа)|
| `JWT_EXPIRATION` | `86400000`                                        | Время жизни токена, мс (24ч)|

---

## Аутентификация

Все эндпоинты кроме `/api/auth/*` требуют JWT-токен:

```
Authorization: Bearer <token>
```

---

## Порядок работы

### Planner mode (планирование)

```
1. POST /api/auth/register          — регистрация
2. POST /api/auth/login             — получить токен
3. POST /api/sprints                — создать спринт
4. POST /api/projects               — создать проект (указать sprintId)
5. POST /api/tasks                  — создать задачи (указать projectId)
6. PATCH /api/sprints/{id}/projects/{pid}/tasks/reorder  — задать порядок (опционально)
7. PATCH /api/sprints/{id}/start    — запустить спринт → включается Focus mode
```

### Focus mode (выполнение)

```
1. GET  /api/sprints/{id}/tasks     — посмотреть незавершённые задачи
2. PATCH /api/tasks/{id}/take       — взять задачу → статус IN_PROGRESS
3. PATCH /api/tasks/{id}/complete   — выполнить задачу → статус COMPLETED
4. (повторять 1–3 до конца)
5. PATCH /api/sprints/{id}/complete — завершить спринт (когда все задачи COMPLETED)
```

---

## Endpoints

### Auth

#### Регистрация

```
POST /api/auth/register
```

**Body:**
```json
{ "email": "user@example.com", "password": "secret123" }
```

**Response `201`:**
```json
{ "token": "eyJhbGciOiJIUzI1NiJ9..." }
```

#### Вход

```
POST /api/auth/login
```

**Body:** аналогично регистрации.

**Response `200`:**
```json
{ "token": "eyJhbGciOiJIUzI1NiJ9..." }
```

---

### Sprints — Planner mode

> Все запросы требуют `Authorization: Bearer <token>`

#### Список спринтов

```
GET /api/sprints
```

**Response `200`:** массив `SprintSummaryResponse` (новые первые).

#### Детали спринта

```
GET /api/sprints/{id}
```

**Response `200`:** `SprintDetailResponse` — метаданные + все проекты + все задачи.

```json
{
  "id": 1,
  "title": "Sprint 1 — MVP",
  "description": "...",
  "goals": "...",
  "status": "PLANNING",
  "totalTasks": 8,
  "completedTasks": 0,
  "projects": [
    {
      "id": 2,
      "title": "Backend",
      "tasks": [
        {
          "id": 5,
          "title": "Написать тесты",
          "status": "ACTIVE",
          "position": 0,
          "projectId": 2,
          "sprintId": 1,
          ...
        }
      ]
    }
  ],
  "startedAt": null,
  "completedAt": null,
  "createdAt": "...",
  "updatedAt": "..."
}
```

#### Создать спринт

```
POST /api/sprints
```

**Body:**
```json
{
  "title": "Sprint 1 — MVP",
  "description": "Основные фичи",
  "goals": "Auth, CRUD, Focus mode"
}
```

**Response `201`:** `SprintSummaryResponse`

#### Обновить спринт

```
PUT /api/sprints/{id}
```

Доступно только в статусе `PLANNING`. Тело — аналогично созданию.

#### Удалить спринт

```
DELETE /api/sprints/{id}
```

Доступно только в статусе `PLANNING`. Проекты спринта не удаляются, `sprintId` обнуляется.

**Response `204`**

#### Запустить спринт

```
PATCH /api/sprints/{id}/start
```

Условия:
- Статус `PLANNING`
- Хотя бы один проект с незавершёнными задачами
- Нет другого активного спринта у пользователя

**Response `200`:** `SprintSummaryResponse` со статусом `ACTIVE`

#### Завершить спринт

```
PATCH /api/sprints/{id}/complete
```

Условия:
- Статус `ACTIVE`
- Все задачи спринта в статусе `COMPLETED`

**Response `200`:** `SprintSummaryResponse` со статусом `COMPLETED`

#### Переупорядочить задачи

```
PATCH /api/sprints/{sprintId}/projects/{projectId}/tasks/reorder
```

Доступно только в статусе `PLANNING`. Задаёт поле `position` для каждой задачи.

**Body:**
```json
{ "taskIds": [5, 3, 7, 2] }
```

**Response `204`**

---

### Sprints — Focus mode

#### Получить сессию фокуса

```
GET /api/sprints/{id}/tasks
```

Доступно только если спринт в статусе `ACTIVE`.

**Response `200`:**
```json
{
  "sprintId": 1,
  "sprintTitle": "Sprint 1 — MVP",
  "sprintStatus": "ACTIVE",
  "totalTasks": 10,
  "completedTasks": 3,
  "inProgressTask": null,
  "projects": [
    {
      "projectId": 2,
      "projectTitle": "Backend",
      "tasks": [
        { "id": 5, "title": "Написать тесты", "status": "ACTIVE", "position": 0, ... },
        { "id": 6, "title": "Рефакторинг", "status": "ACTIVE", "position": 1, ... }
      ]
    }
  ]
}
```

`inProgressTask` — задача, которую обезьянка сейчас выполняет. Пока она не `null` — нельзя взять другую.

---

### Projects

#### Получить все проекты

```
GET /api/projects
```

**Response `200`:** массив проектов (с `sprintId` если привязан).

#### Создать проект

```
POST /api/projects
```

**Body:**
```json
{
  "title": "Backend",
  "sprintId": 1
}
```

`sprintId` опционален. Если указан — спринт должен быть в статусе `PLANNING`.

**Response `201`:** объект проекта.

#### Обновить проект

```
PUT /api/projects/{id}
```

Нельзя изменять проект пока его спринт в статусе `ACTIVE`.

#### Удалить проект

```
DELETE /api/projects/{id}
```

Нельзя удалять проект пока его спринт в статусе `ACTIVE`. Задачи проекта сохраняются без привязки (`projectId = null`).

**Response `204`**

---

### Tasks

#### Получить задачи

```
GET /api/tasks
```

Query-параметры (все опциональны):

| Параметр    | Тип        | Описание                                          |
|-------------|------------|---------------------------------------------------|
| `projectId` | `Long`     | Фильтр по проекту                                 |
| `status`    | `String`   | `ACTIVE`, `IN_PROGRESS`, `COMPLETED`, `BLOCKED`   |
| `search`    | `String`   | Поиск по названию (без учёта регистра)            |
| `sortBy`    | `String`   | `title`, `dueDate` или `createdAt`                |
| `sortDir`   | `String`   | `asc` или `desc`                                  |

**Response `200`:** массив задач. Каждая задача содержит `sprintId` и `position`.

#### Создать задачу

```
POST /api/tasks
```

**Body:**
```json
{
  "title": "Написать тесты",
  "description": "Покрыть сервис юнит-тестами",
  "projectId": 2,
  "dueDate": "2026-05-01T10:00:00"
}
```

Обязательно только `title`. Нельзя создавать задачу в проекте активного спринта.

**Поведение статусов:**
- `dueDate` в будущем → `BLOCKED`
- Иначе → `ACTIVE`

#### Обновить задачу

```
PUT /api/tasks/{id}
```

Нельзя редактировать задачу в активном спринте.

#### Удалить задачу

```
DELETE /api/tasks/{id}
```

Нельзя удалять задачу в активном спринте. **Response `204`**

#### Взять задачу в работу

```
PATCH /api/tasks/{id}/take
```

**Focus mode.** Обезьянка берёт задачу → статус `IN_PROGRESS`.

Условия:
- Задача в статусе `ACTIVE`
- Задача в проекте активного спринта
- В этом спринте нет другой задачи `IN_PROGRESS`

**Response `200`:** обновлённая задача.

#### Завершить задачу

```
PATCH /api/tasks/{id}/complete
```

Статус → `COMPLETED`. **Response `200`**

---

## Статусы

### Задача (`TaskStatus`)

| Статус        | Описание                                                  |
|---------------|-----------------------------------------------------------|
| `ACTIVE`      | Готова к выполнению, можно взять                          |
| `IN_PROGRESS` | Обезьянка выполняет задачу прямо сейчас                   |
| `COMPLETED`   | Выполнена                                                 |
| `BLOCKED`     | Заблокирована до наступления `dueDate` (авто-разблокировка каждый час) |

### Спринт (`SprintStatus`)

| Статус      | Возможности                                              |
|-------------|----------------------------------------------------------|
| `PLANNING`  | Полный доступ: CRUD проектов и задач, редактирование     |
| `ACTIVE`    | Только Focus mode: взять задачу, завершить задачу        |
| `COMPLETED` | Только чтение                                            |

---

## Ошибки

```json
{
  "status": 404,
  "message": "Задача не найдена: 99",
  "timestamp": "2026-04-17T12:00:00"
}
```

| HTTP-статус | Когда возникает                                          |
|-------------|----------------------------------------------------------|
| `400`       | Нарушение бизнес-правил или невалидные данные            |
| `401`       | Нет токена или неверные credentials                      |
| `404`       | Ресурс не найден или не принадлежит пользователю         |
| `500`       | Внутренняя ошибка сервера                                |
