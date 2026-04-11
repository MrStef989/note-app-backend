# Я Обезьяна — Backend API

Персональный планировщик задач с двумя слоями: общий список задач и Inbox (задачи на сегодня).

## Содержание

- [Запуск локально](#запуск-локально)
- [Аутентификация](#аутентификация)
- [Порядок работы с API](#порядок-работы-с-api)
- [Endpoints](#endpoints)
  - [Auth](#auth)
  - [Projects](#projects)
  - [Tasks](#tasks)
  - [Inbox](#inbox)
- [Фильтрация задач](#фильтрация-задач)
- [Ошибки](#ошибки)

---

## Запуск локально

### Вариант 1: Docker Compose (всё вместе)

Запускает приложение + PostgreSQL в контейнерах.

```bash
docker-compose up --build
```

Приложение доступно на `http://localhost:8080`.

### Вариант 2: Maven + локальная БД (для разработки)

Spring Boot автоматически поднимает PostgreSQL через `compose.yaml` при старте.

**Требования:** Docker, Java 17, Maven.

```bash
./mvnw spring-boot:run
```

Spring Boot сам запустит контейнер с PostgreSQL (через spring-boot-docker-compose), применит миграции Liquibase и стартует на порту `8080`.

### Переменные окружения

| Переменная       | По умолчанию                                      | Описание              |
|------------------|---------------------------------------------------|-----------------------|
| `DB_URL`         | `jdbc:postgresql://localhost:5432/yaobezyana`     | URL базы данных       |
| `DB_USERNAME`    | `postgres`                                        | Пользователь БД       |
| `DB_PASSWORD`    | `postgres`                                        | Пароль БД             |
| `JWT_SECRET`     | `dev-secret-key-please-change-in-production-32ch` | Секрет JWT (мин. 32 символа) |
| `JWT_EXPIRATION` | `86400000`                                        | Время жизни токена, мс (24 ч) |

---

## Аутентификация

Все эндпоинты кроме `/api/auth/*` требуют JWT-токен в заголовке:

```
Authorization: Bearer <token>
```

Токен получается при регистрации или логине.

---

## Порядок работы с API

1. **Зарегистрироваться** → `POST /api/auth/register`
2. **Войти** → `POST /api/auth/login` (получить токен)
3. *(опционально)* **Создать проект** → `POST /api/projects`
4. **Создать задачи** → `POST /api/tasks`
5. **Добавить задачи в Inbox** → `PATCH /api/tasks/{id}/inbox`
6. **Посмотреть Inbox** → `GET /api/inbox`
7. **Завершить задачу** → `PATCH /api/tasks/{id}/complete`

---

## Endpoints

### Auth

#### Регистрация

```
POST /api/auth/register
```

**Body:**
```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

Валидация: `email` — валидный адрес, `password` — минимум 6 символов.

**Response `201 Created`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

#### Вход

```
POST /api/auth/login
```

**Body:**
```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

### Projects

> Все запросы требуют `Authorization: Bearer <token>`

#### Получить все проекты

```
GET /api/projects
```

**Response `200 OK`:**
```json
[
  { "id": 1, "title": "Работа" },
  { "id": 2, "title": "Личное" }
]
```

---

#### Создать проект

```
POST /api/projects
```

**Body:**
```json
{
  "title": "Работа"
}
```

**Response `201 Created`:**
```json
{
  "id": 1,
  "title": "Работа"
}
```

---

#### Обновить проект

```
PUT /api/projects/{id}
```

**Body:**
```json
{
  "title": "Новое название"
}
```

**Response `200 OK`:**
```json
{
  "id": 1,
  "title": "Новое название"
}
```

---

#### Удалить проект

```
DELETE /api/projects/{id}
```

**Response `204 No Content`**

> Задачи проекта **не удаляются** — у них сбрасывается `projectId` в `null`.

---

### Tasks

> Все запросы требуют `Authorization: Bearer <token>`

#### Получить все задачи

```
GET /api/tasks
```

Query-параметры (все опциональны):

| Параметр    | Тип          | Описание                                  |
|-------------|--------------|-------------------------------------------|
| `projectId` | `Long`       | Фильтр по проекту                         |
| `status`    | `String`     | `ACTIVE`, `COMPLETED` или `BLOCKED`       |
| `search`    | `String`     | Поиск по названию (без учёта регистра)    |
| `sortBy`    | `String`     | `title`, `dueDate` или `createdAt`        |
| `sortDir`   | `String`     | `asc` или `desc`                          |

Примеры:
```
GET /api/tasks?status=ACTIVE
GET /api/tasks?projectId=1&sortBy=dueDate&sortDir=asc
GET /api/tasks?search=купить
```

**Response `200 OK`:**
```json
[
  {
    "id": 1,
    "title": "Написать отчёт",
    "description": "Q1 отчёт для менеджера",
    "projectId": 1,
    "projectTitle": "Работа",
    "status": "ACTIVE",
    "inbox": true,
    "dueDate": "2026-04-15T10:00:00",
    "createdAt": "2026-04-11T09:00:00",
    "updatedAt": "2026-04-11T09:00:00"
  }
]
```

---

#### Создать задачу

```
POST /api/tasks
```

**Body:**
```json
{
  "title": "Написать отчёт",
  "description": "Q1 отчёт для менеджера",
  "projectId": 1,
  "dueDate": "2026-04-15T10:00:00"
}
```

Обязательно только `title`. Остальные поля опциональны.

Поведение при создании:
- `status = ACTIVE` (если `dueDate` в прошлом или не указан)
- `status = BLOCKED` (если `dueDate` в будущем)
- `inbox = false` всегда

**Response `201 Created`:** объект задачи (см. выше).

---

#### Обновить задачу

```
PUT /api/tasks/{id}
```

**Body:** те же поля, что при создании.

```json
{
  "title": "Написать финальный отчёт",
  "description": "Обновлённое описание",
  "projectId": 2,
  "dueDate": null
}
```

**Response `200 OK`:** обновлённый объект задачи.

---

#### Удалить задачу

```
DELETE /api/tasks/{id}
```

**Response `204 No Content`**

---

#### Завершить задачу

```
PATCH /api/tasks/{id}/complete
```

Тело не нужно.

Результат:
- `status = COMPLETED`
- `inbox = false` (автоматически убирается из Inbox)

**Response `200 OK`:** обновлённый объект задачи.

---

#### Добавить задачу в Inbox

```
PATCH /api/tasks/{id}/inbox
```

Тело не нужно.

Результат: `inbox = true`.

**Response `200 OK`:** обновлённый объект задачи.

---

#### Убрать задачу из Inbox

```
PATCH /api/tasks/{id}/uninbox
```

Тело не нужно.

Результат: `inbox = false`. Задача остаётся в общем списке.

**Response `200 OK`:** обновлённый объект задачи.

---

### Inbox

#### Получить Inbox

```
GET /api/inbox
```

Возвращает задачи где `inbox = true` и `status != COMPLETED`.

**Response `200 OK`:** массив объектов задач (см. формат выше).

---

## Фильтрация задач

Параметры можно комбинировать:

```
GET /api/tasks?projectId=1&status=ACTIVE&search=отчёт&sortBy=dueDate&sortDir=asc
```

| `sortBy`    | Описание              |
|-------------|-----------------------|
| `title`     | По названию (A→Z)     |
| `dueDate`   | По дате выполнения    |
| `createdAt` | По дате создания      |

---

## Ошибки

Все ошибки возвращаются в едином формате:

```json
{
  "status": 404,
  "message": "Task not found with id: 99",
  "timestamp": "2026-04-11T12:00:00"
}
```

| HTTP-статус | Когда возникает                                      |
|-------------|------------------------------------------------------|
| `400`       | Невалидное тело запроса или бизнес-ошибка            |
| `401`       | Нет токена или неверные credentials                  |
| `404`       | Ресурс не найден или не принадлежит пользователю     |
| `500`       | Внутренняя ошибка сервера                            |
