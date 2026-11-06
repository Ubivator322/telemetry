import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class Database {

    private Connection connection;

    private PreparedStatement insertTelemetry;
    private PreparedStatement insertInvalid;
    private PreparedStatement insertTrend;
    private PreparedStatement insertWal;

    public Database(String fileName) throws Exception {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + fileName);

        createTables();

        // Дальше пишем вручную и фиксируем данные раз в секунду
        connection.setAutoCommit(false);
        prepareStatements();
    }

    private void createTables() throws Exception {
        Statement statement = connection.createStatement();

        // При каждом запуске база создаётся заново
        statement.execute("DROP TABLE IF EXISTS telemetry");
        statement.execute("DROP TABLE IF EXISTS invalid_log");
        statement.execute("DROP TABLE IF EXISTS trends");
        statement.execute("DROP TABLE IF EXISTS wal");

        statement.execute("CREATE TABLE telemetry (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ts INTEGER, source INTEGER, name TEXT, type TEXT, value TEXT)");

        statement.execute("CREATE TABLE invalid_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ts INTEGER, source INTEGER, name TEXT, type TEXT, value TEXT)");

        statement.execute("CREATE TABLE trends (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ts INTEGER, source INTEGER, name TEXT, type TEXT, value TEXT)");

        // Самописный WAL-журнал
        statement.execute("CREATE TABLE wal (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ts INTEGER, operation TEXT, payload TEXT)");

        statement.close();
    }

    private void prepareStatements() throws Exception {
        insertTelemetry = connection.prepareStatement(
                "INSERT INTO telemetry(ts, source, name, type, value) VALUES (?, ?, ?, ?, ?)");

        insertInvalid = connection.prepareStatement(
                "INSERT INTO invalid_log(ts, source, name, type, value) VALUES (?, ?, ?, ?, ?)");

        insertTrend = connection.prepareStatement(
                "INSERT INTO trends(ts, source, name, type, value) VALUES (?, ?, ?, ?, ?)");

        insertWal = connection.prepareStatement(
                "INSERT INTO wal(ts, operation, payload) VALUES (?, ?, ?)");
    }

    public void saveTelemetry(Measurement measurement, long ts) throws Exception {
        insertTelemetry.setLong(1, ts);
        insertTelemetry.setInt(2, measurement.source);
        insertTelemetry.setString(3, measurement.name);
        insertTelemetry.setString(4, measurement.type);
        insertTelemetry.setString(5, measurement.value);
        insertTelemetry.executeUpdate();
    }

    public void saveInvalid(Measurement measurement, long ts) throws Exception {
        insertInvalid.setLong(1, ts);
        insertInvalid.setInt(2, measurement.source);
        insertInvalid.setString(3, measurement.name);
        insertInvalid.setString(4, measurement.type);
        insertInvalid.setString(5, measurement.value);
        insertInvalid.executeUpdate();
    }

    public void saveTrendWithWal(Measurement measurement, long ts) throws Exception {
        String payload = measurement.source + "|"
                + measurement.name + "|"
                + measurement.type + "|"
                + measurement.value;

        // WAL: сначала фиксируем операцию, потом меняем таблицу trends
        insertWal.setLong(1, ts);
        insertWal.setString(2, "INSERT trend");
        insertWal.setString(3, payload);
        insertWal.executeUpdate();

        insertTrend.setLong(1, ts);
        insertTrend.setInt(2, measurement.source);
        insertTrend.setString(3, measurement.name);
        insertTrend.setString(4, measurement.type);
        insertTrend.setString(5, measurement.value);
        insertTrend.executeUpdate();
    }

    public void commit() throws Exception {
        connection.commit();
    }

    public void close() throws Exception {
        insertTelemetry.close();
        insertInvalid.close();
        insertTrend.close();
        insertWal.close();
        connection.close();
    }
}
