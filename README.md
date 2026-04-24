# Franchise API

API REST reactiva para la gestión de franquicias, sucursales y productos. Construida con **Spring Boot WebFlux** siguiendo los principios de **Clean Architecture**, con persistencia en **AWS DynamoDB**.

---

## Tabla de contenidos

- [Descripción](#descripción)
- [Stack tecnológico](#stack-tecnológico)
- [Arquitectura](#arquitectura)
- [Modelos de dominio](#modelos-de-dominio)
- [Endpoints](#endpoints)
- [Requisitos previos](#requisitos-previos)
- [Opción 1 — Local con Docker (DynamoDB Local)](#opción-1--local-con-docker-dynamodb-local)
- [Opción 2 — AWS con Terraform + Docker](#opción-2--aws-con-terraform--docker)
- [Opción 3 — Local sin Docker (en memoria)](#opción-3--local-sin-docker-en-memoria)
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
| Infraestructura       | Terraform                           |
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

### Todas las opciones requieren

- **Git**

### Opción 1 — Local con Docker

- **Docker Desktop** — [Descargar](https://www.docker.com/products/docker-desktop/)

No se necesita Java ni Gradle instalados localmente; el build ocurre dentro del contenedor.

### Opción 2 — AWS con Terraform + Docker

- **Docker Desktop** — [Descargar](https://www.docker.com/products/docker-desktop/)
- **Terraform CLI ≥ 1.5** — [Descargar](https://developer.hashicorp.com/terraform/install)
- **Cuenta de AWS** con un usuario IAM que tenga el permiso `AmazonDynamoDBFullAccess`
- **Credenciales AWS** configuradas (mediante `aws configure` o variables de entorno)

### Opción 3 — Local sin Docker

- **Java 21** — [Descargar](https://adoptium.net/)

---

## Opción 1 — Local con Docker (DynamoDB Local)

La forma más sencilla de ejecutar el proyecto. Levanta tres contenedores:

| Contenedor       | Imagen                          | Puerto |
|------------------|---------------------------------|--------|
| `dynamodb-local` | `amazon/dynamodb-local:2.5.2`   | 8000   |
| `dynamodb-init`  | `amazon/aws-cli:2.15.30`        | —      |
| `franchise-api`  | Construida desde el `Dockerfile`| 8080   |

`dynamodb-init` crea la tabla `franchise` automáticamente al iniciar y luego se detiene.

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

---

## Opción 2 — AWS con Terraform + Docker

Usa esta opción para ejecutar la aplicación conectada a una tabla DynamoDB real en AWS. Terraform se encarga de aprovisionar la infraestructura necesaria.

### Paso 1 — Clonar el repositorio

```bash
git clone https://github.com/alexisportillodev/franchise-api.git
cd franchise-api
```

### Paso 2 — Configurar las credenciales de AWS

Asegúrate de tener las credenciales disponibles en el entorno. La forma más directa:

```bash
# Linux / macOS
export AWS_ACCESS_KEY_ID=tu-access-key
export AWS_SECRET_ACCESS_KEY=tu-secret-key

# Windows (PowerShell)
$env:AWS_ACCESS_KEY_ID="tu-access-key"
$env:AWS_SECRET_ACCESS_KEY="tu-secret-key"
```

O de forma permanente con el CLI de AWS:

```bash
aws configure
```

### Paso 3 — Aprovisionar la tabla DynamoDB con Terraform

```bash
cd terraform
terraform init                # descarga el provider de AWS
terraform plan                # previsualiza los recursos que se crearán
terraform apply               # escribe "yes" para confirmar
```

Al finalizar `apply`, Terraform imprime el nombre y ARN de la tabla:

```
table_name = "franchise"
table_arn  = "arn:aws:dynamodb:us-east-2:123456789012:table/franchise"
```

> **Región:** por defecto usa `us-east-2`. Para cambiarla, pasa `-var="aws_region=us-east-1"` al comando `terraform apply`.

> **¿Qué crea Terraform?** Una tabla DynamoDB con clave compuesta `PK` / `SK`, un índice secundario global `GSI_SK_PK` y modo de facturación `PAY_PER_REQUEST`. No crea usuarios IAM ni credenciales; usa las que ya configuraste en el paso anterior.

### Paso 4 — Configurar el archivo de entorno

Copia el archivo de ejemplo y completa tus credenciales:

```bash
cp .env.example .env
```

Edita `.env`:

```env
AWS_ACCESS_KEY_ID=tu-access-key
AWS_SECRET_ACCESS_KEY=tu-secret-key
```

> `.env` está en el `.gitignore`. Nunca lo subas al repositorio.

### Paso 5 — Construir y levantar el contenedor

```bash
docker compose -f docker-compose.aws.yml up --build
```

El contenedor lee `AWS_ACCESS_KEY_ID` y `AWS_SECRET_ACCESS_KEY` desde `.env` y se conecta directamente a DynamoDB en AWS (sin endpoint local).

Cuando veas esta línea, la API está lista:

```
franchise-api  | Started FranchiseApiApplication in X.XXX seconds
```

### Paso 6 — Probar la API

```bash
curl -X POST http://localhost:8080/franchises \
  -H "Content-Type: application/json" \
  -d '{"name": "Mi Franquicia"}'
```

### Detener el contenedor

```bash
docker compose -f docker-compose.aws.yml down
```

> Los datos persisten en DynamoDB en AWS aunque el contenedor se detenga.

### Destruir la infraestructura en AWS (cuando ya no se necesite)

```bash
cd terraform
terraform destroy             # escribe "yes" para confirmar
```

Esto elimina la tabla DynamoDB y todos los recursos creados por Terraform.

---

## Opción 3 — Local sin Docker (en memoria)

Usa esta opción para desarrollo rápido o ejecución de tests. No requiere DynamoDB; la persistencia es en memoria.

### Paso 1 — Clonar el repositorio

```bash
git clone https://github.com/alexisportillodev/franchise-api.git
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
├── docker-compose.yml            # Local — DynamoDB Local
├── docker-compose.aws.yml        # AWS — DynamoDB real
├── build.gradle.kts
│
├── terraform/                    # Infraestructura como código
│   ├── main.tf                   # Tabla DynamoDB + configuración del provider
│   ├── variables.tf              # aws_region, table_name
│   ├── outputs.tf                # table_name, table_arn
│   └── .terraform.lock.hcl      # Lock de versión del provider
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
            │   ├── dto/
            │   │   ├── request/
            │   │   └── response/
            │   ├── handler/
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
            │   └── port/in/
            │
            ├── infrastructure/                 # Capa de infraestructura
            │   └── persistence/
            │       └── dynamodb/
            │
            └── config/
```
