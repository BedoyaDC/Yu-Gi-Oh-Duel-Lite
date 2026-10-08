package ygo.ui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Colores y estilos sencillos compartidos por la interfaz (tema claro con acentos de color). */
final class Theme {
    private Theme() {}

    static final Color BG = new Color(232, 237, 247);          // fondo de la ventana
    static final Color CARD = new Color(250, 251, 255);        // fondo de cada carta
    static final Color LABEL = new Color(70, 80, 115);
    static final Color TEXT = new Color(35, 42, 70);
    static final Color FIELD_BG = new Color(238, 241, 249);
    static final Color FIELD_BORDER = new Color(190, 198, 222);

    static final Color PLAYER = new Color(25, 118, 210);       // jugador: azul
    static final Color MACHINE = new Color(211, 47, 47);       // máquina: rojo
    static final Color START = new Color(46, 125, 50);         // Iniciar duelo
    static final Color CHOOSE = new Color(245, 124, 0);        // Elegir carta
    static final Color RELOAD = new Color(96, 110, 150);       // Nuevas cartas
    static final Color DISABLED = new Color(176, 182, 200);
    static final Color GOLD = new Color(255, 193, 7);          // carta seleccionada
    static final Color ATK = new Color(198, 40, 40);
    static final Color DEF = new Color(21, 101, 192);

    static final Color OK = new Color(46, 125, 50);
    static final Color BAD = new Color(198, 40, 40);
    static final Color INFO = new Color(21, 101, 192);
    static final Color WIN = new Color(230, 126, 0);

    static final Font BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 13);

    /** Botón de color con efecto hover y estado deshabilitado. Llamar DESPUÉS de fijar enabled inicial. */
    static void styleButton(JButton b, Color base) {
        b.setUI(new BasicButtonUI());
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setFont(BOLD);
        b.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(b.isEnabled() ? base : DISABLED);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (b.isEnabled()) b.setBackground(base.brighter()); }
            @Override public void mouseExited(MouseEvent e) { b.setBackground(b.isEnabled() ? base : DISABLED); }
        });
        b.addPropertyChangeListener("enabled", e -> b.setBackground(b.isEnabled() ? base : DISABLED));
    }

    static void styleRadio(JRadioButton r) {
        r.setOpaque(false);
        r.setForeground(TEXT);
        r.setFont(BOLD.deriveFont(12f));
        r.setFocusPainted(false);
    }
}
