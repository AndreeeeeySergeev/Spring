# 🏨 Система бронирования отелей (Hotel Booking System)

> Микросервисная система для управления бронированиями с распределёнными транзакциями, равномерной загрузкой номеров и комплексной безопасностью

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

---

## 🌟 Основные возможности

- ✅ **Распределённые транзакции** — Saga Pattern с компенсацией
- ✅ **Равномерная загрузка номеров** — алгоритм распределения без «простоя»
- ✅ **Идемпотентность** — защита от дублирования операций
- ✅ **Resilience** — Retry, Circuit Breaker, Timeout
- ✅ **JWT Authentication** — безопасность на всех уровнях
- ✅ **Service Discovery** — Eureka для динамического обнаружения сервисов
- ✅ **API Gateway** — единая точка входа с маршрутизацией
- ✅ **Тестовый стенд** — интерактивный UI для демонстрации

---

## 📋 Содержание

- [Технологический стек](#-технологический-стек)
- [Архитектура](#-архитектура)
- [Быстрый старт](#-быстрый-старт)
- [Структура проекта](#-структура-проекта)
- [API Документация](#-api-документация)
- [Ключевые особенности](#-ключевые-особенности)
- [Тестирование](#-тестирование)
- [Развертывание](#-развертывание)

---

## 🛠 Технологический стек

### Backend
- **Java 17** — современная версия Java
- **Spring Boot 3.4.6** — фреймворк для быстрой разработки
- **Spring Cloud Gateway** — маршрутизация и API Gateway
- **Spring Cloud Netflix Eureka** — Service Discovery
- **Spring Data JPA** — работа с базами данных
- **H2 Database** — in-memory база данных для разработки
- **Spring Security + JWT** — аутентификация и авторизация
- **Resilience4j** — Retry, Circuit Breaker для отказоустойчивости
- **OpenFeign** — HTTP клиент для межсервисного взаимодействия

### Инструменты
- **Maven** — управление зависимостями и сборка
- **JUnit 5** — модульное тестирование
- **MockMvc** — тестирование REST API
- **SpringDoc OpenAPI** — автоматическая документация API (Swagger)
- **Lombok** — уменьшение boilerplate кода

---

## 🏗 Архитектура

### Диаграмма микросервисов

```
┌─────────────────────────────────────────────────────────┐
│                    Клиент (Browser)                      │
└───────────────────────┬─────────────────────────────────┘
                        │
                        ▼
            ┌───────────────────────┐
            │   API Gateway :8080   │  ← Единая точка входа
            │  (Spring Cloud)       │     Тестовый стенд: / или /ui
            └───────────┬───────────┘
                        │
            ┌───────────┴───────────┐
            │                       │
            ▼                       ▼
    ┌──────────────┐      ┌──────────────┐
    │   Booking    │      │    Hotel     │
    │  Service     │      │   Service    │
    │    :8081     │◄─────│    :8082     │  ← Feign Client
    └──────┬───────┘      └──────┬───────┘
           │                     │
           ▼                     ▼
    ┌──────────┐          ┌──────────┐
    │H2 Memory │          │H2 Memory │
    │booking-db│          │hotel-db  │
    └──────────┘          └──────────┘
           │                     │
           └──────────┬──────────┘
                      ▼
            ┌──────────────────┐
            │ Eureka Server    │  ← Service Discovery
            │    :8761         │
            └──────────────────┘
```

### Компоненты системы

#### 1. **API Gateway** (`:8080`)
- **Назначение**: Единая точка входа для всех запросов
- **Возможности**:
  - Маршрутизация запросов к микросервисам
  - Трассировка запросов (X-Trace-Id)
  - CORS настройки
  - Интерактивный тестовый стенд на главной странице
- **Маршруты**:
  - `/api/bookings/**`, `/api/user/**`, `/api/booking/**` → Booking Service
  - `/api/hotels/**`, `/api/rooms/**` → Hotel Service

#### 2. **Booking Service** (`:8081`)
- **Назначение**: Управление пользователями и бронированиями
- **База данных**: `booking-db` (H2 in-memory)
- **Основные функции**:
  - Регистрация и аутентификация пользователей (JWT)
  - CRUD операции с бронированиями
  - Saga Pattern для распределённых транзакций
  - Идемпотентность через `requestId`

#### 3. **Hotel Service** (`:8082`)
- **Назначение**: Управление отелями, номерами и доступностью
- **База данных**: `hotel-db` (H2 in-memory)
- **Основные функции**:
  - CRUD операции с отелями и номерами
  - Алгоритм равномерной загрузки номеров
  - Статистика загруженности (`times_booked`)
  - Потокобезопасное управление доступностью

#### 4. **Eureka Server** (`:8761`)
- **Назначение**: Service Discovery и регистрация микросервисов
- **URL**: `http://localhost:8761`

---

## 🚀 Быстрый старт

### Требования

- **Java 17** или выше
- **Maven 3.6+**
- **Порты**: 8080, 8081, 8082, 8761 (должны быть свободны)

### Пошаговая инструкция

#### 1. Клонирование и сборка проекта

```bash
# Перейти в директорию проекта
cd demo1

# Сборка всех модулей
mvn clean install
```

#### 2. Запуск сервисов

**Важно**: Запускайте сервисы в следующем порядке:

##### Шаг 1: Eureka Server
```bash
cd eureka-server
mvn spring-boot:run
# Проверьте: http://localhost:8761
```

##### Шаг 2: Hotel Service
```bash
cd hotel-service
mvn spring-boot:run
# Сервис запущен на :8082
```

##### Шаг 3: Booking Service
```bash
cd booking-service
mvn spring-boot:run
# Сервис запущен на :8081
```

##### Шаг 4: API Gateway
```bash
cd api-gateway
mvn spring-boot:run
# Gateway запущен на :8080
```

#### 3. Доступ к системе

- **Тестовый стенд**: http://localhost:8080 или http://localhost:8080/ui
- **Eureka Dashboard**: http://localhost:8761
- **Swagger UI (Booking)**: http://localhost:8081/swagger-ui.html
- **Swagger UI (Hotel)**: http://localhost:8082/swagger-ui.html

### Предустановленные данные

При первом запуске автоматически создаются:

**Booking Service:**
- Пользователь: `admin` / `admin` (роль: ADMIN)
- Пользователь: `user` / `user` (роль: USER)

**Hotel Service:**
- Отель "Grand Plaza" с номерами 101, 102
- Отель "City Inn" с номером 201
- Номера имеют разные значения `times_booked` для демонстрации алгоритма

---

## 📁 Структура проекта

```
demo1/
├── api-gateway/                 # API Gateway
│   ├── src/main/java/
│   │   └── com/mephi/task/gateway/
│   │       ├── config/          # SecurityConfig
│   │       └── web/             # HomeController, TraceFilter
│   └── src/main/resources/
│       ├── application.yml      # Конфигурация маршрутов
│       └── static/
│           └── index.html       # Тестовый стенд
│
├── booking-service/             # Сервис бронирований
│   ├── src/main/java/
│   │   └── com/mephi/task/booking/
│   │       ├── client/          # Feign клиенты (HotelClient)
│   │       ├── config/          # Security, Swagger, Feign
│   │       ├── domain/          # User, Booking, BookingStatus
│   │       ├── repo/            # Репозитории
│   │       ├── security/       # JWT фильтры и сервисы
│   │       ├── service/          # Бизнес-логика
│   │       └── web/              # Контроллеры и DTO
│   └── src/test/java/           # Тесты (5 файлов, 15+ тестов)
│
├── hotel-service/               # Сервис отелей
│   ├── src/main/java/
│   │   └── com/mephi/task/hotel/
│   │       ├── config/          # Security, Swagger
│   │       ├── domain/          # Hotel, Room, RoomHold
│   │       ├── repo/            # Репозитории
│   │       ├── security/        # JWT фильтры
│   │       ├── service/         # AvailabilityService
│   │       └── web/             # Контроллеры и DTO
│   └── src/test/java/           # Тесты (3 файла, 8+ тестов)
│
├── eureka-server/               # Service Discovery
│   └── src/main/java/
│       └── com/mephi/task/eureka/
│
└── pom.xml                      # Parent POM
```

---

## 📖 API Документация

### Аутентификация

Все запросы (кроме регистрации и входа) требуют JWT токен в заголовке:
```
Authorization: Bearer <your-jwt-token>
```

#### Регистрация и вход

```bash
# Регистрация
POST http://localhost:8080/api/user/register
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}

# Авторизация
POST http://localhost:8080/api/user/auth
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

**Ответ:** Возвращает JWT токен, действителен 1 час.

---

### Booking Service (через Gateway `:8080`)

#### Бронирования

| Метод | Эндпойнт | Роль | Описание |
|-------|----------|------|----------|
| POST | `/api/booking` | USER | Создать бронирование |
| GET | `/api/bookings` | USER | История бронирований (пагинация) |
| GET | `/api/booking/{id}` | USER | Получить бронирование по ID |
| DELETE | `/api/booking/{id}` | USER | Отменить бронирование |

#### Пользователи (Admin)

| Метод | Эндпойнт | Роль | Описание |
|-------|----------|------|----------|
| POST | `/api/user` | ADMIN | Создать пользователя |
| PATCH | `/api/user` | ADMIN | Обновить пользователя |
| DELETE | `/api/user` | ADMIN | Удалить пользователя |

#### Примеры запросов

**Создание бронирования с автоподбором:**
```bash
POST http://localhost:8080/api/booking
Authorization: Bearer <token>
Content-Type: application/json

{
  "startDate": "2025-11-01",
  "endDate": "2025-11-05",
  "autoSelect": true,
  "requestId": "req-12345"
}
```

**Создание бронирования с выбором номера:**
```bash
POST http://localhost:8080/api/booking
Authorization: Bearer <token>
Content-Type: application/json

{
  "startDate": "2025-11-01",
  "endDate": "2025-11-05",
  "autoSelect": false,
  "roomId": 1,
  "requestId": "req-67890"
}
```

**Получение истории бронирований:**
```bash
GET http://localhost:8080/api/bookings?page=0&size=20
Authorization: Bearer <token>
```

---

### Hotel Service (через Gateway `:8080`)

#### Отели

| Метод | Эндпойнт | Роль | Описание |
|-------|----------|------|----------|
| POST | `/api/hotels` | ADMIN | Создать отель |
| GET | `/api/hotels` | USER, ADMIN | Список отелей |
| PATCH | `/api/hotels/{id}` | ADMIN | Обновить отель |
| DELETE | `/api/hotels/{id}` | ADMIN | Удалить отель |

#### Номера

| Метод | Эндпойнт | Роль | Описание |
|-------|----------|------|----------|
| POST | `/api/rooms` | ADMIN | Создать номер |
| GET | `/api/rooms` | USER, ADMIN | Список свободных номеров (с фильтрами) |
| GET | `/api/rooms/recommend` | USER, ADMIN | Рекомендованные номера (по `times_booked`) |
| GET | `/api/rooms/stats` | USER, ADMIN | Статистика загруженности |
| PATCH | `/api/rooms/{id}` | ADMIN | Обновить номер |
| DELETE | `/api/rooms/{id}` | ADMIN | Удалить номер |

#### Примеры запросов

**Получение рекомендованных номеров:**
```bash
GET http://localhost:8080/api/rooms/recommend?start=2025-11-01&end=2025-11-05
Authorization: Bearer <token>
```

**Получение статистики:**
```bash
GET http://localhost:8080/api/rooms/stats
Authorization: Bearer <token>
```

**Фильтрация номеров:**
```bash
GET http://localhost:8080/api/rooms?start=2025-11-01&end=2025-11-05&hotelId=1&sortBy=timesBooked&direction=asc
Authorization: Bearer <token>
```

---

## 🎯 Ключевые особенности

### 1. Saga Pattern (Распределённые транзакции)

Система использует **двухшаговую согласованность** между сервисами:

```
┌─────────────────────────────────────────────────────────┐
│  Шаг 1: PENDING                                         │
│  └─ Booking Service создаёт бронирование в статусе     │
│     PENDING и сохраняет в локальной БД                  │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│  Шаг 2a: CONFIRMED (успех)                             │
│  └─ Вызов Hotel Service для подтверждения              │
│     └─ Обновление статуса на CONFIRMED                 │
└─────────────────────────────────────────────────────────┘

                 │
                 ▼ (ошибка/тайм-аут)
┌─────────────────────────────────────────────────────────┐
│  Шаг 2b: CANCELLED (компенсация)                       │
│  └─ Обновление статуса на CANCELLED                    │
│  └─ Вызов компенсации: POST /internal/rooms/{id}/release│
└─────────────────────────────────────────────────────────┘
```

**Особенности:**
- ✅ Локальные транзакции (не используются глобальные distributed transactions)
- ✅ Автоматическая компенсация при ошибках
- ✅ Тайм-ауты и повторы (Resilience4j)
- ✅ Идемпотентность через `requestId`

### 2. Алгоритм равномерной загрузки номеров

Система ведёт статистику бронирований (`times_booked`) и при автоподборе выбирает номер с наименьшей загрузкой:

```java
// Сортировка по возрастанию times_booked, затем по id
rooms.stream()
    .sorted(Comparator.comparingLong(Room::getTimesBooked)
                      .thenComparing(Room::getId))
    .collect(Collectors.toList());
```

**Результат**: Равномерное распределение бронирований без «простоя» номеров.

### 3. Идемпотентность

Каждый запрос на создание бронирования должен содержать уникальный `requestId`:

```json
{
  "startDate": "2025-11-01",
  "endDate": "2025-11-05",
  "autoSelect": true,
  "requestId": "req-unique-id-123"
}
```

**Механизм:**
- При первом запросе создаётся новое бронирование
- При повторном запросе с тем же `requestId` возвращается существующее бронирование
- Защита от дубликатов при сетевых ошибках и повторах

### 4. Resilience (Отказоустойчивость)

Система использует **Resilience4j** для устойчивости к сбоям:

**Retry:**
- Максимум 3 попытки
- Интервал между попытками: 1 секунда

**Circuit Breaker:**
- Порог ошибок: 50%
- Минимум вызовов: 5
- Время в открытом состоянии: 10 секунд

**Timeout:**
- Connect timeout: 2 секунды
- Read timeout: 2 секунды

### 5. Потокобезопасность

Для предотвращения гонок данных используется **Pessimistic Lock**:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Room findByIdForUpdate(Long id);
```

При параллельных запросах на один номер только один запрос успешно создаст бронирование.

### 6. Безопасность

- **JWT аутентификация** на всех сервисах
- **Роли**: USER (бронирования) и ADMIN (управление)
- **Методная безопасность**: `@PreAuthorize("hasRole('USER')")`
- **Resource Server**: каждый сервис валидирует JWT независимо

---

## 🧪 Тестирование

### Запуск тестов

**Booking Service:**
```bash
cd booking-service
mvn test
```

**Hotel Service:**
```bash
cd hotel-service
mvn test
```

### Покрытие тестами

| Сервис | Файлов | Тестов | Покрытие |
|--------|--------|--------|----------|
| Booking Service | 5 | 15+ | ✅ Полное |
| Hotel Service | 3 | 8+ | ✅ Полное |
| **Всего** | **8** | **23+** | **✅** |

#### Тесты Booking Service

- ✅ `AuthControllerTests` — авторизация и регистрация
- ✅ `BookingControllerTests` — CRUD операции, валидация
- ✅ `BookingSagaTests` — Saga Pattern и компенсация
- ✅ `BookingTimeoutTests` — обработка тайм-аутов и идемпотентность
- ✅ `BookingFlowTests` — успешный сценарий PENDING → CONFIRMED

#### Тесты Hotel Service

- ✅ `AvailabilityConcurrencyTests` — потокобезопасность
- ✅ `LoadBalancingTests` — алгоритм равномерной загрузки (3 теста)
- ✅ `RoomControllerStatsTests` — статистика загруженности

---

## 🖥 Тестовый стенд

Интерактивный веб-интерфейс доступен на главной странице Gateway:

**URL**: http://localhost:8080 или http://localhost:8080/ui

### Возможности тестового стенда:

1. **🔐 Авторизация** — регистрация и вход пользователей
2. **🏨 Управление отелями** — просмотр списка отелей
3. **🛏️ Комнаты** — просмотр свободных и рекомендованных номеров
4. **📅 Создание бронирований** — с визуализацией Saga Pattern
5. **📊 Статистика** — графики загруженности номеров
6. **📋 История бронирований** — просмотр всех бронирований пользователя
7. **🔄 Тест идемпотентности** — проверка повторных запросов
8. **📝 Логи процесса** — отслеживание всех операций в реальном времени

### Демонстрация:

- ✅ **Saga Flow**: Визуализация переходов PENDING → CONFIRMED/CANCELLED
- ✅ **Алгоритм загрузки**: Сортировка номеров по `times_booked`
- ✅ **Идемпотентность**: Повторные запросы с тем же `requestId`

---

## 📊 Мониторинг и логирование

### Логирование

Система использует структурированное логирование с корреляцией:

- **Trace ID**: Передаётся через Gateway в заголовке `X-Trace-Id`
- **Booking ID**: Уникальный идентификатор бронирования
- **Request ID**: Для идемпотентности
- **Correlation ID**: Для межсервисных вызовов

Пример логов:
```
[INFO] gateway incoming GET /api/bookings traceId=abc-123
[INFO] createPending requestId=req-123, userId=1, roomId=1
[INFO] confirm bookingId=1 correlationId=corr-456
[INFO] confirmed bookingId=1 correlationId=corr-456
```

### Eureka Dashboard

Мониторинг зарегистрированных сервисов:
- URL: http://localhost:8761
- Статус здоровья сервисов
- Список инстансов

---

## 🚢 Развертывание

### Локальная разработка

Все сервисы используют H2 in-memory базу данных и не требуют дополнительной настройки.

### Production готовность

Для production потребуется:

1. **База данных**: Замена H2 на PostgreSQL/MySQL
2. **Конфигурация**: Обновление `application.yml` с реальными параметрами БД
3. **Eureka**: Настройка для production окружения
4. **Мониторинг**: Интеграция с Prometheus/Grafana (опционально)
5. **Контейнеризация**: Docker Compose для оркестрации

### Docker Compose (пример)

```yaml
version: '3.8'
services:
  eureka-server:
    build: ./eureka-server
    ports:
      - "8761:8761"
  
  hotel-service:
    build: ./hotel-service
    depends_on:
      - eureka-server
    ports:
      - "8082:8082"
  
  booking-service:
    build: ./booking-service
    depends_on:
      - eureka-server
      - hotel-service
    ports:
      - "8081:8081"
  
  api-gateway:
    build: ./api-gateway
    depends_on:
      - eureka-server
    ports:
      - "8080:8080"
```

---

## 📚 Дополнительные материалы

- **Swagger UI**: Доступен на каждом сервисе (`/swagger-ui.html`)
- **CRITERIA_AUDIT.md**: Детальная проверка по критериям оценивания
- **FINAL_CHECK.md**: Финальная проверка готовности проекта

---

## 👥 Поддержка

При возникновении проблем проверьте:

1. ✅ Все сервисы запущены (Eureka → Hotel → Booking → Gateway)
2. ✅ Порты свободны (8080, 8081, 8082, 8761)
3. ✅ Java 17+ установлена
4. ✅ Maven установлен и настроен

---

## 📄 Лицензия

MIT License

---

## 🎓 Учебный проект

Проект разработан в рамках курса "Фреймворк Spring и работа с REST API" для демонстрации:
- Микросервисной архитектуры
- Распределённых транзакций (Saga Pattern)
- Алгоритмов планирования ресурсов
- Отказоустойчивости и безопасности

---

**Made with ❤️ using Spring Boot**