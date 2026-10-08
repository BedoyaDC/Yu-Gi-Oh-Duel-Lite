package ygo.ui;

import ygo.model.Card;
import ygo.model.Position;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

/**
 * Ficha de una carta diseñada con el GUI Designer de IntelliJ (CardPanel.form).
 * Los campos marcados como "enlazados" corresponden por nombre a componentes del formulario:
 * IntelliJ los inicializa automáticamente al construir el objeto (no uses new sobre ellos).
 *
 * Es el componente que la ventana principal usa para las 3 cartas del jugador y las 3 de la máquina.
 * Maneja sus estados visuales: cargando, seleccionada, usada (con posición) y resultado de la ronda.
 */
public class CardPanel {
    private static final Color USED_BG = new Color(214, 219, 232);

    // ---- Componentes enlazados con CardPanel.form ----
    private JPanel mainPanel;
    private JLabel positionLabel;
    private JLabel imageLabel;
    private JLabel nameLabel;
    private JLabel typeLabel;
    private JTextField atkField;
    private JTextField defField;

    private final Color accent;      // color del bando (azul jugador / rojo máquina)
    private Card card;
    private Image source;            // ilustración original, para reescalarla al cambiar el tamaño
    private boolean loading = true;
    private boolean selected;
    private boolean used;
    private Position position;       // posición con la que se jugó (null si aún no se juega)
    private Color outcome;           // verde = ganó la ronda, rojo = la perdió

    public CardPanel(Color accent) {
        this.accent = accent;
        if (mainPanel == null) {
            throw new IllegalStateException("El formulario CardPanel.form no fue inicializado. "
                    + "Ejecuta desde IntelliJ (Build and run using: IntelliJ IDEA) y revisa "
                    + "Settings > Editor > GUI Designer.");
        }
        applyTheme();
        imageLabel.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { renderImage(); }
        });
        refresh();
    }

    public JPanel getMainPanel() { return mainPanel; }
    public Card getCard() { return card; }

    // ---------------------------------------------------------------- estado

    /** Muestra los datos de una carta (imagen, nombre, tipo, ATK y DEF). */
    public void setCard(Card c) {
        card = c;
        loading = false;
        source = c == null ? null : c.getImage();
        refresh();
    }

    /** Indica que la carta aún se está descargando. */
    public void setLoading(boolean value) {
        loading = value;
        refresh();
    }

    public void setSelected(boolean value) {
        selected = value;
        refresh();
    }

    /** Cambia el cursor a "mano" cuando la carta se puede elegir. */
    public void setSelectable(boolean value) {
        applyCursor(mainPanel, Cursor.getPredefinedCursor(value ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    /** Marca la carta como jugada, con su posición y si ganó o perdió la ronda. */
    public void markUsed(Position pos, boolean won) {
        used = true;
        selected = false;
        position = pos;
        outcome = won ? Theme.OK : Theme.BAD;
        refresh();
    }

    /** Quita las marcas del duelo (conserva la carta). */
    public void clearDuelState() {
        used = false;
        selected = false;
        position = null;
        outcome = null;
        refresh();
    }

    /** Vacía la carta (para recargar el mazo); queda en estado "cargando". */
    public void reset() {
        card = null;
        source = null;
        loading = true;
        clearDuelState();
    }

    /** Registra una acción para el clic sobre cualquier parte de la ficha. */
    public void onClick(Runnable action) {
        attachClick(mainPanel, new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { action.run(); }
        });
    }

    // ---------------------------------------------------------------- aspecto

    private void applyTheme() {
        mainPanel.setOpaque(true);

        positionLabel.setForeground(Color.WHITE);
        positionLabel.setFont(Theme.BOLD.deriveFont(11f));
        positionLabel.setHorizontalAlignment(SwingConstants.CENTER);

        imageLabel.setOpaque(true);
        imageLabel.setBackground(Theme.FIELD_BG);
        imageLabel.setForeground(Theme.LABEL);
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setBorder(BorderFactory.createLineBorder(Theme.FIELD_BORDER, 1, true));

        nameLabel.setFont(Theme.BOLD.deriveFont(13f));
        typeLabel.setFont(Theme.BOLD.deriveFont(11f));
        typeLabel.setForeground(Theme.LABEL);

        for (JTextField f : new JTextField[]{atkField, defField}) {
            f.setEditable(false);
            f.setFocusable(false);
            f.setOpaque(true);
            f.setBackground(Theme.FIELD_BG);
            f.setFont(Theme.BOLD);
            f.setHorizontalAlignment(SwingConstants.CENTER);
            f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.FIELD_BORDER, 1, true),
                    BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        }
        atkField.setForeground(Theme.ATK);
        defField.setForeground(Theme.DEF);
    }

    /** Vuelca el estado actual (carta + marcas) en los componentes del formulario. */
    private void refresh() {
        // Fondo y borde según el estado
        mainPanel.setBackground(used ? USED_BG : Theme.CARD);
        Color border = outcome != null ? outcome : (selected ? Theme.GOLD : accent);
        int thickness = (outcome != null || selected) ? 4 : 2;
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, thickness, true),
                BorderFactory.createEmptyBorder(6 - thickness, 6 - thickness, 6 - thickness, 6 - thickness)));

        // Insignia de posición (siempre ocupa su espacio para que nada "salte")
        if (position != null) {
            positionLabel.setOpaque(true);
            positionLabel.setBackground(position == Position.ATTACK ? Theme.ATK : Theme.DEF);
            positionLabel.setText(position.getLabel());
        } else {
            positionLabel.setOpaque(false);
            positionLabel.setText(" ");
        }
        positionLabel.repaint();

        // Textos
        if (card == null) {
            nameLabel.setText("-");
            nameLabel.setToolTipText(null);
            typeLabel.setText(" ");
            atkField.setText("-");
            defField.setText("-");
        } else {
            nameLabel.setText(card.getName());
            nameLabel.setToolTipText(card.getName());   // nombre completo si no cabe
            typeLabel.setText(card.getType());
            atkField.setText(String.valueOf(card.getAtk()));
            defField.setText(card.isLink() ? "-" : String.valueOf(card.getDef()));
        }
        nameLabel.setForeground(used ? Theme.LABEL : Theme.TEXT);

        renderImage();
    }

    /** Dibuja la ilustración del tamaño del espacio disponible (y la oscurece si la carta ya se usó). */
    private void renderImage() {
        int w = source == null ? 0 : source.getWidth(null);
        int h = source == null ? 0 : source.getHeight(null);
        if (w <= 0 || h <= 0) {
            imageLabel.setIcon(null);
            imageLabel.setText(card != null ? "Sin imagen" : (loading ? "Cargando..." : "Sin carta"));
            return;
        }
        int boxW = imageLabel.getWidth() - 8, boxH = imageLabel.getHeight() - 8;
        if (boxW < 40 || boxH < 40) { boxW = 168; boxH = 246; } // antes del primer layout
        double s = Math.min((double) boxW / w, (double) boxH / h);
        int nw = Math.max(1, (int) (w * s)), nh = Math.max(1, (int) (h * s));

        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(source, 0, 0, nw, nh, null);
        if (used) {
            g.setColor(new Color(40, 44, 60, 120)); // velo: carta ya usada
            g.fillRect(0, 0, nw, nh);
        }
        g.dispose();
        imageLabel.setText(null);
        imageLabel.setIcon(new ImageIcon(out));
    }

    // ---------------------------------------------------------------- utilidades

    /** Los hijos (campos de texto, etiquetas) capturan el clic: se registra el listener en todos. */
    private static void attachClick(Component c, MouseListener listener) {
        c.addMouseListener(listener);
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) attachClick(child, listener);
        }
    }

    private static void applyCursor(Component c, Cursor cursor) {
        c.setCursor(cursor);
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) applyCursor(child, cursor);
        }
    }
}
