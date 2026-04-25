# Я Обезьяна — Backend API

Планировщик с режимом фокуса. Модель: **умный человек** планирует → **обезьянка** выполняет без отвлечений.

## Содержание

- [Концепция](#концепция)
- [Запуск](#запуск)
- [Аутентификация](#аутентификация)
- [Порядок работы](#порядок-работы)
- [Endpoints](#endpoints)
  - [Auth](#auth)
  - [Sprint](#sprint)
  - [Calendar (блокировки)](#calendar-блокировки)
  - [Inbox (заметки)](#inbox-заметки)
  - [Projects](#projects)
  - [Tasks](#tasks)
- [Статусы](#статусы)
- [Ошибки](#ошибки)

---

## Концепция

### Умный человек (Planner mode)
Создаёт проекты и задачи, разбирает заметки из инбокса, формирует спринт — добавляет **1 задачу из каждого проекта** + 1 задачу из Текучки (если есть), затем запускает спринт.

### Обезьянка (Focus mode)
Активируется после запуска спринта. Видит задачи спринта по группам. Сама выбирает задачу → берёт в работу → выполняет. Пока задача `IN_PROGRESS` — взять другую нельзя.

### Текучка
Задачи без проекта (`projectId = null`). Если в Текучке есть задачи, нужно включить 1 из них в спринт.

### Календарь блокировок
Запись в календаре содержит заметку-причину, дату и список заблокированных задач. Заблокированные задачи остаются в проектах, но не доступны для добавления в спринт. Список отсортирован по дате: **первая запись — ближайшая к разблокировке**.

### Иерархия
```
User
├── Sprint (авто-создаётся, 1 активный, numbered)
│    └── Task (добавляется: 1 из каждого проекта + 1 из текучки)
├── Project (независимы от спринта)
│    └── Task
├── Task (без проекта = Текучка)
└── InboxNote (заметки → конвертируются в проект/задачу/текучку)
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

### Maven + локальная БД

```bash
./mvnw spring-boot:run
```

**Требования:** Docker, Java 17, Maven.

### Переменные окружения

| Переменная       | По умолчанию                                      | Описание                     |
|------------------|---------------------------------------------------|------------------------------|
| `DB_URL`         | `jdbc:postgresql://localhost:5432/yaobezyana`     | URL базы данных              |
| `DB_USERNAME`    | `postgres`                                        | Пользователь БД              |
| `DB_PASSWORD`    | `postgres`                                        | Пароль БД                    |
| `JWT_SECRET`     | `dev-secret-key-please-change-in-production-32ch` | Секрет JWT (мин. 32 символа) |
| `JWT_EXPIRATION` | `86400000`                                        | Время жизни токена, мс (24ч) |

---

## Аутентификация

Все эндпоинты кроме `/api/auth/*` требуют JWT-токен:

```
Authorization: Bearer <token>
```

---

## Порядок работы

### Planner mode

```
1. POST /api/auth/register               — регистрация (авто-создаётся спринт №1)
2. POST /api/auth/login                  — получить токен
3. POST /api/projects                    — создать проекты
4. POST /api/tasks                       — создать задачи (projectId или без = Текучка)
5. POST /api/inbox                       — добавить заметки (опционально)
6. POST /api/inbox/{id}/convert          — конвертировать заметку в проект/задачу/текучку
7. POST /api/sprints/current/tasks/{id}  — добавить 1 задачу из каждого проекта в спринт
8. PATCH /api/sprints/current/start      — запустить спринт
```

### Focus mode

```
1. GET  /api/sprints/current/focus       — посмотреть задачи спринта
2. PATCH /api/tasks/{id}/take            — взять задачу → IN_PROGRESS
3. PATCH /api/tasks/{id}/complete        — завершить задачу → COMPLETED
4. (повторять 2–3 до конца)
5. PATCH /api/sprints/current/complete   — завершить спринт (авто-создаётся следующий)
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

**Response `201`:** `{ "token": "..." }`

Автоматически создаётся первый спринт (№1) в статусе `PLANNING`.

#### Вход

```
POST /api/auth/login
```

**Response `200`:** `{ "token": "..." }`

---

### Sprint

#### История спринтов

```
GET /api/sprints
```

**Response `200`:** массив `SprintSummaryResponse` (новые первые).

#### Текущий спринт

```
GET /api/sprints/current
```

Возвращает PLANNING или ACTIVE спринт с задачами сгруппированными по проектам. Группа с `id=null` — это Текучка.

**Response `200`:**
```json
{
  "id": 3,
  "number": 3,
  "status": "PLANNING",
  "totalTasks": 3,
  "completedTasks": 0,
  "projects": [
    {
      "id": 2,
      "title": "Backend",
      "tasks": [{ "id": 5, "title": "Написать тесты", "status": "ACTIVE", ... }]
    },
    {
      "id": null,
      "title": null,
      "tasks": [{ "id": 9, "title": "Текучая задача", "status": "ACTIVE", ... }]
    }
  ],
  ...
}
```

#### Спринт по ID (история)

```
GET /api/sprints/{id}
```

#### Добавить задачу в спринт

```
POST /api/sprints/current/tasks/{taskId}
```

Правила:
- Спринт должен быть в статусе `PLANNING`
- Из каждого проекта можно добавить только **1 задачу**
- Из Текучки тоже только **1 задачу**
- Задача уже не должна быть в спринте

**Response `200`:** обновлённый `SprintSummaryResponse`

#### Убрать задачу из спринта

```
DELETE /api/sprints/current/tasks/{taskId}
```

**Response `204`**

#### Запустить спринт

```
PATCH /api/sprints/current/start
```

Условия:
- В спринте есть хотя бы 1 задача
- Инбокс **пустой** (все заметки обработаны)
- Если в Текучке есть незавершённые задачи — одна из них **обязана** быть в спринте

**Response `200`:** `SprintSummaryResponse` со статусом `ACTIVE`

#### Завершить спринт

```
PATCH /api/sprints/current/complete
```

Условие: все задачи спринта в статусе `COMPLETED`.

Автоматически создаётся следующий спринт (номер + 1).

**Response `200`:** `SprintSummaryResponse` со статусом `COMPLETED`

#### Сессия фокуса

```
GET /api/sprints/current/focus
```

Доступно только если есть ACTIVE спринт.

**Response `200`:**
```json
{
  "sprintId": 3,
  "sprintNumber": 3,
  "sprintStatus": "ACTIVE",
  "totalTasks": 3,
  "completedTasks": 1,
  "inProgressTask": null,
  "projects": [
    {
      "projectId": 2,
      "projectTitle": "Backend",
      "tasks": [{ "id": 5, "status": "ACTIVE", ... }]
    },
    {
      "projectId": null,
      "projectTitle": null,
      "tasks": [{ "id": 9, "title": "Текучая задача", "status": "ACTIVE", ... }]
    }
  ]
}
```

`inProgressTask` — задача в работе. Пока она не `null` — взять другую нельзя.

#### Доступные задачи для спринта

```
GET /api/sprints/current/available-tasks
```

Возвращает задачи которые **можно добавить** в текущий PLANNING спринт, сгруппированные по проектам.

Исключает: заблокированные (календарь), уже в спринте, статус BLOCKED/COMPLETED.

**Response `200`:**
```json
{
  "groups": [
    {
      "projectId": 2,
      "projectTitle": "Backend",
      "sprintTaskAdded": false,
      "tasks": [
        { "id": 5, "title": "Написать тесты", "status": "ACTIVE", ... }
      ]
    },
    {
      "projectId": null,
      "projectTitle": null,
      "sprintTaskAdded": true,
      "tasks": [
        { "id": 9, "title": "Другая текучка", "status": "ACTIVE", ... }
      ]
    }
  ]
}
```

`sprintTaskAdded: true` — лимит для этого проекта исчерпан (задача уже добавлена).

---

### Calendar (блокировки)

#### Список блокировок

```
GET /api/calendar
```

Отсортировано по дате: ближайшие к разблокировке — первые.

**Response `200`:** массив `CalendarEntryResponse`

#### Создать блокировку

```
POST /api/calendar
```

**Body:**
```json
{
  "note": "Ждём ответа от заказчика",
  "date": "2026-05-10",
  "taskIds": [5, 7, 12]
}
```

**Response `201`:** `CalendarEntryResponse` с заблокированными задачами

#### Обновить блокировку

```
PUT /api/calendar/{id}
```

Тело аналогично созданию, `taskIds` опционален — пустой список снимает блокировку со всех задач.

#### Удалить блокировку

```
DELETE /api/calendar/{id}
```

Все задачи этой записи становятся доступными для спринта. **Response `204`**

---

### Inbox (заметки)

#### Получить заметки

```
GET /api/inbox
```

**Response `200`:** массив заметок (старые первые).

#### Создать заметку

```
POST /api/inbox
```

**Body:** `{ "content": "Разобраться с CI/CD" }`

**Response `201`:** `InboxNoteResponse`

#### Удалить заметку

```
DELETE /api/inbox/{id}
```

**Response `204`**

#### Конвертировать заметку

```
POST /api/inbox/{id}/convert
```

Конвертирует заметку в сущность и удаляет её из инбокса.

**Body:**
```json
{
  "type": "TASK",
  "title": "Написать документацию",
  "projectId": 2
}
```

Типы:
- `PROJECT` — создаёт проект с указанным `title`
- `TASK` — создаёт задачу в проекте (`projectId` обязателен)
- `ROUTINE` — создаёт задачу в Текучке (без проекта)

**Response `200`:** `{ "type": "TASK", "id": 15 }`

---

### Projects

#### Получить все проекты

```
GET /api/projects
```

#### Создать проект

```
POST /api/projects
```

**Body:** `{ "title": "Backend", "description": "Серверная часть" }`

**Response `201`:** `ProjectResponse`

#### Обновить проект

```
PUT /api/projects/{id}
```

#### Удалить проект

```
DELETE /api/projects/{id}
```

Задачи проекта переходят в Текучку (`projectId = null`).

**Response `204`**

---

### Tasks

#### Получить задачи

```
GET /api/tasks
```

Query-параметры:

| Параметр    | Тип       | Описание                                        |
|-------------|-----------|-------------------------------------------------|
| `projectId` | `Long`    | Фильтр по проекту                               |
| `routine`   | `Boolean` | `true` — только задачи без проекта (Текучка)    |
| `status`    | `String`  | `ACTIVE`, `IN_PROGRESS`, `COMPLETED`, `BLOCKED` |
| `search`    | `String`  | Поиск по названию                               |
| `sortBy`    | `String`  | `title`, `dueDate`, `createdAt`                 |
| `sortDir`   | `String`  | `asc` или `desc`                                |

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

`projectId` опционален — без него задача попадает в Текучку.

`dueDate` в будущем → статус `BLOCKED`.

#### Обновить задачу

```
PUT /api/tasks/{id}
```

Нельзя изменять задачу в активном спринте.

#### Удалить задачу

```
DELETE /api/tasks/{id}
```

Нельзя удалять задачу в активном спринте. **Response `204`**

#### Взять задачу в работу

```
PATCH /api/tasks/{id}/take
```

**Focus mode.** Статус → `IN_PROGRESS`.

Условия: задача `ACTIVE`, в активном спринте, нет другой `IN_PROGRESS` задачи в спринте.

#### Завершить задачу

```
PATCH /api/tasks/{id}/complete
```

Статус → `COMPLETED`. **Response `200`**

---

## Статусы

### Задача (`TaskStatus`)

| Статус        | Описание                                                             |
|---------------|----------------------------------------------------------------------|
| `ACTIVE`      | Готова к выполнению                                                  |
| `IN_PROGRESS` | Обезьянка выполняет прямо сейчас                                     |
| `COMPLETED`   | Выполнена                                                            |
| `BLOCKED`     | Заблокирована до `dueDate` (авто-разблокировка каждый час)           |

### Спринт (`SprintStatus`)

| Статус      | Описание                                                 |
|-------------|----------------------------------------------------------|
| `PLANNING`  | Формирование спринта: добавление задач                   |
| `ACTIVE`    | Focus mode активен                                       |
| `COMPLETED` | Завершён, доступен только для просмотра                  |

---

## Ошибки

```json
{
  "status": 400,
  "message": "Из проекта 'Backend' уже добавлена задача в спринт",
  "timestamp": "2026-04-25T12:00:00"
}
```

| HTTP-статус | Когда возникает                                     |
|-------------|-----------------------------------------------------|
| `400`       | Нарушение бизнес-правил или невалидные данные        |
| `401`       | Нет токена или неверный логин                        |
| `404`       | Ресурс не найден или не принадлежит пользователю    |
| `500`       | Внутренняя ошибка                                    |
