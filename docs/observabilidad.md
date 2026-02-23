# Observabilidad - Prometheus y Grafana

Esta guía explica cómo configurar y usar Prometheus y Grafana para monitorear DOMINation.

## Arquitectura de Observabilidad

```
Servicios Spring Boot
    ↓ (expone métricas)
/actuator/prometheus
    ↓ (scraping cada 10s)
Prometheus
    ↓ (datasource)
Grafana
    ↓ (dashboards)
Visualización
```

## Prometheus

### Configuración

**Archivo**: `infra/prometheus/prometheus.yml`

```yaml
global:
  scrape_interval: 10s

scrape_configs:
  - job_name: gateway
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['domination-gateway:8080']

  - job_name: catalog
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['domination-catalog-service:8081']

  - job_name: booking
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['domination-booking-service:8082']

  - job_name: auth
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['domination-auth-service:9000']
```

### Acceso

- **URL**: http://localhost:9090
- **Targets**: http://localhost:9090/targets
- **Graph**: http://localhost:9090/graph

### Verificar Targets

1. Acceder a http://localhost:9090/targets
2. Verificar que todos los targets estén `UP` (verde)
3. Si algún target está `DOWN`, verificar:
   - Que el servicio esté corriendo
   - Que el endpoint `/actuator/prometheus` esté accesible
   - Que la red Docker esté configurada correctamente

### Queries Útiles

#### Verificar que todos los servicios estén arriba

```promql
up
```

#### Tasa de peticiones HTTP por segundo

```promql
rate(http_server_requests_seconds_count[5m])
```

#### Latencia promedio (p50, p95, p99)

```promql
# p50 (mediana)
histogram_quantile(0.50, rate(http_server_requests_seconds_bucket[5m]))

# p95
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# p99
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))
```

#### Errores 5xx por segundo

```promql
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```

#### Uso de memoria JVM

```promql
jvm_memory_used_bytes{area="heap"}
```

#### Uso de CPU

```promql
process_cpu_usage
```

#### Peticiones por servicio

```promql
sum(rate(http_server_requests_seconds_count[5m])) by (application)
```

## Grafana

### Configuración Inicial

1. **Acceder**: http://localhost:3000
2. **Login**: `admin` / `admin` (cambiar en producción)
3. **Agregar Datasource**:
   - Click en "Add your first data source"
   - Seleccionar "Prometheus"
   - URL: `http://prometheus:9090` (nombre interno de Docker)
   - Click "Save & Test"

### Dashboards Recomendados

#### 1. JVM (Micrometer)

**ID**: 4701

1. Click en "Dashboards" → "Import"
2. Ingresar ID: `4701`
3. Seleccionar datasource: Prometheus
4. Click "Import"

Este dashboard muestra:
- Uso de memoria heap/non-heap
- Threads
- Garbage collection
- Class loading

#### 2. Spring Boot 2.1 Statistics

**ID**: 11378

Similar al anterior, con métricas específicas de Spring Boot.

#### 3. Crear Dashboard Personalizado

**Panel 1: Requests por segundo**

```promql
sum(rate(http_server_requests_seconds_count[1m])) by (application)
```

**Visualización**: Time series
**Título**: Requests per Second

**Panel 2: Latencia p95**

```promql
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```

**Visualización**: Time series
**Título**: Latency p95

**Panel 3: Errores 5xx**

```promql
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (application)
```

**Visualización**: Time series
**Título**: 5xx Errors

**Panel 4: Uso de memoria**

```promql
jvm_memory_used_bytes{area="heap"} / 1024 / 1024
```

**Visualización**: Time series
**Título**: Heap Memory (MB)

## Actuator Endpoints

Todos los servicios exponen endpoints de Actuator:

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

Respuesta:
```json
{
  "status": "UP"
}
```

### Prometheus Metrics

```bash
curl http://localhost:8080/actuator/prometheus
```

Retorna métricas en formato Prometheus.

### Otros Endpoints

- `/actuator/info` - Información de la aplicación
- `/actuator/metrics` - Lista de métricas disponibles
- `/actuator/metrics/{metricName}` - Valor de una métrica específica

## Métricas Clave

### HTTP Server Requests

- `http_server_requests_seconds_count` - Contador de peticiones
- `http_server_requests_seconds_sum` - Suma de tiempos
- `http_server_requests_seconds_max` - Tiempo máximo
- `http_server_requests_seconds_bucket` - Histograma de latencias

**Labels**:
- `method` - GET, POST, etc.
- `status` - 200, 404, 500, etc.
- `uri` - Ruta de la petición
- `application` - Nombre del servicio

### JVM Metrics

- `jvm_memory_used_bytes` - Memoria usada
- `jvm_memory_max_bytes` - Memoria máxima
- `jvm_gc_pause_seconds` - Pausas de GC
- `jvm_threads_live` - Threads activos

### Process Metrics

- `process_cpu_usage` - Uso de CPU
- `process_uptime_seconds` - Tiempo de ejecución

## Alertas (Opcional)

### Configurar Alertmanager

1. Agregar servicio en `docker-compose.yml`:

```yaml
alertmanager:
  image: prom/alertmanager:latest
  ports:
    - "9093:9093"
  volumes:
    - ./alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml
  networks:
    - domination-network
```

2. Configurar Prometheus para usar Alertmanager:

```yaml
# prometheus.yml
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']

rule_files:
  - alerts.yml
```

3. Crear reglas de alerta (`alerts.yml`):

```yaml
groups:
  - name: domination_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
        for: 5m
        annotations:
          summary: "High error rate detected"
      
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
        for: 5m
        annotations:
          summary: "High memory usage"
```

## Troubleshooting

### Prometheus no puede scrapear servicios

1. Verificar que los servicios estén corriendo:
   ```bash
   docker-compose ps
   ```

2. Verificar conectividad desde Prometheus:
   ```bash
   docker-compose exec prometheus wget -O- http://gateway:8080/actuator/prometheus
   ```

3. Verificar configuración de red:
   ```bash
   docker network inspect domination-network
   ```

### Grafana no puede conectar a Prometheus

1. Verificar URL del datasource: debe ser `http://prometheus:9090` (nombre interno)
2. Verificar que Prometheus esté corriendo:
   ```bash
   docker-compose ps prometheus
   ```

### Métricas no aparecen

1. Verificar que los servicios expongan `/actuator/prometheus`:
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

2. Verificar configuración en `application.properties`:
   ```properties
   management.endpoints.web.exposure.include=health,info,metrics,prometheus
   management.endpoint.prometheus.enabled=true
   ```

### Performance de Prometheus

Si Prometheus consume mucha memoria:

1. Reducir `scrape_interval` (mínimo 15s recomendado)
2. Reducir retención de datos (por defecto 15 días)
3. Usar remote write para almacenamiento externo

## Mejores Prácticas

1. **Scrape interval**: 10-15 segundos es suficiente para la mayoría de casos
2. **Retención**: 15-30 días para desarrollo, más para producción
3. **Dashboards**: Crear dashboards específicos por servicio
4. **Alertas**: Configurar alertas para métricas críticas
5. **Labels**: Usar labels consistentes para facilitar queries

---

## Changelog

### 2026-02-23

- Documentación revisada para alinear con el resto del repo.

## Referencias

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer](https://micrometer.io/)
