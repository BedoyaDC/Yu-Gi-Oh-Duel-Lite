# Yu-Gi-Oh! Duel Lite

Laboratorio #1 — Desarrollo de Software III, Tecnología en Sistemas, Universidad del Valle (Sede Tuluá).

Mini-aplicación de escritorio en **Java Swing** que simula un duelo sencillo de Yu-Gi-Oh! entre un jugador y la máquina,
con cartas Monster obtenidas en vivo desde la API **YGOProDeck** (`randomcard.php`).

## Requisitos
- Java 11 o superior
- Maven 3.6+ (descarga automáticamente `org.json`, la única dependencia)
- Conexión a internet

## Ejecución
Abrir la carpeta como proyecto Maven en **IntelliJ IDEA** y ejecutar la clase `ygo.Main`.

> La ficha de cada carta está diseñada con el **GUI Designer de IntelliJ** (`CardPanel.form`), por lo que el proyecto
> debe compilarse con IntelliJ (*Settings > Build Tools > Maven > Runner* / *Build and run using: IntelliJ IDEA*),
> que es quien procesa los `.form`. Con `mvn exec:java` por consola el formulario no se inicializa.

## Cómo se juega
1. Al abrir la app se piden **3 cartas Monster para el jugador y 3 para la máquina** (se ve el progreso en la barra de estado).
   Si algo falla (red, API) se muestra el error en rojo y se puede reintentar con **Nuevas cartas**.
2. Cuando las 6 cartas están cargadas se habilita **Iniciar duelo** (antes no se puede iniciar).
3. El turno inicial se define al azar. En cada ronda: haz clic en una de tus cartas, elige **Ataque** o **Defensa** y pulsa
   **Elegir carta**. La máquina responde con una carta y una posición al azar.
4. Quien gane **2 de 3 rondas** gana el duelo (anuncio en el log, barra de estado y ventana emergente).
   Después puedes pedir la revancha con las mismas cartas o repartir **Nuevas cartas**.

## Reglas implementadas
| Situación | Resultado |
|---|---|
| Ambas en **Ataque** | Gana el mayor ATK |
| Una en Ataque y otra en **Defensa** | Se compara el ATK del atacante con la DEF del defensor: gana el atacante solo si ATK > DEF; si es igual o menor, resiste el defensor |
| Ambas en **Defensa** (no definido en el enunciado) | Gana la mayor DEF |
| **Empate** de valores | Gana quien tenga la *iniciativa* en esa ronda, para que siempre haya un único ganador |

- **Iniciativa:** el turno inicial es aleatorio y la iniciativa alterna en cada ronda; sirve para desempatar.
- Cada carta se usa **una sola vez** (las usadas quedan oscurecidas, con su posición y borde verde/rojo según ganaran o no).
- **Link Monsters:** no tienen DEF, así que se muestran con `DEF -` y siempre se juegan en Ataque.
- **Validación Monster:** si la API devuelve una carta que no es Monster (Spell/Trap) o una repetida, se vuelve a solicitar (hasta 30 intentos).

## Diseño
El código está separado en paquetes: `model` (`Card`, `Position`), `api` (`YgoApiClient`, `YgoApiException`),
`duel` (`Duel`, `BattleListener`, `RoundResult`) y `ui` (`MainFrame`, `CardPanel` + `CardPanel.form`, `Theme`).
`YgoApiClient` usa `java.net.http.HttpClient` y `org.json`; sus llamadas son bloqueantes, por eso la UI las ejecuta siempre
dentro de un `SwingWorker`, que publica cada carta a medida que llega. La interfaz nunca se congela y los errores
("No se pudo cargar la carta", "Error de red") se muestran en la barra de estado y en el log.

`Duel` contiene las reglas y no conoce Swing: se comunica únicamente mediante `BattleListener`
(`onTurn`, `onScoreChanged`, `onDuelEnded`, más eventos opcionales como `onRoundStarted`). `MainFrame` implementa ese
listener y actualiza marcador, cartas y log a partir de esos eventos. Cada carta es un `CardPanel`, diseñado en el
GUI Designer (imagen, nombre, tipo, ATK, DEF e insignia de posición) y enlazado a su clase; reescala la ilustración
al tamaño disponible, de modo que la ventana se ajusta a cualquier pantalla y puede redimensionarse o maximizarse.

CAPTURAS DE PANTALLA:
<img width="866" height="476" alt="image" src="https://github.com/user-attachments/assets/57e4fff3-a1ff-45a9-a820-c4215031c87a" />

<img width="870" height="480" alt="image" src="https://github.com/user-attachments/assets/f5c63bc9-3422-42e3-a274-a13228af7a4f" />

<img width="866" height="476" alt="image" src="https://github.com/user-attachments/assets/292e5bd6-bd8f-479b-b313-dd8b842b1af9" />

<img width="869" height="481" alt="image" src="https://github.com/user-attachments/assets/ecab1bde-2e9a-4923-8dfa-6e170e6e0fa4" />

<img width="867" height="475" alt="image" src="https://github.com/user-attachments/assets/405f5930-c2ce-4d23-b6da-3bf58e4d7d38" />

<img width="862" height="478" alt="image" src="https://github.com/user-attachments/assets/6a7488e3-68b3-4922-8e08-bb445bd8a0f1" />

