# Reglas del Proyecto (Ultra TV Android Native)

## Gestión de Operaciones de Red y Corrutinas

- **Evitar NetworkOnMainThreadException**: Cualquier operación de red (solicitudes HTTP síncronas con OkHttp, consultas a APIs externas de CDN, descargas de catálogos) realizada desde componentes de la UI o ViewModels debe delegarse explícitamente a un hilo de fondo.
- **Uso de Dispatchers.IO**: Las funciones encargadas de estas tareas (como la firma de URLs de Flow o recuperadores de tokens) deben definirse como suspendidas (`suspend fun`) y encapsular su lógica dentro de `withContext(Dispatchers.IO)`.
