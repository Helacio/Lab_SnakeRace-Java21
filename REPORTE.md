# Reporte de laboratorio — ARSW Lab #2

**Estudiante:** Juan David Rangel
**Tema:** concurrencia en Java 21 — condiciones de carrera, sincronización y colecciones seguras.

---

## Parte I — PrimeFinder: pausar/reanudar hilos con `wait/notify`

### Qué se modificó

`PrimeFinderThread` y `Control` (programa original de la ejercitación `wait-notify-excercise`):

- Se subió `pom.xml` de `source/target 1.7` a `21` (el `< 8` ya no compila con JDK 21+).
- Los hilos trabajadores (`PrimeFinderThread`) reciben ahora una referencia al `Control` compartido.
- Cada **5 segundos** (`TMILISECONDS`), el hilo coordinador (`Control`) **pausa** a todos los trabajadores, imprime cuántos primos se han encontrado hasta ese momento y queda esperando **ENTER** para reanudarlos. El ciclo se repite hasta que los hilos terminan.

### Diseño de sincronización

**Qué lock (monitor):** el objeto `Control` compartido por los 3 hilos. Se usa un único monitor porque la condición "el juego está pausado" es **global**: todos los trabajadores deben esperar y despertarse juntos, y `notifyAll()` sobre el mismo objeto los despierta a todos.

**Qué condición:** el estado `pause` (expuesto como `getPause()`). Un trabajador, cada 1000 números procesados, verifica la condición dentro de un bloque `synchronized` sobre el monitor común y, si está pausado, se suspende de forma **pasiva**:

```java
synchronized (control) {
    while (control.getPause()) {   // condición re-evaluada en loop
        control.wait();
    }
}
```

Se usa `while` y no `if` porque, tras despertar, la condición debe **re-verificarse** (despertares espurios o cambios de estado entre la notificación y la re-adquisición del monitor). El cambiador de estado es `Control`: al pausar pone `pause = true`, y al recibir ENTER pone `pause = false` y ejecuta `notifyAll()`, siempre dentro del mismo monitor:

```java
private void continues() {
    synchronized (this) {
        this.pause = false;
        this.notifyAll();
    }
}
```

### Cómo se evita el *lost wakeup*

La señal no es un "pulso" único, sino un **estado persistente** (el flag `pause`). La información de si el hilo debe esperar o no viaja en el estado, no solo en la notificación: si un trabajador todavía no llegó al `wait()` cuando ocurre el `notifyAll()`, al llegar comprobará `getPause() == false` y **no esperará**. Así no existe la ventana en la que el `notify` ocurre antes del `wait` y el hilo queda colgado para siempre.

Además, comprobar la condición y suspender ocurren **dentro del mismo bloque `synchronized`**: no hay instante en el que el estado pueda cambiar entre la verificación y el `wait()` sin que el monitor lo serialice.

### Sin *busy-waiting*

- Los trabajadores se bloquean en `wait()` (espera pasiva) y solo se despiertan cuando cambia la condición.
- El coordinador usa `Thread.sleep(TMILISECONDS)` para esperar cada intervalo.
- No hay ningún bucle de espera activa; el único costo es el `synchronized` puntual cada 1000 números, que se elige así para no pagar el lock en cada iteración.

### Verificación (observada en ejecución)

Con `TMILISECONDS = 5000`, al ejecutar se observaron salidas del tipo:

```
Hay 17969 primos
Presiona enter...
(se presiona ENTER y los hilos reanudan)
Hay 36601 primos
Presiona enter...
```

El programa completa el ciclo pausa → muestra conteo → espera ENTER → reanuda, sin excepciones ni bloqueos mutuos.

---

<!-- Parte II: análisis de concurrencia, correcciones, UI y robustez → se completa al trabajar SnakeRace -->
