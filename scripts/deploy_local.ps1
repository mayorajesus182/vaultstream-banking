Write-Host "Building VaultStream Banking System..." -ForegroundColor Green

Write-Host "1. Building VaultStream Common..." -ForegroundColor Cyan
mvn clean install -pl vaultstream-common -DskipTests
if ($LASTEXITCODE -ne 0) { 
    Write-Error "Build failed for vaultstream-common"
    exit $LASTEXITCODE 
}

Write-Host "2. Building Customer Service..." -ForegroundColor Cyan
mvn clean package -pl customer-service -DskipTests
if ($LASTEXITCODE -ne 0) { 
    Write-Error "Build failed for customer-service"
    exit $LASTEXITCODE 
}

Write-Host "3. Starting Infrastructure and Services..." -ForegroundColor Cyan
docker-compose down -v
docker-compose up -d --build

Write-Host "Deployment Complete!" -ForegroundColor Green
Write-Host "URLs:"
Write-Host "  Customer Service: http://localhost:8080/q/swagger-ui"
Write-Host "  Kafka UI:         http://localhost:8090"
Write-Host "  Grafana:          http://localhost:3000"
Write-Host "  Jaeger:           http://localhost:16686"
Write-Host "  Prometheus:       http://localhost:9090"
