# Comprobación en teléfono o emulador

Esta lista describe verificaciones que requieren ejecutar la app en Android. Las pruebas unitarias del motor y Android Lint no sustituyen estas comprobaciones visuales.

1. **Sketch y color.** Abre la app, elige Sketch y dibuja con tres colores. Cambia el grosor. Los trazos deben conservar su color y grosor originales y permanecer inmóviles.
2. **Opacidad y orden.** Selecciona Line art: el sketch se debe atenuar al 40 %. Dibuja encima con un color distinto. Vuelve a Sketch: recupera el 100 % y line art continúa por encima, incluso de trazos de sketch dibujados después.
3. **Animación al soltar.** Mantén un trazo de line art en curso: debe seguir el dedo. Levántalo: ese trazo debe empezar a moverse. Dibuja más líneas y puntos. Todos los trazos terminados de line art deben seguir animados, con una vibración suave, sin alejarse de su posición original.
4. **Goma.** Selecciona Goma de trazos y toca una línea larga por el centro. Debe desaparecer completa. En un cruce de líneas de la misma capa solo desaparece la superior. Tocar un espacio vacío no cambia nada. La otra capa no se borra.
5. **Deshacer.** Deshaz el borrado: debe reaparecer todo el trazo con su color, grosor, posición en la capa y animación. Deshaz un trazo nuevo después de cambiar de capa: debe deshacerse la última acción realizada.
6. **Selector de color.** Desplaza la paleta y pulsa el círculo multicolor. Prueba tono, saturación y luminosidad. Escribe `#35A87B` y aplica. Prueba un código incorrecto: debe mostrar un error y mantener abierto el selector.
7. **Dedo y lápiz.** Prueba líneas rápidas, curvas, puntos y el borde del papel. Un segundo dedo no debe unir trazos ni crear saltos. Al cancelar un gesto no debe guardarse un trazo parcial. Si el lápiz dispone de extremo de goma, debe borrar trazos completos.
8. **Rotación y ventanas.** Gira el dispositivo y vuelve a vertical. Comprueba que el dibujo conserva su proporción, que el historial de deshacer sigue disponible y que los controles se pueden utilizar. Repite con navegación por gestos, botones y pantalla con recorte.
9. **Guardado.** Termina varios trazos, sal de la app y vuelve. Fuerza el cierre después de que el guardado haya concluido y vuelve a abrirla: las dos capas y la animación deben recuperarse. Un trazo que aún estaba en curso al salir se descarta.
10. **Segundo plano.** Comprueba con el profiler que la vista deja de solicitar fotogramas al salir de la actividad. Al volver, las líneas continúan animándose.

Dispositivos recomendados: Android 8.0 (API 26), Android 15 (API 35), un teléfono pequeño y una tablet o emulador horizontal.
