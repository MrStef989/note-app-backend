# Я Обезьяна — Backend

## Суть проекта
Персональный планировщик задач с двумя слоями:
- **Общий список** — база всех задач пользователя
- **Inbox** — текущий фокус: задачи, выбранные для выполнения сегодня

Inbox — это НЕ отдельная таблица, а выборка задач где `is_inbox = true`.

Идея: утром пользователь открывает общий список, выбирает задачи на день → они падают в Inbox.
Заблокированные задачи (с датой в будущем) автоматически активируются и попадают в Inbox когда дата наступает.

## Стек
- Java 17
- Spring Boot 3
- Spring Data JPA + Hibernate
- Spring Security + JWT
- PostgreSQL
- Maven
- Lombok
- MapStruct (DTO mapping)

## Архитектура
Монолит. Пакетная структура:
com.yaobezyana
├── auth          — регистрация, логин, JWT
├── user          — сущность пользователя
├── project       — проекты
├── task          — задачи, inbox, фильтрация
└── common        — исключения, утилиты

Слои: Controller → Service → Repository → Entity

## Сущности

### User
id, email, password_hash

### Project
id, title, user_id
При удалении проекта — задачи НЕ удаляются, у них сбрасывается project_id.

### Task
id
title
description        (nullable)
user_id
project_id         (nullable — задача без проекта)
status             ENUM: ACTIVE | COMPLETED | BLOCKED
is_inbox           boolean, default false
due_date           (nullable)
created_at
updated_at

## Бизнес-правила

**Создание задачи:**
- status = ACTIVE
- is_inbox = false
- project_id = null по умолчанию

**Завершение задачи:**
- status = COMPLETED
- is_inbox = false (автоматически убирать из Inbox)

**Назначение due_date в будущем:**
- status = BLOCKED
- is_inbox = false

**Наступление due_date (scheduled job):**
- status = ACTIVE
- is_inbox = true

**Удаление проекта:**
- задачи остаются, project_id = null

**Inbox:**
- Показывать только: is_inbox = true AND status != COMPLETED
- Добавить вручную: is_inbox = true
- Убрать вручную: is_inbox = false (задача остаётся в общем списке)

**Изоляция данных:**
- Все операции проверяют user_id
- Пользователь видит только свои задачи и проекты

## REST API
POST   /api/auth/register
POST   /api/auth/login
GET    /api/projects
POST   /api/projects
PUT    /api/projects/{id}
DELETE /api/projects/{id}
GET    /api/tasks                         # все задачи, фильтры: ?projectId=&status=
POST   /api/tasks
PUT    /api/tasks/{id}
DELETE /api/tasks/{id}
PATCH  /api/tasks/{id}/complete
PATCH  /api/tasks/{id}/inbox             # добавить в inbox
PATCH  /api/tasks/{id}/uninbox           # убрать из inbox
GET    /api/inbox                         # задачи где is_inbox=true и status!=COMPLETED

## Фильтрация и поиск задач
- по project_id
- по status
- поиск по title (case-insensitive, LIKE)
- сортировка по title или due_date

## Scheduled Job
Раз в час (или раз в день утром) проверять задачи где:
- status = BLOCKED
- due_date <= now()

→ переводить в status = ACTIVE, is_inbox = true

## Соглашения
- snake_case в БД, camelCase в Java
- DTO для всех входящих и исходящих данных (никогда не отдавать Entity напрямую)
- Все ошибки через @ControllerAdvice с понятными сообщениями
- Тесты в src/test/

## Что НЕ делаем сейчас
- Фронтенд
- Режимы пользователя (умный/обезьяна) — это логика фронта
- Повторяющиеся задачи (dailies) — второй этап
- Календарь — второй этап
- Заметки (не-задачи) — низкий приоритет
