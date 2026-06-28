# TaskFlow Backend

Java 21 + Spring Boot 3.3 + MySQL backend for TaskFlow.

## Run

```bash
# MySQL 8 must be running.
mysql -uroot -padmin -e "CREATE DATABASE IF NOT EXISTS flowbuddy CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
cd backend
mvn spring-boot:run
```

The server starts at **http://localhost:8080**. Hibernate creates and updates
the tables automatically.

On first start, a super admin is seeded:

- username: `super_admin`
- password: `super1234!`

## Configuration

| Variable | Purpose | Default |
| --- | --- | --- |
| `JWT_SECRET` | HS256 signing key (32+ chars) | development value |
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL | `jdbc:mysql://localhost:3306/flowbuddy...` |
| `SPRING_DATASOURCE_USERNAME` | DB user | `root` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `admin` |
| `UPLOAD_DIR` | Chat upload directory | `uploads` |
| `CLOUDINARY_URL` | Cloudinary authenticated media storage URL | — |
| `GEMINI_API_KEY` | Gemini API key used by `@flowa` | — |
| `GEMINI_MODEL` | Gemini model name | `gemini-2.5-flash` |
| `LIVEKIT_URL` | LiveKit server URL | — |
| `LIVEKIT_API_KEY` | LiveKit API key | — |
| `LIVEKIT_API_SECRET` | LiveKit API secret | — |
| `CORS_ORIGINS` | Comma-separated allowed origins | `*` |

## Main endpoints

- `POST /api/auth/login` – login and JWT
- `GET/PUT /api/auth/me` – current user profile
- `GET/POST /api/users`, `GET/PUT/DELETE /api/users/{id}`
- `GET/POST /api/boards`, `GET /api/boards/{id}`
- `POST /api/boards/{id}/columns`, `POST /api/boards/{id}/tasks`
- `PUT/DELETE /api/tasks/{id}`, `POST /api/tasks/{id}/move`
- `GET /api/notifications`, `PATCH /api/notifications/read-all`
- `GET/POST /api/meetings`, `DELETE /api/meetings/{id}`
- `GET/POST /api/chat/groups`
- `GET/POST /api/chat/groups/{id}/messages`
- `POST /api/chat/uploads` – image, video, audio, and file upload
- `WS /ws/chat?token=...&group=...` – real-time messages and typing state
