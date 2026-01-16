# Guía de Pruebas - Customer Service API

Esta guía contiene los comandos necesarios para probar manualmente los endpoints del microservicio `customer-service` utilizando PowerShell.

## Prerrequisitos
1. **Infraestructura Docker**: Asegúrate de que los contenedores (Postgres, Kafka) estén corriendo.
   ```powershell
   docker-compose up -d
   ```
2. **Customer Service**: El servicio debe estar corriendo en el puerto 8081.
   ```powershell
   java "-Dquarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/customer_db" "-Dquarkus.datasource.username=customer_user" "-Dquarkus.datasource.password=customer_pass" "-Dkafka.bootstrap.servers=localhost:9094" -jar customer-service/target/quarkus-app/quarkus-run.jar
   ```

## Variables Comunes
Ejecuta esto primero para configurar la URL base:
```powershell
$baseUrl = "http://localhost:8081/api/v1/customers"
```

---

## 1. Crear Cliente (POST)
Crea un nuevo cliente con estado `PENDING_VERIFICATION`.

```powershell
$body = @{
    firstName = "Maria"
    lastName = "Rodriguez"
    email = "maria.rodriguez@vaultstream.com"
    phoneNumber = "+584149998877"
    dateOfBirth = "1995-10-20"
    nationalId = "V-20202020"
    type = "INDIVIDUAL"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri $baseUrl -Method POST -Body $body -ContentType "application/json"
$id = $response.id
Write-Host "Cliente Creado con ID: $id"
$response | ConvertTo-Json -Depth 5
```

---

## 2. Consultar Cliente (GET)
Obtiene los detalles del cliente usando el ID guardado.

```powershell
Invoke-RestMethod -Uri "$baseUrl/$id" -Method GET | ConvertTo-Json -Depth 5
```

---

## 3. Actualizar Cliente (PUT)
Actualiza datos del cliente (ej. Dirección).

```powershell
$updateBody = @{
    customerId = $id
    firstName = "Maria Julia"
    address = @{
        street = "Av. Libertador"
        number = "45-A"
        city = "Caracas"
        state = "Distrito Capital"
        postalCode = "1050"
        country = "VE"
    }
} | ConvertTo-Json -Depth 5

Invoke-RestMethod -Uri "$baseUrl/$id" -Method PUT -Body $updateBody -ContentType "application/json" | ConvertTo-Json -Depth 5
```

---

## 4. Activar Cliente (POST)
Cambia el estado del cliente a `ACTIVE`. Esto dispara un evento a Kafka.

```powershell
# Nota: Se envía un JSON vacío {} porque el método requiere Content-Type application/json
Invoke-RestMethod -Uri "$baseUrl/$id/activate" -Method POST -Body "{}" -ContentType "application/json" | ConvertTo-Json -Depth 5
```

---

## 5. Suspender Cliente (POST)
Suspende al cliente (ej. por actividad sospechosa).

```powershell
Invoke-RestMethod -Uri "$baseUrl/$id/suspend?reason=VerificacionManual" -Method POST -Body "{}" -ContentType "application/json" | ConvertTo-Json -Depth 5
```

---

## 6. Verificación de Base de Datos
Puedes verificar directamente en PostgreSQL que los cambios se guardaron.

```powershell
docker exec vaultstream-postgres-customer psql -U customer_user -d customer_db -c "SELECT id, first_name, status, version FROM public.customers WHERE id='$id';"
```
