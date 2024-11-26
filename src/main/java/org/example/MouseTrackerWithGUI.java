package org.example;

import com.sun.jna.platform.unix.X11;
import com.sun.jna.ptr.IntByReference;

import javax.swing.*;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

public class MouseTrackerWithGUI extends JFrame {

    private JLabel mousePositionLabel;
    private JLabel distanceLabel;

    private int lastX = -1;
    private int lastY = -1;
    private double totalDistance = 0.0;

    public MouseTrackerWithGUI() {
        setTitle("Global Mouse Tracker");
        setSize(400, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(2, 1));

        // Создаем метки
        mousePositionLabel = new JLabel("Mouse Position: X=0, Y=0", SwingConstants.CENTER);
        distanceLabel = new JLabel("Total Distance: 0.0 meters", SwingConstants.CENTER);

        add(mousePositionLabel);
        add(distanceLabel);

        // Запускаем трекинг в фоновом потоке
        startTracking();
    }

    private void startTracking() {
        X11 x11 = X11.INSTANCE;
        X11.Display display = x11.XOpenDisplay(null);

        if (display == null) {
            JOptionPane.showMessageDialog(this, "Cannot open X11 display!", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        X11.Window root = x11.XDefaultRootWindow(display);
        System.out.println("Root window ID: " + root);

        // Устанавливаем обработку событий только для движения мыши
        x11.XSelectInput(display, root, new com.sun.jna.NativeLong(X11.PointerMotionMask));

        // Таймер для обновления GUI
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                trackMouseMovement(display, root, x11);
            }
        }, 0, 50); // Обновление каждые 50 мс
    }

    private void trackMouseMovement(X11.Display display, X11.Window root, X11 x11) {
        IntByReference rootX = new IntByReference();
        IntByReference rootY = new IntByReference();
        IntByReference winX = new IntByReference();
        IntByReference winY = new IntByReference();
        X11.WindowByReference child = new X11.WindowByReference();
        IntByReference mask = new IntByReference();

        x11.XQueryPointer(display, root, new X11.WindowByReference(), child, rootX, rootY, winX, winY, mask);

        int currentX = rootX.getValue();
        int currentY = rootY.getValue();

        if (lastX != -1 && lastY != -1) {
            int deltaX = currentX - lastX;
            int deltaY = currentY - lastY;
            totalDistance += Math.sqrt(deltaX * deltaX + deltaY * deltaY) * 0.000264583; // 96 DPI
        }

        lastX = currentX;
        lastY = currentY;

        // Обновляем GUI
        SwingUtilities.invokeLater(() -> {
            mousePositionLabel.setText(String.format("Mouse Position: X=%d, Y=%d", currentX, currentY));
            distanceLabel.setText(String.format("Total Distance: %.2f meters", totalDistance));
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MouseTrackerWithGUI tracker = new MouseTrackerWithGUI();
            tracker.setVisible(true);
        });
    }
}
