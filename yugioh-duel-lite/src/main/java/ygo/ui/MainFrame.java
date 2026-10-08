package ygo.ui;

import ygo.api.YgoApiClient;
import ygo.api.YgoApiException;
import ygo.duel.BattleListener;
import ygo.duel.Duel;
import ygo.duel.RoundResult;
import ygo.model.Card;
import ygo.model.Position;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Ventana principal. Implementa BattleListener: marcador, log, cartas usadas y ganador
 * se reflejan en pantalla a partir de los eventos del Duel.
 * Las peticiones a la API se hacen en un SwingWorker (la UI nunca se bloquea).
 */
public class MainFrame extends JFrame implements BattleListener {
    private static final int N = Duel.CARDS_PER_PLAYER;

    private final YgoApiClient client = new YgoApiClient();

    // Mazos y vistas
    private final Card[] playerCards = new Card[N];
    private final Card[] aiCards = new Card[N];
    private final CardPanel[] playerViews = new CardPanel[N];
    private final CardPanel[] aiViews = new CardPanel[N];

    // Controles
    private final JButton startButton = new JButton("Iniciar duelo");
    private final JButton chooseButton = new JButton("Elegir carta");
    private final JButton reloadButton = new JButton("Nuevas cartas");
    private final JRadioButton attackRadio = new JRadioButton("Ataque", true);
    private final JRadioButton defenseRadio = new JRadioButton("Defensa");
    private final JLabel roundLabel = new JLabel("Ronda - / " + N, SwingConstants.CENTER);
    private final JLabel turnLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel playerScoreLabel = new JLabel();
    private final JLabel aiScoreLabel = new JLabel();
    private final JTextArea logArea = new JTextArea();
    private final JLabel statusLabel = new JLabel(" ");

    // Estado de la UI
    private Duel duel;
    private boolean loading;
    private boolean deckReady;
    private boolean awaitingChoice;
    private int selectedIndex = -1;
    private int loadedCount;
    private RoundResult lastResult;

    /** Carta recién descargada por el SwingWorker (slot 0-2 jugador, 3-5 máquina). */
    private static final class Loaded {
        final int slot;
        final Card card;
        Loaded(int slot, Card card) { this.slot = slot; this.card = card; }
    }

    public MainFrame() {
        super("Yu-Gi-Oh! Duel Lite");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        setContentPane(root);

        root.add(buildScoreboard(), BorderLayout.NORTH);
        root.add(buildArena(), BorderLayout.CENTER);
        root.add(buildSouth(), BorderLayout.SOUTH);

        // Tamaño ajustado al área útil de la pantalla (sin barra de tareas); ventana redimensionable
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int w = Math.min(1180, usable.width - 30);
        int h = Math.min(780, usable.height - 20);
        setSize(w, h);
        setMinimumSize(new Dimension(Math.min(860, w), Math.min(580, h)));
        setLocationRelativeTo(null);

        updateScores(0, 0);
        refreshControls();
        loadDeck(); // "Al iniciar, cada jugador recibe 3 cartas Monster desde la API"
    }

    // ================================================================= construcción de la UI

    private JComponent buildScoreboard() {
        JLabel title = new JLabel("YU-GI-OH! DUEL LITE", SwingConstants.CENTER);
        title.setFont(Theme.BOLD.deriveFont(24f));
        title.setForeground(Theme.TEXT);

        playerScoreLabel.setFont(Theme.BOLD.deriveFont(20f));
        playerScoreLabel.setForeground(Theme.PLAYER);
        aiScoreLabel.setFont(Theme.BOLD.deriveFont(20f));
        aiScoreLabel.setForeground(Theme.MACHINE);
        aiScoreLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel bar = new JPanel(new GridBagLayout());
        bar.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.weightx = 1; bar.add(playerScoreLabel, gbc);
        gbc.gridx = 1; gbc.weightx = 1; bar.add(title, gbc);
        gbc.gridx = 2; gbc.weightx = 1; bar.add(aiScoreLabel, gbc);
        return bar;
    }

    private JComponent buildArena() {
        JPanel arena = new JPanel(new GridBagLayout());
        arena.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;

        gbc.gridx = 0; gbc.weightx = 1; gbc.insets = new Insets(0, 0, 0, 10);
        arena.add(buildSide("Jugador", Theme.PLAYER, playerViews, true), gbc);
        gbc.gridx = 1; gbc.weightx = 0;
        arena.add(buildCenter(), gbc);
        gbc.gridx = 2; gbc.weightx = 1; gbc.insets = new Insets(0, 0, 0, 0);
        arena.add(buildSide("Máquina", Theme.MACHINE, aiViews, false), gbc);
        return arena;
    }

    /** Lado del jugador o de la máquina: 3 cartas que se estiran con la ventana. */
    private JPanel buildSide(String title, Color accent, CardPanel[] views, boolean clickable) {
        JPanel side = new JPanel(new GridLayout(1, N, 8, 0));
        side.setOpaque(true);
        side.setBackground(Theme.BG);
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(accent, 2, true), " " + title.toUpperCase() + " ",
                TitledBorder.CENTER, TitledBorder.TOP, Theme.BOLD.deriveFont(14f), accent);
        side.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(2, 6, 6, 6)));

        for (int i = 0; i < N; i++) {
            final int index = i;
            CardPanel view = new CardPanel(accent); // ficha diseñada en CardPanel.form
            views[i] = view;
            if (clickable) view.onClick(() -> onPlayerCardClicked(index));
            side.add(view.getMainPanel());
        }
        return side;
    }

    /** Columna central: ronda, iniciativa, posición y botones. */
    private JComponent buildCenter() {
        roundLabel.setFont(Theme.BOLD.deriveFont(18f));
        roundLabel.setForeground(Theme.TEXT);
        turnLabel.setFont(Theme.BOLD.deriveFont(12f));
        turnLabel.setForeground(Theme.LABEL);
        turnLabel.setToolTipText("La iniciativa alterna cada ronda y desempata valores iguales");

        JLabel vs = new JLabel("VS", SwingConstants.CENTER);
        vs.setFont(Theme.BOLD.deriveFont(Font.BOLD | Font.ITALIC, 30f));
        vs.setForeground(Theme.TEXT);

        JLabel posTitle = new JLabel("Posición de la carta:", SwingConstants.CENTER);
        posTitle.setFont(Theme.BOLD.deriveFont(12f));
        posTitle.setForeground(Theme.LABEL);
        Theme.styleRadio(attackRadio);
        Theme.styleRadio(defenseRadio);
        ButtonGroup group = new ButtonGroup();
        group.add(attackRadio);
        group.add(defenseRadio);

        JPanel positions = new JPanel(new GridLayout(2, 1));
        positions.setOpaque(false);
        positions.add(attackRadio);
        positions.add(defenseRadio);

        // Botones (ActionListener obligatorios: "Iniciar duelo" y "Elegir carta")
        startButton.setEnabled(false);
        chooseButton.setEnabled(false);
        reloadButton.setEnabled(false);
        Theme.styleButton(startButton, Theme.START);
        Theme.styleButton(chooseButton, Theme.CHOOSE);
        Theme.styleButton(reloadButton, Theme.RELOAD);
        startButton.addActionListener(e -> startDuel());
        chooseButton.addActionListener(e -> chooseCard());
        reloadButton.addActionListener(e -> loadDeck());

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setPreferredSize(new Dimension(170, 100));

        center.add(Box.createVerticalGlue());
        center.add(centered(roundLabel, 150, 30));
        center.add(centered(turnLabel, 150, 20));
        center.add(Box.createVerticalStrut(6));
        center.add(centered(vs, 150, 46));
        center.add(Box.createVerticalStrut(6));
        center.add(centered(posTitle, 150, 20));
        center.add(centered(positions, 120, 52));
        center.add(Box.createVerticalStrut(10));
        center.add(centered(chooseButton, 150, 40));
        center.add(Box.createVerticalStrut(8));
        center.add(centered(startButton, 150, 40));
        center.add(Box.createVerticalStrut(8));
        center.add(centered(reloadButton, 150, 36));
        center.add(Box.createVerticalGlue());
        return center;
    }

    private static JComponent centered(JComponent c, int width, int height) {
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        c.setMaximumSize(new Dimension(width, height));
        c.setPreferredSize(new Dimension(width, height));
        return c;
    }

    private JComponent buildSouth() {
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBackground(Color.WHITE);
        logArea.setForeground(Theme.TEXT);
        logArea.setMargin(new Insets(4, 8, 4, 8));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setPreferredSize(new Dimension(100, 115));
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Theme.FIELD_BORDER, 1, true), " Log de batalla ",
                TitledBorder.LEFT, TitledBorder.TOP, Theme.BOLD.deriveFont(12f), Theme.LABEL));

        statusLabel.setFont(Theme.BOLD);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));

        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.setOpaque(false);
        south.add(scroll, BorderLayout.CENTER);
        south.add(statusLabel, BorderLayout.SOUTH);
        return south;
    }

    // ================================================================= carga del mazo (hilo de fondo)

    /** Pide 6 cartas Monster (3 + 3) a la API en un SwingWorker, sin congelar la ventana. */
    private void loadDeck() {
        loading = true;
        deckReady = false;
        awaitingChoice = false;
        selectedIndex = -1;
        loadedCount = 0;
        duel = null;
        for (int i = 0; i < N; i++) {
            playerCards[i] = null;
            aiCards[i] = null;
            playerViews[i].reset();
            aiViews[i].reset();
            playerViews[i].setSelectable(false);
        }
        roundLabel.setText("Ronda - / " + N);
        turnLabel.setText(" ");
        updateScores(0, 0);
        setStatus("Cargando cartas (0/" + (2 * N) + ")...", Theme.INFO);
        log("Solicitando " + (2 * N) + " cartas Monster a YGOProDeck...");
        refreshControls();

        new SwingWorker<Void, Loaded>() {
            @Override
            protected Void doInBackground() throws Exception {
                Set<Integer> usedIds = new HashSet<>();
                for (int slot = 0; slot < 2 * N; slot++) {
                    Card card = client.fetchRandomMonster(usedIds); // hilo de fondo
                    usedIds.add(card.getId());
                    publish(new Loaded(slot, card));
                }
                return null;
            }

            @Override
            protected void process(List<Loaded> chunks) { // EDT
                for (Loaded l : chunks) {
                    if (l.slot < N) {
                        playerCards[l.slot] = l.card;
                        playerViews[l.slot].setCard(l.card);
                    } else {
                        aiCards[l.slot - N] = l.card;
                        aiViews[l.slot - N].setCard(l.card);
                    }
                    loadedCount++;
                    setStatus("Cargando cartas (" + loadedCount + "/" + (2 * N) + ")...", Theme.INFO);
                }
            }

            @Override
            protected void done() { // EDT
                loading = false;
                try {
                    get();
                    deckReady = true;
                    setStatus("Cartas listas. Pulsa «Iniciar duelo».", Theme.OK);
                    log("Cartas cargadas correctamente.");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    showError(cause instanceof YgoApiException ? cause.getMessage()
                            : "No se pudo cargar la carta: " + cause);
                    for (int i = 0; i < N; i++) { // los espacios vacíos pasan de "Cargando" a "Sin carta"
                        if (playerCards[i] == null) playerViews[i].setLoading(false);
                        if (aiCards[i] == null) aiViews[i].setLoading(false);
                    }
                    log("Pulsa «Nuevas cartas» para reintentar.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                refreshControls();
            }
        }.execute();
    }

    // ================================================================= acciones de la UI (EDT)

    private void startDuel() {
        if (!deckReady) return; // validación: no iniciar sin las 3 + 3 cartas cargadas
        for (int i = 0; i < N; i++) {
            if (playerCards[i] == null || aiCards[i] == null) return;
        }
        duel = new Duel(Arrays.asList(playerCards), Arrays.asList(aiCards), this);
        logArea.setText("");
        duel.start();
    }

    private void onPlayerCardClicked(int index) {
        if (!awaitingChoice || duel == null || !duel.isActive() || duel.isPlayerCardUsed(index)) return;
        selectedIndex = index;
        for (int i = 0; i < N; i++) playerViews[i].setSelected(i == index);

        // Los Link Monsters no pueden estar en defensa
        Card card = playerCards[index];
        defenseRadio.setEnabled(!card.isLink());
        if (card.isLink()) attackRadio.setSelected(true);
        setStatus("Elegiste «" + card.getName() + "». Escoge posición y pulsa «Elegir carta».", Theme.INFO);
        refreshControls();
    }

    /** "Elegir carta": el jugador juega la carta seleccionada; la máquina responde tras una breve pausa. */
    private void chooseCard() {
        if (!awaitingChoice || selectedIndex < 0 || duel == null) return;
        final int index = selectedIndex;
        final Position position = defenseRadio.isSelected() ? Position.DEFENSE : Position.ATTACK;

        awaitingChoice = false;
        refreshControls();
        setStatus("La máquina está eligiendo su carta...", Theme.WIN);

        javax.swing.Timer timer = new javax.swing.Timer(700, e -> duel.playRound(index, position));
        timer.setRepeats(false);
        timer.start();
    }

    /** Habilita/deshabilita controles según el estado (cargando, listo, duelo en curso). */
    private void refreshControls() {
        boolean dueling = duel != null && duel.isActive();
        startButton.setEnabled(deckReady && !loading && !dueling);
        reloadButton.setEnabled(!loading && !dueling);
        chooseButton.setEnabled(dueling && awaitingChoice && selectedIndex >= 0);
        boolean canPick = dueling && awaitingChoice;
        attackRadio.setEnabled(canPick);
        if (!canPick) defenseRadio.setEnabled(false);
        else defenseRadio.setEnabled(selectedIndex < 0 || !playerCards[selectedIndex].isLink());
        for (int i = 0; i < N; i++) {
            playerViews[i].setSelectable(canPick && !duel.isPlayerCardUsed(i));
        }
    }

    private void showError(String message) {
        setStatus("⚠ " + message, Theme.BAD);
        log("[ERROR] " + message);
    }

    private void setStatus(String text, Color color) {
        statusLabel.setForeground(color);
        statusLabel.setText(text);
    }

    private void log(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength()); // auto-scroll
    }

    private void updateScores(int player, int ai) {
        playerScoreLabel.setText("JUGADOR  " + player);
        aiScoreLabel.setText("MÁQUINA  " + ai);
    }

    private static String ownerName(boolean player) { return player ? Duel.PLAYER_NAME : Duel.AI_NAME; }

    // ================================================================= BattleListener
    // Duel se ejecuta en el EDT (lo llama un Timer de Swing), así que se puede tocar la UI directamente.

    @Override
    public void onDuelStarted(boolean playerStarts) {
        for (int i = 0; i < N; i++) {
            playerViews[i].clearDuelState();
            aiViews[i].clearDuelState();
        }
        selectedIndex = -1;
        log("=== DUELO INICIADO ===");
        log("Turno inicial (iniciativa): " + ownerName(playerStarts));
    }

    @Override
    public void onRoundStarted(int round, boolean playerHasInitiative) {
        roundLabel.setText("Ronda " + round + " / " + N);
        turnLabel.setText("Iniciativa: " + ownerName(playerHasInitiative));
        turnLabel.setForeground(playerHasInitiative ? Theme.PLAYER : Theme.MACHINE);
        selectedIndex = -1;
        awaitingChoice = true;
        attackRadio.setSelected(true);
        for (int i = 0; i < N; i++) playerViews[i].setSelected(false);
        setStatus("Ronda " + round + ": elige una de tus cartas disponibles.", Theme.INFO);
        refreshControls();
    }

    @Override
    public void onRoundPlayed(RoundResult r) {
        lastResult = r;
        playerViews[r.getPlayerIndex()].markUsed(r.getPlayerPosition(), r.isPlayerWon());
        aiViews[r.getAiIndex()].markUsed(r.getAiPosition(), !r.isPlayerWon());
    }

    @Override
    public void onTurn(String playerCard, String aiCard, String winner) {
        boolean playerFirst = duel == null || duel.isPlayerInitiative();
        log("--- Ronda " + (duel == null ? "?" : String.valueOf(duel.getRoundsPlayed())) + " ---");
        String p = "  " + Duel.PLAYER_NAME + " jugó: " + playerCard;
        String a = "  " + Duel.AI_NAME + " jugó:   " + aiCard;
        log(playerFirst ? p : a);
        log(playerFirst ? a : p);
        log("  Resultado: gana " + winner + (lastResult == null ? "" : ". " + lastResult.getExplanation()));
        setStatus("Gana la ronda: " + winner, Duel.PLAYER_NAME.equals(winner) ? Theme.OK : Theme.BAD);
    }

    @Override
    public void onScoreChanged(int playerScore, int aiScore) {
        updateScores(playerScore, aiScore);
        log("  Puntaje: " + Duel.PLAYER_NAME + " " + playerScore + " - " + aiScore + " " + Duel.AI_NAME);
    }

    @Override
    public void onDuelEnded(String winner) {
        awaitingChoice = false;
        selectedIndex = -1;
        roundLabel.setText("Fin del duelo");
        turnLabel.setText(" ");
        log("*** ¡" + winner.toUpperCase() + " GANA EL DUELO! ***");
        setStatus("Ganador final: " + winner + ". Pulsa «Iniciar duelo» para la revancha.",
                Duel.PLAYER_NAME.equals(winner) ? Theme.OK : Theme.BAD);
        refreshControls();
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                "¡Ganador del duelo: " + winner + "!", "Fin del duelo", JOptionPane.INFORMATION_MESSAGE));
    }
}
