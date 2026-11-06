import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class TelemetryWindow extends JFrame {

    public static final int TICKS = 300;
    public static final int WINDOW_SIZE = 3600;
    public static final int TICK_DELAY_MS = 50;

    private JLabel tickLabel = new JLabel("Секунда: 0 / " + TICKS);
    private JLabel statsLabel = new JLabel("Всего: 0 | Валидных: 0 | Невалидных: 0 | Трендов (WAL): 0");
    private JLabel windowLabel = new JLabel("Окно трендов: 0 / " + WINDOW_SIZE);

    private DefaultListModel<String> trendModel = new DefaultListModel<>();
    private JList<String> trendList = new JList<>(trendModel);
    private JButton startButton = new JButton("Старт");

    private Timer timer;
    private Database database;
    private JSONArray ticks;

    private int tick = 0;
    private int total = 0;
    private int valid = 0;
    private int invalid = 0;
    private int trends = 0;

    public TelemetryWindow() {
        setTitle("Сбор телеметрии");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new GridLayout(3, 1));
        top.add(tickLabel);
        top.add(statsLabel);
        top.add(windowLabel);
        add(top, BorderLayout.NORTH);

        trendList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(trendList), BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.add(startButton);
        add(bottom, BorderLayout.SOUTH);

        timer = new Timer(TICK_DELAY_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nextTick();
            }
        });

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                start();
            }
        });
    }

    private void start() {
        try {
            startButton.setEnabled(false);

            File jsonFile = new File("telemetry.json");
            if (!jsonFile.exists()) {
                tickLabel.setText("Генерация telemetry.json...");
                JsonGenerator.generate();
            }

            tickLabel.setText("Загрузка telemetry.json...");
            String text = Files.readString(Path.of("telemetry.json"));
            JSONObject root = new JSONObject(text);
            ticks = root.getJSONArray("ticks");

            database = new Database("telemetry.db");
            timer.start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            startButton.setEnabled(true);
        }
    }

    private void nextTick() {
        try {
            if (tick >= TICKS) {
                finish();
                return;
            }

            long now = System.currentTimeMillis();
            JSONObject tickObject = ticks.getJSONObject(tick);
            JSONObject sources = tickObject.getJSONObject("sources");

            for (int source = 1; source <= JsonGenerator.SOURCES; source++) {
                JSONArray objects = sources.getJSONArray(String.valueOf(source));

                for (int i = 0; i < objects.length(); i++) {
                    JSONObject object = objects.getJSONObject(i);

                    Measurement measurement = new Measurement(
                            source,
                            object.getString("name"),
                            object.getString("type"),
                            object.getString("value")
                    );

                    total++;

                    if (measurement.isValid()) {
                        valid++;
                        database.saveTelemetry(measurement, now);

                        if (measurement.isTrend()) {
                            trends++;
                            database.saveTrendWithWal(measurement, now);
                            addTrendEvent(measurement);
                        }
                    } else {
                        invalid++;
                        database.saveInvalid(measurement, now);
                    }
                }
            }

            database.commit();
            tick++;
            updateLabels();
        } catch (Exception e) {
            timer.stop();
            JOptionPane.showMessageDialog(this, "Ошибка обработки: " + e.getMessage());
            startButton.setEnabled(true);
        }
    }

    private void addTrendEvent(Measurement measurement) {
        String event = "Секунда " + (tick + 1) + " | " + measurement;
        trendModel.addElement(event);

        // Постоянное окно видимости на 3600 элементов
        if (trendModel.size() > WINDOW_SIZE) {
            trendModel.remove(0);
        }
    }

    private void updateLabels() {
        tickLabel.setText("Секунда: " + tick + " / " + TICKS);
        statsLabel.setText("Всего: " + total
                + " | Валидных: " + valid
                + " | Невалидных: " + invalid
                + " | Трендов (WAL): " + trends);
        windowLabel.setText("Окно трендов: " + trendModel.size() + " / " + WINDOW_SIZE);
    }

    private void finish() {
        timer.stop();

        try {
            database.close();
        } catch (Exception e) {
            // база всё равно закрывается при выходе из программы
        }

        tickLabel.setText("Готово: " + TICKS + " секунд");
        startButton.setEnabled(false);

        System.out.println("Обработка завершена");
        System.out.println("Всего: " + total);
        System.out.println("Валидных: " + valid);
        System.out.println("Невалидных: " + invalid);
        System.out.println("Трендов: " + trends);
        System.out.println("Окно трендов: " + trendModel.size());
    }
}
