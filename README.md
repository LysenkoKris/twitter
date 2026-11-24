# Тввитер

Три сервиса: пользователи (`ums`), сообщения (`mms`), подписки (`sms`).

## Тесты

```bash
./mvnw test
```

База для тестов поднимается сама, Docker не нужен.

## Сервисы

```bash
docker compose up -d
./mvnw -pl ums spring-boot:run
./mvnw -pl sms spring-boot:run
./mvnw -pl mms spring-boot:run
```

Пользователи: `8080`, MySQL `localhost:3306/ums`. Сообщения: `8084`, подписки: `8085`, PostgreSQL `localhost:5435` (базы `messages` и `subscribers`). Пользователь и пароль баз: `ums`. Администратор: `admin` / `admin12345`.

Сессия передаётся заголовком `X-Session-Id`.
