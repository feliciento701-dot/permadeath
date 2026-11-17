PERMADEATH 2 - mod Fabric 1.21.1

COMPILAR
  Requiere JDK 21 y Gradle 8.8+ (o abrir la carpeta en IntelliJ como proyecto Gradle).
    gradle build
  El .jar queda en build/libs/ (usa el que NO termina en -sources).

INSTALAR
  El .jar va en la carpeta mods del SERVIDOR y de CADA CLIENTE (+ Fabric API).

COMANDOS (nivel 2 / op)
  /permadeath test               reproduce la animación sin morir
  /permadeath revive <jugador>   quita la marca de muerte y lo pasa a supervivencia

AJUSTES
  PermadeathManager.KICK_DELAY_TICKS   tiempo antes de expulsar (240 = 12 s)
  DeathAnimation.TIMELINE              frames y duración de la calavera
  assets/permadeath/textures/gui/skull/frame_0..6.png   imágenes de la calavera

OBTENER EL .JAR SIN INSTALAR NADA (GitHub Actions)
  1. Crea un repositorio en github.com (puede ser privado).
  2. Sube TODO el contenido de esta carpeta a la raíz del repositorio (incluida la carpeta .github).
  3. Pestaña "Actions" -> espera a que termine "Compilar mod" (2-4 min).
  4. Entra a esa ejecución -> sección "Artifacts" -> descarga "permadeath2-jar".
  5. Dentro del zip está permadeath2-2.0.0.jar (ignora el que dice -sources).
  Si falla, abre la ejecución, copia el error en rojo y pásamelo.
