# Actualizaciones de Tinta Viva

Android exige el mismo `applicationId`, el mismo certificado de firma y un `versionCode` igual o superior para actualizar una aplicación instalada. Los APK anteriores se compilaban como `debug` en máquinas temporales de GitHub Actions. Cada máquina generaba una clave nueva: el nombre del paquete era el mismo, pero la firma cambiaba. Por eso Android rechazaba la actualización y permitía instalarla después de desinstalar.

## Firma permanente

La distribución usa ahora una clave privada PKCS12, alias `tinta-viva`, guardada en dos secretos de GitHub Actions. Solo se conserva en el directorio temporal del runner durante la compilación y se retira al terminar. La clave y su contraseña no deben subirse al repositorio ni a artefactos públicos.

El certificado público esperado está fijado en `signing/certificate.sha256`. El workflow comprueba tanto la clave recuperada como el APK terminado contra esa huella. Si faltan ambos secretos, ejecuta las pruebas y verifica el empaquetado con una clave temporal exclusiva para CI, pero no publica ningún APK descargable. Si los secretos están incompletos, la contraseña es incorrecta o la huella cambia, detiene la distribución.

## Configuración inicial: una vez

1. Abre [los secretos de Actions de este repositorio](https://github.com/KukoCL/tinta-viva-android/settings/secrets/actions).
2. Crea el secreto `TINTA_VIVA_KEYSTORE_BASE64` con el contenido completo del archivo privado del mismo nombre terminado en `.txt`.
3. Crea el secreto `TINTA_VIVA_KEYSTORE_PASSWORD` con el contenido del archivo privado del mismo nombre terminado en `.txt`.
4. Vuelve a ejecutar el workflow **Compilar APK** de la rama del PR o haz una nueva compilación. El artefacto **tinta-viva-apk** contendrá el APK de distribución, su firma pública y su número de versión.

La copia privada entregada en esta conversación contiene esos dos archivos, `tinta-viva.p12` y la huella del certificado. Conserva esa copia fuera del repositorio. Cambiar o perder la clave volvería a impedir actualizar las instalaciones existentes. La conexión de esta sesión permite cambiar código, pero GitHub respondió `403 Resource not accessible by integration` al acceder a los secretos; por eso estos dos valores deben configurarse desde tu cuenta.

Los números de versión de distribución se asignan como `1000 + GITHUB_RUN_NUMBER`, de forma creciente para cada nueva ejecución del workflow. Los nombres del APK y `version.txt` indican ese número. Para actualizar, utiliza una compilación posterior; volver a descargar una ejecución antigua no crea una versión nueva. Un reintento de la misma ejecución conserva su número.

## La instalación anterior

La firma de un APK no permite recuperar la clave privada que lo firmó. Las claves de las máquinas temporales anteriores no se guardaron. Sin una copia de la clave original no se puede convertir esa instalación en una instalación con la firma permanente: hará falta una última desinstalación e instalación del APK con firma estable. Desinstalar borra el dibujo y la imagen guardados en el espacio privado de la app; conserva lo que necesites antes de hacerlo.

El APK con firma estable entregado en esta conversación contiene las funciones de la versión 1.3.0 ya verificadas. Las compilaciones de distribución posteriores usarán el mismo certificado y podrán actualizarlo, conservando los datos de la aplicación. Los APK `debug` de Android Studio usan otra firma y se reservan para desarrollo; no se publican como actualizaciones.

## Compilación local de distribución

En PowerShell, selecciona la copia privada de la clave y configura los valores solo para la sesión de compilación:

```powershell
$env:TINTA_VIVA_KEYSTORE_PATH = 'C:\ruta-privada\tinta-viva.p12'
$env:TINTA_VIVA_KEYSTORE_PASSWORD = Get-Content 'C:\ruta-privada\TINTA_VIVA_KEYSTORE_PASSWORD.txt' -Raw
$env:TINTA_VIVA_VERSION_CODE = Read-Host 'versionCode del último APK de Actions (archivo version.txt)'
.\gradlew.bat testDebugUnitTest lintDebug lintRelease assembleRelease
```

El APK se genera en `app\build\outputs\apk\release\app-release.apk`. Para una compilación local usa el mismo número del último APK de Actions: Android admite actualizar con el mismo número y la próxima ejecución del workflow seguirá avanzando. No adelantes números arbitrarios que puedan superar las próximas compilaciones automáticas. Usa siempre esta misma clave para distribuir actualizaciones. No es necesario configurar la firma permanente para ejecutar las pruebas o compilar `debug`.
