# Bitácora de Avances - 18 de Julio de 2026

Hoy se realizaron mejoras significativas en la generación de grillas EPG (Guía de Programación) en formato XMLTV, tanto para TeleRed como para la incorporación de Antina, optimizando su integración con la aplicación nativa de Android (Ultra TV).

---

## 1. Mejoras en la Grilla de TeleRed (`grilla_telered.py`)

*   **Navegación e Interacción Dinámica**:
    *   La grilla web original solo expone bloques de 6 horas por defecto. Se modificó el scraper para simular clics en todos los turnos del día (`trasnocho`, `mañana`, `tarde`, `noche`) y clics adicionales en los botones de avance temporal (`siguiente`), obteniendo así el DOM con las 24 horas de la grilla.
    *   Se mejoró la consolidación y desduplicación de datos provenientes de múltiples cargas de DOMs.
*   **Corrección de Codificación de Consola**:
    *   Se añadió soporte para reconfigurar la salida estándar a `UTF-8` en entornos Windows, evitando fallos de tipo `UnicodeEncodeError` provocados por la impresión de emojis.

---

## 2. Nuevo Scraper de EPG de Antina (`grilla_antina.py`)

*   **Descubrimiento e Integración de API**:
    *   Se descubrió que la grilla de Antina se incrusta mediante un iframe de **Reportv** (`https://www.reportv.com.ar/finder/index/2313`).
    *   Diseñamos un scraper directo que consume las APIs `POST` internas de Reportv: `/finder/grid` y `/finder/gridGetNext` mediante la librería nativa de Python `requests` (sin necesidad de Playwright/navegador), lo cual es extremadamente rápido y eficiente.
*   **Paginación e Infinite Scroll Seguros**:
    *   Implementamos paginación automática de canales (de 5 en 5). La condición de parada detecta la presencia del contenedor `programacion-fila` en las respuestas de Reportv, evitando bucles infinitos con plantillas vacías.
*   **Corrección de Codificación de Caracteres (UTF-8)**:
    *   Configuramos explícitamente `r.encoding = 'utf-8'` en las respuestas HTTP para evitar textos rotos (como `TelevisiÃ³n` en vez de `Televisión`), logrando que la aplicación de Android empareje los canales de forma perfecta.
*   **Cobertura Extendida de 2 Días (48 horas)**:
    *   El script ahora recorre tanto el día actual como el siguiente (offset 0 y 1) en 4 bloques horarios diarios (`00:00`, `06:00`, `12:00`, `18:00`), acumulando 3587 programas continuos y sin saltos de fecha.

---

## 3. Separación de Workflows en GitHub Actions

Separamos las tareas de actualización en dos flujos de trabajo independientes dentro de `.github/workflows/`:
1.  **Actualizar Grilla TeleRed Diaria** (`cron.yml`): Ejecuta el scraper de TeleRed con soporte para Playwright y dependencias de Ubuntu.
2.  **Actualizar Grilla Antina Diaria** (`cron_antina.yml`): Ejecuta el nuevo scraper ligero de Antina de forma ultra rápida (15-20 segundos).
*   Se habilitó `keep_files: true` en el paso de publicación de GitHub Pages de ambos flujos para asegurar que ambos archivos XMLTV convivan en línea en `https://sico33.github.io/telered-xmltv/` sin pisarse entre sí.

---

## 4. Adaptación de la APK de Android (Ultra TV)

*   **Enlace de EPG**:
    *   Modificamos temporalmente la URL hardcodeada en `XmltvParser.kt` apuntando directamente a la nueva grilla generada de Antina:
        ```kotlin
        val url = if (p.kind == "PREMIUM_TV" || p.baseUrl.isBlank()) {
            "https://sico33.github.io/telered-xmltv/grilla_antina.xml"
        }
        ```
*   **Compilación**:
    *   Se compiló y exportó con éxito el archivo APK firmado de producción en modo Release (`app-release.apk`) confirmando que la aplicación empareja los 104 canales y carga la programación completa correctamente.
