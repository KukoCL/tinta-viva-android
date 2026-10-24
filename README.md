# Tinta Viva

Una pequeña app nativa de Android para dibujar con líneas que parecen cobrar vida. Interfaz en español, un pincel redondo, dos capas y un efecto de animación inspirado en las caricaturas dibujadas a mano.

## Qué hace

- **Sketch:** capa inferior, con colores libres.
- **Line art:** capa superior. Cada trazo comienza a moverse en cuanto levantas el dedo o el lápiz.
- **Grosor vivo:** distintas zonas del mismo trazo se afinan y ensanchan de forma marcada e irregular mientras se mueve. La variación local alcanza como máximo un 55 % del grosor elegido. Las zonas cambian independientemente en cada fotograma, con transiciones suaves entre puntos vecinos.
- **Fuerza del movimiento:** el botón de onda junto a las capas abre un panel pequeño con Movimiento suave (el nivel original) y Movimiento intenso (el más exagerado). Se aplica también a las líneas ya dibujadas y recuerda la elección al girar o volver a abrir la app. Solo cambia el desplazamiento: la variación irregular de grosor es exactamente la misma en ambos niveles. El nivel inicial es Intenso.
- Al seleccionar **Line art**, el sketch se muestra al **30 % de opacidad**: 0 % sería invisible y 100 % completamente opaco. Al volver a Sketch recupera el 100 %.
- **Solo line art:** tercer botón que oculta el sketch y permite seguir dibujando en la capa superior. El sketch se conserva y vuelve a mostrarse al seleccionar las otras vistas.
- Barra compacta con iconos de pincel, goma y color. El icono de paleta abre los colores predefinidos y el selector de cualquier color mediante tono, saturación, luminosidad o código hexadecimal.
- Un solo tipo de pincel, con grosor ajustable.
- **Goma de trazos:** toca y suelta un trazo para borrarlo entero. Solo afecta a la capa seleccionada y, si varios trazos se cruzan, borra el último dibujado en esa capa.
- **Zoom de 1× a 8×:** pellizca con dos dedos para ampliar o reducir. Mueve ambos dedos juntos para desplazar el lienzo ampliado. Pellizca hacia dentro hasta 1× para volver a ver la página completa.
- Los gestos de dos dedos no dibujan ni borran. Si colocas el segundo dedo mientras un trazo está en curso, ese borrador de trazo se descarta y empieza la navegación. Al levantar los dedos, puedes iniciar un trazo nuevo normalmente.
- **Deshacer y Rehacer:** botones contiguos para revertir y recuperar trazos nuevos o borrados, incluso después de cambiar de capa. Conservan hasta 128 acciones durante la sesión. Dibujar o borrar un trazo después de deshacer inicia una nueva secuencia y descarta las acciones pendientes de rehacer; cambiar de capa o movimiento las conserva.
- **Menú de guardar:** el icono de tres puntos verticales de la cabecera abre Guardar GIF / compartir, Importar imagen de sketch, Quitar imagen de sketch y Nuevo lienzo.
- **Imagen de sketch:** selecciona un archivo de imagen. La app lo centra sin deformarlo debajo de los trazos del sketch, de modo que puedas dibujar encima. La imagen y los trazos se atenúan juntos al 30 % en Line art y se ocultan juntos en Solo line art. Quitar imagen conserva todos los trazos. Se guarda una copia privada de la imagen para recuperarla al volver a abrir la app.
- **Nuevo lienzo:** está en el menú de guardar y pide confirmación antes de borrar la imagen, las dos capas y ambos historiales. Vuelve a Sketch, selecciona el pincel y muestra la página completa; conserva el color, el grosor y el nivel de movimiento elegidos. El lienzo vacío se guarda automáticamente.
- Guardado automático de un dibujo en el dispositivo. Conserva las dos capas, los colores y la animación al volver a abrir la app.
- Distribución adaptada a orientación vertical, horizontal y pantallas grandes.
- En vertical, teléfonos y tablets usan todo el ancho para el lienzo, con las capas arriba y las herramientas abajo. El panel lateral se utiliza en horizontal a partir de 600 dp de ancho. Los botones de herramientas conservan su tamaño compacto de 48 dp.

La versión **1.1.0** amplía el espacio disponible para el lienzo al retirar la línea de ayuda, compactar la cabecera y reunir las herramientas en una sola fila. El zoom y el desplazamiento no modifican el tamaño de los trazos guardados y se conservan durante la rotación de pantalla en la misma sesión.

La versión **1.3.0** añade imágenes de referencia, exportación GIF y el menú de compartir de Android. El GIF contiene ocho fotogramas a 540 × 675, con un bucle de un segundo, fondo blanco, la fuerza de movimiento elegida y las capas visibles en el momento de exportar. Exporta la página completa, independientemente del zoom, sin controles ni mensajes del editor. Al terminar abre el menú de compartir para elegir la aplicación de destino.

La versión **1.2.0** añade variación animada de grosor a cada zona del line art, incluidos los trazos ya guardados. El sketch conserva su grosor original. La goma tiene en cuenta el grosor local del fotograma visible.

No requiere cuenta, conexión a Internet ni permisos generales de almacenamiento. Las imágenes se eligen con el selector de archivos de Android y se copian al espacio privado de la app. Trabaja con un solo lienzo y permite exportarlo como GIF animado; no incluye galería de dibujos ni exportación de vídeo. Los historiales de deshacer y rehacer se conservan al girar la pantalla y se reinician cuando Android termina el proceso.

## Compilar en Windows con Android Studio

1. Instala **Android Studio Ladybug (2024.2.1) o posterior** desde [developer.android.com/studio](https://developer.android.com/studio).
2. Descarga este repositorio como ZIP y extráelo, o clónalo con Git. Abre la carpeta que contiene `settings.gradle` desde **Open** en Android Studio.
3. En **SDK Manager**, instala **Android SDK Platform 35** y **Android SDK Build-Tools 34.0.0**. Acepta las licencias cuando se solicite.
4. En **Settings → Build, Execution, Deployment → Build Tools → Gradle**, selecciona un **Gradle JDK 17 o 21**. El JDK 21 incluido en Android Studio es compatible.
5. Espera a que termine la sincronización de Gradle. La primera vez necesita Internet para descargar las herramientas y dependencias.
6. Selecciona **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. En algunas versiones aparece como **Build → Generate App Bundles or APKs → Generate APKs**.

El APK instalable de prueba se genera en:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Puedes copiarlo a tu teléfono e instalarlo autorizando la instalación desde esa aplicación, o conectar el teléfono con depuración USB y pulsar **Run** en Android Studio. Requiere **Android 8.0 o posterior**.

### Compilar desde PowerShell

Con Android Studio y el SDK instalados, abre PowerShell en la carpeta del proyecto. Si Java no está en el PATH, puedes utilizar el JDK que trae Android Studio:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

También puedes ejecutar `compilar-windows.bat`, que realiza esas mismas comprobaciones y genera el APK. No necesitas instalar Gradle: se incluye su wrapper oficial.

Si el SDK está en otra carpeta, configura `ANDROID_HOME` con su ubicación, o deja que Android Studio cree `local.properties`. Ese archivo contiene rutas de tu equipo y no se sube a Git.

El APK de `debug` está firmado para desarrollo. Las actualizaciones de distribución deben usar siempre la firma permanente descrita en [Firma y actualizaciones](docs/FIRMA-Y-ACTUALIZACIONES.md). Usa la misma clave en Android Studio o al compilar `release`; crear una nueva impediría actualizar las instalaciones anteriores. Conserva la clave fuera del repositorio.

## APK desde GitHub

El workflow **Compilar APK** ejecuta las pruebas y Android Lint en cada push. También se puede iniciar desde **Actions → Compilar APK → Run workflow**. Los APK descargables se compilan como `release` con una firma privada permanente y versiones crecientes, para permitir actualizaciones sin desinstalar. La firma se valida antes y después de compilar. Si faltan sus secretos, el workflow comprueba el código y el empaquetado, pero no publica APK. Consulta [la configuración inicial y la migración de las instalaciones anteriores](docs/FIRMA-Y-ACTUALIZACIONES.md). Cuando esté configurada, descarga **tinta-viva-apk** y extrae su APK.

## Cómo se anima

El documento guarda los trazos como puntos vectoriales en un lienzo de 1080 × 1350. La pantalla ajusta su escala sin modificar el dibujo. Al terminar un trazo de line art se muestran ocho variantes que se repiten a ocho fotogramas por segundo. Cada variante deforma los puntos a lo largo del trazo, con una semilla distinta para cada línea y un desplazamiento máximo de 3,2 unidades por eje en Suave y 8 en Intenso. También calcula un grosor distinto para cada punto mediante variaciones independientes por región y fotograma. Combina regiones de 96 y 40 unidades con interpolación suave para que las zonas anchas y finas cambien de forma irregular, sin el efecto de una onda que avanza por la línea. El grosor local se mantiene entre el 45 % y el 155 % del valor del pincel en ambos niveles de movimiento.

Cada fotograma se dibuja como un contorno relleno con extremos y uniones redondeados. Los originales nunca cambian: la animación no acumula desplazamiento ni cambios de grosor y el sketch permanece inmóvil. La semilla del trazo permite recuperar el mismo efecto al abrir un dibujo guardado.

Las geometrías se reutilizan entre fotogramas y se vuelven a calcular al cambiar de fuerza. La animación se detiene cuando la actividad pasa a segundo plano. La goma comprueba la geometría del fotograma visible con el nivel de movimiento seleccionado para poder tocar las líneas animadas, también después de ampliar y desplazar el lienzo.

## Estructura y comprobaciones

```text
app/src/main/java/com/kukocl/tintaviva/
  core/                 Documento, trazos, animación, goma, historial y zoom
  DrawingView.java      Lienzo y entrada táctil/lápiz
  DrawingRenderer.java  Renderizado común del editor y el GIF
  GifExporter.java      Ocho fotogramas del documento capturado
  SketchImages.java     Importación limitada y copia privada de imágenes
  AnimatedStrokePath.java  Contorno del pincel con grosor variable
  MainActivity.java     Interfaz del estudio
  ColorPickerDialog.java
  DocumentStore.java    Guardado JSON atómico en segundo plano
app/src/test/           Pruebas del motor y gestos/interfaz con Robolectric
docs/PRUEBAS-MANUALES.md
.github/workflows/android.yml
```

Tecnología: Java 17, Android SDK 35, Android Gradle Plugin 8.7.3 y Gradle 8.9. La exportación utiliza gifencoder 0.10.1 de Square (Apache 2.0), compatible con Android, y AndroidX Core 1.15.0 para compartir el GIF mediante FileProvider con acceso temporal de lectura. JUnit y Robolectric se utilizan únicamente para las pruebas y no se incluyen en el APK. La licencia de gifencoder se incluye en los assets de la app.

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Las pruebas verifican opacidad, vista de solo line art, separación de capas, colores, borrado completo, orden de superposición al deshacer, restauración de documentos, puntos aislados, muestreo de entrada y animación determinista sin deriva. También comprueban el foco y los límites del zoom, las coordenadas al dibujar y borrar después de ampliar, la cancelación de trazos al pellizcar y que la goma no borre durante esos gestos. Las pruebas del grosor animado comprueban su variación local y temporal, sus límites, las transiciones suaves entre puntos y el borrado de zonas finas y gruesas. Una regresión compara los perfiles de grosor entre fotogramas, incluso desplazándolos, para detectar si reaparece el efecto de onda que avanza. Las pruebas gráficas de Skia verifican los píxeles del contorno, las uniones redondeadas y los giros cerrados. La guía de pruebas manuales cubre los gestos y la apariencia en un dispositivo.
