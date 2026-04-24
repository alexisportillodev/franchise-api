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
- [Ejecución local](#ejecución-local)
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

| Componente        | Tecnología                          |
|-------------------|-------------------------------------|
| Lenguaje          | Java 21                             |
| Framework         | Spring Boot 3.5.13                  |
| Modelo reactivo   | Spring WebFlux (Reactor / Netty)    |
| Build tool        | Gradle (Kotlin DSL)                 |
| Reducción boilerplate | Lombok                          |
| Validación        | Jakarta Bean Validation             |
| Testing           | JUnit 5, Mockito, Reactor Test      |
| Persistencia      | In-memory (`ConcurrentHashMap`)     |

> La capa de persistencia está diseñada para ser reemplazada por AWS DynamoDB sin modificar la lógica de negocio.

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

Antes de ejecutar el proyecto asegúrate de tener instalado:

- **Java 21** — [Descargar](https://adoptium.net/)
- **Git** — para clonar el repositorio

No es necesario instalar Gradle; el proyecto incluye el wrapper `gradlew`.

Verifica tu versión de Java:

```bash
java -version
# java version "21.x.x" ...
```

---

## Ejecución local

### 1. Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd franchise-api
```

### 2. Compilar el proyecto

```bash
# Linux / macOS
./gradlew build

# Windows
gradlew.bat build
```

### 3. Ejecutar la aplicación

```bash
# Linux / macOS
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

La aplicación arranca en `http://localhost:8080`.

### 4. Ejecutar los tests

```bash
# Linux / macOS
./gradlew test

# Windows
gradlew.bat test
```

### 5. Generar el JAR y ejecutarlo directamente

```bash
# Compilar y empaquetar
./gradlew bootJar

# Ejecutar el JAR generado
java -jar build/libs/franchise-api-0.0.1-SNAPSHOT.jar
```

### Perfil local (opcional)

El archivo `src/main/resources/application-local.properties` contiene configuración para conectar con una instancia local de DynamoDB (por ejemplo, LocalStack). Para activarlo:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

> Sin este perfil, la aplicación usa almacenamiento en memoria y no requiere ninguna dependencia externa.

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
  "products": [...]
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
src/
└── main/
    └── java/com/franchise/
        ├── FranchiseApiApplication.java
        │
        ├── api/                          # Capa de presentación
        │   ├── controller/
        │   │   ├── FranchiseController.java
        │   │   ├── BranchController.java
        │   │   └── ProductController.java
        │   ├── dto/
        │   │   ├── request/              # FranchiseRequest, BranchRequest, ProductRequest, ...
        │   │   └── response/             # FranchiseResponse, BranchResponse, ProductResponse, ...
        │   ├── handler/
        │   │   └── ApiExceptionHandler.java
        │   └── mapper/
        │       ├── FranchiseMapper.java
        │       ├── BranchMapper.java
        │       ├── ProductMapper.java
        │       └── TopProductMapper.java
        │
        ├── application/                  # Capa de aplicación
        │   └── usecase/
        │       ├── franchise/            # CreateFranchiseUseCase, UpdateFranchiseNameUseCase
        │       ├── branch/               # AddBranchToFranchiseUseCase, UpdateBranchNameUseCase, ...
        │       ├── product/              # AddProductToBranchUseCase, RemoveProductFromBranchUseCase, ...
        │       └── query/                # GetTopStockProductPerBranchUseCase
        │
        ├── domain/                       # Capa de dominio (sin dependencias externas)
        │   ├── model/
        │   │   ├── Franchise.java
        │   │   ├── Branch.java
        │   │   └── Product.java
        │   └── port/in/
        │       └── FranchiseRepository.java
        │
        ├── infrastructure/               # Capa de infraestructura
        │   └── persistence/
        │       └── FranchiseRepositoryImpl.java
        │
        └── config/
            └── BeanConfig.java           # Wiring manual de dependencias
```
