public class Trends {

    // 20 предопределённых трендов из 1000 объектов
    public static final String[] NAMES = new String[20];

    static {
        for (int i = 0; i < NAMES.length; i++) {
            NAMES[i] = "trend_" + (i + 1);
        }
    }

    public static boolean isTrend(String name) {
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(name)) {
                return true;
            }
        }
        return false;
    }
}
