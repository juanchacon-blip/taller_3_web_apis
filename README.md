# Proyecto 03 – Web, API y Microservicios

Aplicación web basada en **microservicios Spring Boot (Java 17)** con **API REST** (POST, GET, PUT, DELETE), persistencia en **PostgreSQL**, empaquetada con **Docker** y desplegable en **Kubernetes**, con seguridad **JWT firmado con RSA (RS256)** y **OAuth 2.0 (Resource Server)**.

## Arquitectura

```
                    ┌──────────────────────┐
  POST /api/auth/*  │     auth-service     │   JWKS (llave pública)
 ─────────────────► │  :8081  (emite JWT)  │ ◄───────────────┐
                    └──────────┬───────────┘                 │
                               │                             │ valida firma RS256
                               ▼                             │
                    ┌──────────────────────┐     ┌───────────┴──────────┐
                    │ PostgreSQL  (authdb) │     │     task-service     │ ◄── GET/POST/PUT/DELETE
                    │            (taskdb)  │ ◄───┤ :8082 (Resource Srv) │     /api/tasks  (Bearer JWT)
                    └──────────────────────┘     └──────────────────────┘
```

| Servicio | Puerto | Rol |
|---|---|---|
| `auth-service` | 8081 | Registro, login (emite JWT RS256) y publicación de la llave pública en `/.well-known/jwks.json`. Contraseñas con BCrypt. |
| `task-service` | 8082 | CRUD de tareas. OAuth 2.0 Resource Server: valida cada JWT (firma, expiración, issuer) contra el JWKS de `auth-service`. |
| `postgres` | 5432 | Una base de datos por microservicio (`authdb`, `taskdb`). |
| `frontend` | 80 (8080 en Compose) | Página web (HTML + JS) servida por nginx. Reenvía `/api/auth/*` a `auth-service` y `/api/tasks*` a `task-service`, por lo que el navegador solo habla con un origen (sin CORS). |

## Cumplimiento de la rúbrica

1. **Dockerfile en cada microservicio + despliegue con Kubernetes**
   - `auth-service/Dockerfile`, `task-service/Dockerfile` (multi-stage, usuario no root).
   - `docker-compose.yml` para desarrollo local y `k8s/` para Kubernetes (Namespace, Secrets, Deployments, Services, PVC, probes de salud).
2. **Seguridad web y de APIs con JWT RSA y OAuth 2.0**
   - `auth-service` firma tokens con **RS256** (llave privada RSA 2048) y responde en formato OAuth 2.0 (`access_token`, `token_type`, `expires_in`).
   - `task-service` es **OAuth 2.0 Resource Server**: valida el Bearer token con la llave pública (JWKS).
   - Autorización por roles (`USER` solo ve sus tareas, `ADMIN` ve y modifica todas), sesiones stateless, BCrypt, validación de entrada.
3. **Repositorio en GitHub** – ver sección final.

## Requisitos

Docker (con Compose). Para Kubernetes: `kubectl` + minikube, kind o Docker Desktop con Kubernetes. Para compilar sin Docker: JDK 17 y Maven.

## Ejecución con Docker Compose

```bash
docker compose up --build
```

Abre la página web en **http://localhost:8080**: crea una cuenta, inicia sesión y gestiona tus tareas (crear, editar, completar, borrar).

> En este modo `auth-service` genera un par RSA nuevo al arrancar; si lo reinicias, los tokens anteriores dejan de ser válidos. En Kubernetes las llaves vienen de un Secret y son estables.

## Ejecución en Kubernetes

```bash
./scripts/deploy-k8s.sh
```

El script construye las imágenes, genera las llaves RSA (`keys/`, ignoradas por Git), crea el Secret `jwt-keys` y aplica los manifiestos de `k8s/`. La página web queda en NodePort `30080` y los servicios en `30081` (auth) y `30082` (tasks). Si no puedes alcanzar los NodePort, deja esto corriendo en una terminal y abre **http://localhost:8080**:

```bash
kubectl port-forward -n microservicios svc/frontend 8080:80
```

Para probar las APIs directamente con `curl` (sección siguiente) también puedes exponerlas:

```bash
kubectl port-forward -n microservicios svc/auth-service 8081:8081
kubectl port-forward -n microservicios svc/task-service 8082:8082
```

Escalar: `kubectl scale deployment/task-service --replicas=2 -n microservicios` (haz el escalado después de que el primer pod haya arrancado, porque Hibernate crea las tablas al inicio).

## Probar la API

```bash
# 1) Registrar usuario
curl -X POST localhost:8081/api/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"maria","password":"Password123"}'

# 2) Login -> token
TOKEN=$(curl -s -X POST localhost:8081/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"maria","password":"Password123"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')

# 3) CRUD de tareas
curl -X POST   localhost:8082/api/tasks -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Entregar proyecto 03","description":"Subir enlace de GitHub"}'
curl           localhost:8082/api/tasks -H "Authorization: Bearer $TOKEN"
curl -X PUT    localhost:8082/api/tasks/1 -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Entregar proyecto 03","description":"Listo","completed":true}'
curl -X DELETE localhost:8082/api/tasks/1 -H "Authorization: Bearer $TOKEN"

# Sin token -> 401
curl -i localhost:8082/api/tasks
```

Usuario administrador inicial: `admin` / `Admin12345!` (configurable con `ADMIN_USERNAME` y `ADMIN_PASSWORD`; cámbialo fuera de desarrollo).

### Endpoints

| Servicio | Método | Ruta | Acceso |
|---|---|---|---|
| auth | POST | `/api/auth/register` | público |
| auth | POST | `/api/auth/login` | público |
| auth | GET | `/.well-known/jwks.json` | público (llave pública) |
| tasks | GET | `/api/tasks`, `/api/tasks/{id}` | Bearer JWT |
| tasks | POST | `/api/tasks` | Bearer JWT |
| tasks | PUT | `/api/tasks/{id}` | Bearer JWT (dueño o ADMIN) |
| tasks | DELETE | `/api/tasks/{id}` | Bearer JWT (dueño o ADMIN) |

## Variables de entorno principales

| Variable | Servicio | Descripción |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | ambos | Conexión a PostgreSQL |
| `JWT_ISSUER` | ambos | Debe coincidir en los dos servicios |
| `JWT_PRIVATE_KEY_PATH`, `JWT_PUBLIC_KEY_PATH` | auth | PEM PKCS#8 / X.509. Si se omiten, se genera un par efímero |
| `JWT_TTL_SECONDS` | auth | Vigencia del token (3600 por defecto) |
| `JWT_JWKS_URI` | tasks | URL del JWKS de auth-service |

## Estructura

```
├── auth-service/        # Spring Boot + Dockerfile
├── task-service/        # Spring Boot + Dockerfile
├── frontend/            # Página web + nginx + Dockerfile
├── k8s/                 # Manifiestos de Kubernetes
├── scripts/             # generate-keys.sh, deploy-k8s.sh
├── db/init.sql          # Crea authdb y taskdb
└── docker-compose.yml
```

## Publicar en GitHub

Cada integrante debe tener el proyecto en **su propio** repositorio:

```bash
git init
git add .
git commit -m "Proyecto 03: microservicios con Spring Boot, Docker, Kubernetes y JWT RSA"
git branch -M main
git remote add origin https://github.com/<tu-usuario>/<tu-repositorio>.git
git push -u origin main
```

Las llaves (`keys/`) y archivos `.env` están en `.gitignore`: no los subas.
# taller_3_web_apis
