public class Measurement {

    public int source;
    public String name;
    public String type;
    public String value;

    public Measurement(int source, String name, String type, String value) {
        this.source = source;
        this.name = name;
        this.type = type;
        this.value = value;
    }

    // Проверка значения: scalar должен быть числом, bool — true/false
    public boolean isValid() {
        if (type.equals("scalar")) {
            try {
                double number = Double.parseDouble(value.replace(',', '.'));
                return !Double.isNaN(number) && !Double.isInfinite(number);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        if (type.equals("bool")) {
            return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
        }

        return false;
    }

    public boolean isTrend() {
        return Trends.isTrend(name);
    }

    @Override
    public String toString() {
        return "источник " + source + " | " + name + " = " + value;
    }
}
