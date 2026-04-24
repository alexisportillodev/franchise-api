# Franchise API

API REST reactiva para la gestión de franquicias, sucursales y productos. Construida con **Spring Boot WebFlux** siguiendo los principios de **Clean Architecture**.

---

## Tabla de contenidos

- [Descripción](#descripción)
- [Stack tecnológico](#stack-tecnológico)
- [Arquitectura](#arquitectura)
- [Modelos de dominio](#modelos-de-dominio)
- [Endpoints](#endpoints)
- [Requisitos previos](#requisitos-previos)
- [Ejecución con Docker (recomendado)](#ejecución-con-docker-recomendado)
- [Ejecución local sin Docker](#ejecución-local-sin-docker)
- [Ejemplos de uso](#ejemplos-de-uso)
- [Manejo de errores](#manejo-de-errores)
- [Estructura del proyecto](#estructura-del-proyecto)

---

## Descripción

Franchise API permite administrar una red de franquicias. Cada franquicia puede tener múltiples sucursales, y cada sucursal gestiona su propio catálogo de productos con control de stock.

Las operaciones principales son:

- Crear y renombrar franquicias
- Agregar y renombrar sucursales dentro de una franquicia
- Agregar, eliminar y actualizar productos (nombre y stock) en una sucursal
- Consultar el producto con mayor stock por sucursal para una franquicia dada

---

## Stack tecnológico

| Componente            | Tecnología                          |
|-----------------------|-------------------------------------|
| Lenguaje              | Java 21                             |
| Framework             | Spring Boot 3.5.13                  |
| Modelo reactivo       | Spring WebFlux (Reactor / Netty)    |
| Build tool            | Gradle (Kotlin DSL)                 |
| Reducción boilerplate | Lombok                              |
| Validación            | Jakarta Bean Validation             |
| Base de datos         | AWS DynamoDB / DynamoDB Local       |
| Testing               | JUnit 5, Mockito, Reactor Test      |
| Contenedores          | Docker + Docker Compose             |

---

## Arquitectura

El proyecto implementa **Clean Architecture** con cuatro capas bien definidas:

```
┌──────────────────────────────────────────┐
│           PRESENTATION (API)             │
│  Controllers · DTOs · Mappers · Handler  │
├──────────────────────────────────────────┤
│         APPLICATION (Use Cases)          │
│  CreateFranchise · AddBranch · etc.      │
├──────────────────────────────────────────┤
│            DOMAIN (Entities)             │
│  Franchise · Branch · Product · Ports    │
├──────────────────────────────────────────┤
│       INFRASTRUCTURE (Adapters)          │
│  FranchiseRepositoryImpl · BeanConfig    │
└──────────────────────────────────────────┘
```

**Regla de dependencias:** las capas externas dependen de las internas, nunca al revés. El dominio no conoce Spring ni ningún framework.

**Decisiones de diseño clave:**
- Los use cases retornan agregados de dominio.
- Los controllers proyectan esos agregados en DTOs específicos por recurso.
- Toda la comunicación HTTP es no bloqueante (`Mono<T>` / `Flux<T>`).

---

## Modelos de dominio

```
Franchise
├── id: String (UUID)
├── name: String
└── branches: List<Branch>
        ├── id: String (UUID)
        ├── name: String
        └── products: List<Product>
                ├── id: String (UUID)
                ├── name: String
                └── stock: int
```

---

## Endpoints

### Franquicias — `/franchises`

| Método | Ruta                          | Descripción                                  | Status |
|--------|-------------------------------|----------------------------------------------|--------|
| POST   | `/franchises`                 | Crear una nueva franquicia                   | 201    |
| PUT    | `/franchises/{id}`            | Actualizar el nombre de una franquicia       | 200    |
| POST   | `/franchises/{id}/branches`   | Agregar una sucursal a la franquicia         | 201    |
| GET    | `/franchises/{id}/top-stock`  | Producto con mayor stock por sucursal        | 200    |

### Sucursales — `/branches`

| Método | Ruta                      | Descripción                              | Status |
|--------|---------------------------|------------------------------------------|--------|
| PUT    | `/branches/{id}`          | Actualizar el nombre de una sucursal     | 200    |
| POST   | `/branches/{id}/products` | Agregar un producto a la sucursal        | 201    |

### Productos — `/products`

| Método | Ruta                    | Descripción                          | Status |
|--------|-------------------------|--------------------------------------|--------|
| DELETE | `/products/{id}`        | Eliminar un producto de una sucursal | 200    |
| PUT    | `/products/{id}/stock`  | Actualizar el stock de un producto   | 200    |
| PUT    | `/products/{id}/name`   | Actualizar el nombre de un producto  | 200    |

---

## Requisitos previos

### Para ejecutar con Docker (recomendado)

- **Docker Desktop** — [Descargar](https://www.docker.com/products/docker-desktop/)
- **Git**

No se necesita Java ni Gradle instalados localmente; el build ocurre dentro del contenedor.

### Para ejecutar sin Docker

- **Java 21** — [Descargar](https://adoptium.net/)
- **Git**

---

## Ejecución con Docker (recomendado)

Este es el método más sencillo. Levanta dos contenedores:

| Contenedor      | Imagen                          | Puerto |
|-----------------|---------------------------------|--------|
| `dynamodb-local`| `amazon/dynamodb-local:2.5.2`   | 8000   |
| `franchise-api` | Construida desde el `Dockerfile`| 8080   |

Un tercer contenedor (`dynamodb-init`) crea la tabla `franchise` automáticamente al iniciar y luego se detiene.

### Paso 1 — Clonar el repositorio

```bash
git clone https://github.com/alexisportillodev/franchise-api.git
cd franchise-api
```

### Paso 2 — Construir y levantar los contenedores

```bash
docker compose up --build
```

> La primera vez tarda unos minutos porque descarga las imágenes base y compila el JAR dentro del contenedor. Las siguientes ejecuciones son mucho más rápidas gracias al caché de capas.

Cuando veas esta línea en los logs, la API está lista:

```
franchise-api  | Started FranchiseApiApplication in X.XXX seconds
```

### Paso 3 — Verificar que todo está corriendo

```bash
docker compose ps
```

Deberías ver `dynamodb-local` y `franchise-api` con estado `running`.

### Paso 4 — Probar la API

```bash
curl -X POST http://localhost:8080/franchises \
  -H "Content-Type: application/json" \
  -d '{"name": "Mi Franquicia"}'
```

### Detener los contenedores

```bash
docker compose down
```

> Los datos se pierden al detener porque DynamoDB Local corre en modo `inMemory`. Esto es intencional para el entorno de desarrollo.

### Reconstruir la imagen después de cambios en el código

```bash
docker compose up --build
```

---

## Ejecución local sin Docker

Usa este método si prefieres correr la aplicación directamente con Gradle. En este modo la persistencia es **in-memory** (no requiere DynamoDB).

### Paso 1 — Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd franchise-api
```

### Paso 2 — Compilar el proyecto

```bash
# Linux / macOS
./gradlew build

# Windows
gradlew.bat build
```

### Paso 3 — Ejecutar la aplicación

```bash
# Linux / macOS
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

La aplicación arranca en `http://localhost:8080`.

### Paso 4 — Ejecutar los tests

```bash
# Linux / macOS
./gradlew test

# Windows
gradlew.bat test
```

### Generar y ejecutar el JAR directamente

```bash
./gradlew bootJar
java -jar build/libs/franchise-api-0.0.1-SNAPSHOT.jar
```

---

## Ejemplos de uso

### Crear una franquicia

```bash
curl -X POST http://localhost:8080/franchises \
  -H "Content-Type: application/json" \
  -d '{"name": "McDonald'\''s"}'
```

**Respuesta `201 Created`:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "McDonald's",
  "branches": []
}
```

---

### Agregar una sucursal

```bash
curl -X POST http://localhost:8080/franchises/550e8400-e29b-41d4-a716-446655440000/branches \
  -H "Content-Type: application/json" \
  -d '{"name": "Sucursal Centro"}'
```

**Respuesta `201 Created`:**
```json
{
  "id": "branch-uuid-001",
  "name": "Sucursal Centro",
  "products": []
}
```

---

### Agregar un producto a una sucursal

```bash
curl -X POST http://localhost:8080/branches/branch-uuid-001/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Big Mac", "stock": 50}'
```

**Respuesta `201 Created`:**
```json
{
  "id": "product-uuid-001",
  "name": "Big Mac",
  "stock": 50
}
```

---

### Actualizar el stock de un producto

```bash
curl -X PUT http://localhost:8080/products/product-uuid-001/stock \
  -H "Content-Type: application/json" \
  -d '{"stock": 120}'
```

**Respuesta `200 OK`:**
```json
{
  "id": "product-uuid-001",
  "name": "Big Mac",
  "stock": 120
}
```

---

### Actualizar el nombre de una sucursal

```bash
curl -X PUT http://localhost:8080/branches/branch-uuid-001 \
  -H "Content-Type: application/json" \
  -d '{"name": "Sucursal Norte"}'
```

**Respuesta `200 OK`:**
```json
{
  "id": "branch-uuid-001",
  "name": "Sucursal Norte",
  "products": []
}
```

---

### Consultar el producto con mayor stock por sucursal

```bash
curl http://localhost:8080/franchises/550e8400-e29b-41d4-a716-446655440000/top-stock
```

**Respuesta `200 OK`:**
```json
[
  {
    "branchName": "Sucursal Norte",
    "productName": "Big Mac",
    "stock": 120
  },
  {
    "branchName": "Sucursal Sur",
    "productName": "McFlurry",
    "stock": 80
  }
]
```

---

### Eliminar un producto

```bash
curl -X DELETE http://localhost:8080/products/product-uuid-001
```

**Respuesta `200 OK`:**
```json
{
  "id": "product-uuid-001",
  "name": "Big Mac",
  "stock": 120
}
```

---

## Manejo de errores

Todos los errores siguen el mismo formato de respuesta:

```json
{
  "message": "Descripción del error"
}
```

| Código | Causa                                                  |
|--------|--------------------------------------------------------|
| 400    | Validación fallida (campo requerido, stock negativo)   |
| 404    | Recurso no encontrado (franquicia, sucursal, producto) |

**Ejemplo — campo requerido vacío:**
```bash
curl -X POST http://localhost:8080/franchises \
  -H "Content-Type: application/json" \
  -d '{"name": ""}'
```
```json
{
  "message": "Name is required"
}
```

**Ejemplo — stock negativo:**
```bash
curl -X POST http://localhost:8080/branches/branch-001/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Producto", "stock": -5}'
```
```json
{
  "message": "Stock must be non-negative"
}
```

---

## Estructura del proyecto

```
franchise-api/
├── Dockerfile
├── docker-compose.yml
├── build.gradle.kts
│
└── src/
    └── main/
        ├── resources/
        │   ├── application.properties
        │   └── application-local.properties   # Perfil Docker / DynamoDB Local
        │
        └── java/com/franchise/
            ├── FranchiseApiApplication.java
            │
            ├── api/                            # Capa de presentación
            │   ├── controller/
            │   │   ├── FranchiseController.java
            │   │   ├── BranchController.java
            │   │   └── ProductController.java
            │   ├── dto/
            │   │   ├── request/
            │   │   └── response/
            │   ├── handler/
            │   │   └── ApiExceptionHandler.java
            │   └── mapper/
            │
            ├── application/                    # Capa de aplicación
            │   └── usecase/
            │       ├── franchise/
            │       ├── branch/
            │       ├── product/
            │       └── query/
            │
            ├── domain/                         # Capa de dominio (sin dependencias externas)
            │   ├── model/
            │   │   ├── Franchise.java
            │   │   ├── Branch.java
            │   │   └── Product.java
            │   └── port/in/
            │       └── FranchiseRepository.java
            │
            ├── infrastructure/                 # Capa de infraestructura
            │   └── persistence/
            │       └── FranchiseRepositoryImpl.java
            │
            └── config/
                └── BeanConfig.java
```
