package model;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by TRUST on 27.10.2016.
 */
public class SQLiteHandler implements FileIOHandler {

    private Connection connection;

    public SQLiteHandler() {
        String url = "jdbc:sqlite:caseStorage.db";
        try {
            connection = DriverManager.getConnection(url);
            System.out.println("Connection to SQLite has been established.");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Initializing the connection to SQLite was failed.");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }));

        //Temporary solving of DB initialization. Need to decide where to move this
        try {
            String sql = "CREATE TABLE IF NOT EXISTS \"numbers\" (\n" +
                    "    \"num_id\" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
                    "    \"number\" TEXT NOT NULL\n" +
                    ");\n" +
                    "CREATE UNIQUE INDEX unq_number ON numbers(number);";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.executeUpdate();
            sql = "CREATE TABLE IF NOT EXISTS \"hearings\" (\n" +
                    "    \"id\" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
                    "    \"date\" TEXT,\n" +
                    "    \"num_id\" TEXT,\n" +
                    "    \"involved\" TEXT,\n" +
                    "    \"description\" TEXT,\n" +
                    "    \"judge\" TEXT,\n" +
                    "    \"form\" TEXT,\n" +
                    "    \"address\" TEXT\n" +
                    ");";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.executeUpdate();
            System.out.println("DB init finished.");
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException("Crashed when tried to run SQL-query for initializing the tables in DB.\n" + e);
        }
    }

    @Override
    public List<String> getAllNumbers() {
        List<String> ids = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT number FROM numbers");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                ids.add(resultSet.getString("number"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return ids;
    }

    @Override
    public List<CourtCase> getCurrentListOfCases() {
        List<CourtCase> caseList = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT h.date, n.number, h.involved, h.description, h.judge, h.form, h.address\n" +
                            "FROM hearings h\n" +
                            "JOIN numbers n ON h.num_id=n.num_id;");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                CourtCase courtCase = new CourtCase(
                        resultSet.getString("date"),
                        resultSet.getString("number"),
                        resultSet.getString("involved"),
                        resultSet.getString("description"),
                        resultSet.getString("judge"),
                        resultSet.getString("form"),
                        resultSet.getString("address")
                );
                caseList.add(courtCase);
                resultSet.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return caseList;
    }

    @Override
    public void save(List<CourtCase> listOfRows) throws IOException {
        String sql = buildSaveTransaction(listOfRows);
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeQuery();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addNumber(String number) {
        try {
            String sql = "INSERT OR IGNORE INTO numbers (number) VALUES (?);";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, number);
            statement.executeQuery();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteNumber(String number) {
        try {
            String sql = "DELETE FROM numbers WHERE number = ?;";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, number);
            statement.executeQuery();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }



    // is it an extremely ugly solution?
    private String buildSaveTransaction(List<CourtCase> listOfRows) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN;\nDELETE FROM hearings;\n");
        for (CourtCase courtCase : listOfRows) {
            int numId;
            try {
                String sql = "SELECT num_id FROM numbers WHERE number=?";
                PreparedStatement stmnt = connection.prepareStatement(sql);
                stmnt.setString(1, courtCase.getNumber());
                ResultSet resultSet = stmnt.executeQuery();
                resultSet.next();
                numId = resultSet.getInt("num_id");
                resultSet.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            sb.append("INSERT INTO hearings (date, num_id, involved, description, judge, form, address) VALUES " +
                    "(" + courtCase.getDate() + ", " +
                    numId + ", " +
                    courtCase.getInvolved() + ", " +
                    courtCase.getDescription() + ", " +
                    courtCase.getJudge() + ", " +
                    courtCase.getForma() + ", " +
                    courtCase.getAdd_address() + ");\n");
        }
        sb.append("COMMIT;");
        System.out.println(sb.toString());
        return sb.toString();
    }
}
