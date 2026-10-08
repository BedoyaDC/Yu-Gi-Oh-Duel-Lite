package ygo;

import ygo.ui.MainFrame;

import javax.swing.SwingUtilities;

/** Punto de entrada: crea la ventana en el hilo de eventos de Swing (EDT). */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
