# Library — Система управления библиотекой

Учебное консольное приложение на Spring Boot, демонстрирующее работу с JPA/Hibernate, транзакциями и паттернами доступа к данным.

## Стек технологий

- Java 17
- Spring Boot 3.4.4
- Spring Data JPA / Hibernate
- PostgreSQL 16 (prod) / H2 (тесты)
- Gradle 9

## Функциональность

### Управление книгами
- Добавление книги (название, автор, ISBN, год издания)
- Удаление книги (запрещено, если книга выдана)
- Просмотр всех книг
- Поиск по названию и автору (без учёта регистра)

### Управление читателями
- Регистрация читателя (имя, email)
- Удаление читателя (запрещено при наличии невозвращённых книг)
- Просмотр всех читателей с количеством активных выдач

### Выдача и возврат
- Выдача книги читателю (с защитой от гонки через `REPEATABLE_READ`)
- Приём возврата книги
- Просмотр всех активных выдач
- История выдач конкретного читателя

### Отчёты
- Топ-5 самых популярных книг
- Должники — читатели, не вернувшие книгу более 14 дней

### Демонстрация JPA-паттернов
- Загрузка книг с текущим читателем через `JOIN FETCH` (оптимизированный запрос)
- Загрузка книг с N+1 проблемой (для сравнения)

## Запуск через Docker

```bash
docker-compose up --build
```

Приложение запустится в интерактивном режиме. PostgreSQL поднимается автоматически.

> При первом запуске база заполняется демонстрационными данными: 4 книги, 2 читателя, несколько записей о выдачах (включая просроченную).

Чтобы остановить и удалить контейнеры:

```bash
docker-compose down
```

Чтобы также удалить данные БД:

```bash
docker-compose down -v
```

## Локальный запуск

Требуется запущенный PostgreSQL с базой `librarydb` и пользователем `postgres` / паролем `1234`.

```bash
./gradlew bootRun
```

## Запуск тестов

```bash
./gradlew test
```

Тесты используют H2 (in-memory) — PostgreSQL не нужен.

## Структура проекта

```
src/main/java/com/example/library/
├── LibraryApplication.java
├── command/
│   ├── LibraryConsoleRunner.java   # Интерактивное меню
│   └── DataInitializer.java        # Загрузка демо-данных
├── domain/
│   ├── model/                      # Book, Reader, BorrowRecord
│   ├── embeddable/                 # ISBN (value object)
│   └── repository/                 # Spring Data репозитории
├── dto/                            # BookDto, ReaderDto, BorrowRecordDto
├── service/                        # BookService, ReaderService, BorrowService,
│                                   # BorrowLogService, ReportService
└── exception/                      # Кастомные исключения
```

## Модель данных

| Сущность | Основные поля |
|---|---|
| `Book` | id, title, author, isbn (unique), publicationYear, borrowed |
| `Reader` | id, fullName, email (unique), membershipDate |
| `BorrowRecord` | id, book, reader, borrowDate, returnDate |
