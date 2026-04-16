## Running Tests

To execute tests using Docker:

```bash
docker run --rm -v "$(pwd):/app" -w /app maven:3.9.11-eclipse-temurin-21 mvn test -pl services/auth-service -Dtest=AuthControllerTest
```

---

## User Registration

**Endpoint:**

```
POST http://localhost:8081/api/auth/register
```

**Request body:**

```json
{
  "username": "user",
  "email": "user@gmail.com",
  "password": "password"
}
```

---

## Check Users in Database

To verify users stored in the database (make sure the database container is running):

```bash
docker exec -it compose-postgres-1 psql -U user -d database -c "SELECT * FROM users;"
```

---

## Login

Open in browser:

```
http://localhost:8081/login
```

---

## Get Current User Data

After logging in, you can retrieve the currently authenticated user:

```
http://localhost:8081/api/user/me
```
