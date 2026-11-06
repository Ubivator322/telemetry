import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Random;

public class JsonGenerator {

    public static final int SOURCES = 4;
    public static final int TICKS = 300;
    public static final int OBJECTS = 1000;
    public static final int TRENDS = 20;

    private static final Random RANDOM = new Random();

    public static void generate() throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter("telemetry.json"));

        writer.write("{\"ticks\":[\n");

        for (int tick = 0; tick < TICKS; tick++) {
            writer.write("{\"sources\":{\n");

            for (int source = 1; source <= SOURCES; source++) {
                writeSource(writer, source);
                if (source < SOURCES) {
                    writer.write(",");
                }
                writer.write("\n");
            }

            writer.write("}}");
            if (tick < TICKS - 1) {
                writer.write(",");
            }
            writer.write("\n");
        }

        writer.write("]}\n");
        writer.close();
    }

    private static void writeSource(BufferedWriter writer, int source) throws Exception {
        writer.write("\"" + source + "\":[");

        boolean[] used = new boolean[OBJECTS];
        int count = 100 + RANDOM.nextInt(501); // 100..600 объектов
        int found = 0;

        while (found < count) {
            int index = RANDOM.nextInt(OBJECTS);
            if (used[index]) {
                continue;
            }

            used[index] = true;

            String name = objectName(index);
            String type = objectType(index);
            String value = generateValue(type);

            if (found > 0) {
                writer.write(",");
            }

            writer.write("{\"name\":\"" + name + "\",\"type\":\"" + type
                    + "\",\"value\":\"" + value + "\"}");

            found++;

            if (found % 20 == 0) {
                writer.write("\n");
            }
        }

        writer.write("]");
    }

    private static String objectName(int index) {
        if (index < TRENDS) {
            return "trend_" + (index + 1);
        }
        return "obj_" + (index - TRENDS + 1); // obj_1..obj_980
    }

    private static String objectType(int index) {
        if (index % 2 == 0) {
            return "scalar";
        }
        return "bool";
    }

    private static String generateValue(String type) {
        // Примерно 10% невалидных значений
        if (RANDOM.nextInt(100) < 10) {
            if (type.equals("scalar")) {
                return "bad";
            }
            return "maybe";
        }

        if (type.equals("scalar")) {
            double value = RANDOM.nextInt(10000) / 100.0;
            return String.valueOf(value);
        }

        return String.valueOf(RANDOM.nextBoolean());
    }

    public static void main(String[] args) throws Exception {
        generate();
        System.out.println("telemetry.json создан");
    }
}
