package me.pulsi_.bankplus.sql;

import me.pulsi_.bankplus.BankPlus;
import me.pulsi_.bankplus.bankSystem.BankRegistry;
import me.pulsi_.bankplus.economy.BPEconomy;
import me.pulsi_.bankplus.utils.BPLogger;
import me.pulsi_.bankplus.values.ConfigValues;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * BankPlus MySQL system save both database and files to synchronize
 * them, local files data is always available and updated, and will
 * be updated on the database.
 * <p>
 * In case the database is not available or the connection has been
 * closed, the data will continue to be saved locally, and once the
 * database will connect again the local data will be updated.
 */
public class BPSQL {

    public enum SQLSearch {
        BANK_LEVEL,
        DEBT,
        INTEREST,
        MONEY
    }

    /**
     * Get the string of default arguments that a bankplus table has.
     *
     * @return The default arguments of the bankplus table.
     */
    public static String GET_TABLE_ARGUMENTS() {
        return "uuid varchar(255)," +
                "account_name varchar(255) DEFAULT ''," +
                "bank_level varchar(255) DEFAULT '1'," +
                "money varchar(255) DEFAULT '0'," +
                "interest varchar(255) DEFAULT '0'," +
                "debt varchar(255) DEFAULT '0'," +
                "PRIMARY KEY (uuid)";
    }

    /**
     * Return a string representing all the default arguments for that specified player.
     * More specifically, returns the following values:
     * - UUID
     * - Name
     * - BankLevel = 1
     * - Money = 0 (Or default amount)
     * - Interest = 0
     * - Debt = 0
     *
     * @param p        The player where to retrieve the arguments.
     * @param bankName The bank where to get the information.
     * @return The argument based on the specified player.
     */
    public static String GET_DEFAULT_PLAYER_ARGUMENTS(OfflinePlayer p, String bankName) {
        String playerName = p.getName();
        String startAmount = ConfigValues.getMainGuiName().equals(bankName) ? ConfigValues.getStartAmount().toPlainString() : "0";
        return "'" + p.getUniqueId() + "'," +
                "'" + (playerName == null ? "" : playerName.replace("'", "''")) + "'," +
                "'1'," +
                "'" + startAmount + "'," +
                "'0'," +
                "'0'," +
                "'0'";
    }

    private static Connection connection;

    /**
     * Get the connection, that can be to MySQL or SQLite.
     *
     * @return The connection object.
     */
    public static Connection getConnection() {
        return connection;
    }

    public static void disconnect() {
        if (connection == null) return;

        try {
            connection.close();
            connection = null;
        } catch (SQLException e) {
            BPLogger.Console.error(e, "Could not close SQL connection.");
        }
    }

    /**
     * Get the bank level of the specified player in the specified bank.
     *
     * @param player   The player.
     * @param bankName The bank.
     * @return The bank level or 0 if not found.
     */
    public static int getBankLevel(OfflinePlayer player, String bankName) {
        String level = get(player, bankName, SQLSearch.BANK_LEVEL);
        if (level == null || level.isEmpty()) return 0;

        try {
            return Integer.parseInt(level);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Get the debt of the specified player in the specified bank.
     *
     * @param player   The player.
     * @param bankName The bank.
     * @return The debt or ZERO if not found.
     */
    public static BigDecimal getDebt(OfflinePlayer player, String bankName) {
        String debt = get(player, bankName, SQLSearch.DEBT);
        if (debt == null || debt.isEmpty()) return BigDecimal.ZERO;

        try {
            return new BigDecimal(debt);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Get the interest of the specified player in the specified bank.
     *
     * @param player   The player.
     * @param bankName The bank.
     * @return The interest or ZERO if not found.
     */
    public static BigDecimal getInterest(OfflinePlayer player, String bankName) {
        String interest = get(player, bankName, SQLSearch.INTEREST);
        if (interest == null || interest.isEmpty()) return BigDecimal.ZERO;

        try {
            return new BigDecimal(interest);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Get the money of the specified player in the specified bank.
     *
     * @param player   The player.
     * @param bankName The bank.
     * @return The money or ZERO if not found.
     */
    public static BigDecimal getMoney(OfflinePlayer player, String bankName) {
        String money = get(player, bankName, SQLSearch.MONEY);
        if (money == null || money.isEmpty()) return BigDecimal.ZERO;

        try {
            return new BigDecimal(money);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Set the debt of the specified player in the specified bank to the selected amount.
     *
     * @param player   The player.
     * @param bankName The bank.
     * @param newValue The new value for the money.
     */
    public static void setBankLevel(OfflinePlayer player, String bankName, BigDecimal newValue) {
        set(player, bankName, SQLSearch.BANK_LEVEL, newValue.toPlainString());
    }

    /**
     * Set the debt of the specified player in the specified bank to the selected amount.
     *
     * @param player   The player.
     * @param bankName The bank.
     * @param newValue The new value for the money.
     */
    public static void setDebt(OfflinePlayer player, String bankName, BigDecimal newValue) {
        set(player, bankName, SQLSearch.DEBT, newValue.toPlainString());
    }

    /**
     * Set the interest (interest earned while being offline to show to the player when
     * entering the server) of the specified player in the specified bank to the selected amount.
     *
     * @param player   The player.
     * @param bankName The bank.
     * @param newValue The new value for the money.
     */
    public static void setInterest(OfflinePlayer player, String bankName, BigDecimal newValue) {
        set(player, bankName, SQLSearch.INTEREST, newValue.toPlainString());
    }

    /**
     * Set the money of the specified player in the specified bank to the selected amount.
     *
     * @param player   The player.
     * @param bankName The bank.
     * @param newValue The new value for the money.
     */
    public static void setMoney(OfflinePlayer player, String bankName, BigDecimal newValue) {
        set(player, bankName, SQLSearch.MONEY, newValue.toPlainString());
    }

    /**
     * Save all the player's current bank statistics to the database.
     *
     * @param player   The player to save.
     * @param bankEconomy The economy of the bank.
     */
    public static void savePlayer(OfflinePlayer player, BPEconomy bankEconomy) {
        String bankName = bankEconomy.getOriginBank().getIdentifier();

        int level = bankEconomy.getBankLevel(player);
        String debt = bankEconomy.getDebt(player).toPlainString();
        String money = bankEconomy.getBankBalance(player).toPlainString();

        String query;
        if (ConfigValues.isMySqlEnabled())
            query = "INSERT INTO " + bankName + " (uuid, bank_level, debt, money) " +
                    "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                    "bank_level=VALUES(bank_level), debt=VALUES(debt), money=VALUES(money)";
        else query = "INSERT INTO " + bankName + " (uuid, bank_level, debt, money) " +
                "VALUES (?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET " +
                "bank_level=excluded.bank_level, debt=excluded.debt, money=excluded.money";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, player.getUniqueId().toString());
            statement.setString(2, String.valueOf(level));
            statement.setString(3, debt);
            statement.setString(4, money);
            statement.executeUpdate();
        } catch (SQLException e) {
            BPLogger.Console.error(e, "Cannot save player " + player.getName() + " in bank " + bankName + ".");
        }
    }

    /**
     * Check if the specified player is registered in the given bank table (= bank name)
     * @param player The player to check.
     * @param bankName The table name.
     * @return true if a record with its uuid exists, false otherwise.
     */
    public static boolean isRegistered(OfflinePlayer player, String bankName) {
        String query = "SELECT 1 FROM " + bankName + " WHERE uuid=? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, player.getUniqueId().toString());
            try (ResultSet set = statement.executeQuery()) {
                return set.next();
            }
        } catch (SQLException e) {
            BPLogger.Console.error(e, "Cannot check if player " + player.getName() + " is registered in the bank " + bankName + ".");
            return false;
        }
    }

    /**
     * Fill all the different tables with the base values as if a
     * new player join, this method will only place the values
     * if they are not there, otherwise it won't update anything.
     *
     * @param player The player to register.
     */
    public static void fillRecords(OfflinePlayer player) {
        String playerName = player.getName() == null ? "" : player.getName();
        for (String bank : BPEconomy.nameList()) {
            String money = ConfigValues.getMainGuiName().equals(bank) ? ConfigValues.getStartAmount().toPlainString() : "0";
            String query;
            if (ConfigValues.isMySqlEnabled())
                query = "INSERT IGNORE INTO " + bank +
                        " (uuid, account_name, bank_level, money, interest, debt) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
            else query = "INSERT OR IGNORE INTO " + bank +
                    " (uuid, account_name, bank_level, money, interest, debt) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, player.getUniqueId().toString());
                statement.setString(2, playerName);
                statement.setString(3, "0");
                statement.setString(4, money);
                statement.setString(5, "0");
                statement.setString(6, "0");
                statement.executeUpdate();
            } catch (SQLException e) {
                BPLogger.Console.error(e, "Cannot insert base value in bank " + bank + " for player " + player.getName() + ".");
            }
        }
    }

    public static class MySQL {

        /**
         * Creates a new instance of the connection and assign it.
         * This method also enables the SQLMethods.
         */
        public static void connect() {
            if (connection != null) {
                BPLogger.Console.warn("MySQL is already connected.");
                return;
            }

            String host = ConfigValues.getMySqlHost();
            String port = ConfigValues.getMySqlPort();
            String database = ConfigValues.getMySqlDatabase();

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + ConfigValues.isMySqlUsingSSL();

            try {
                connection = DriverManager.getConnection(url, ConfigValues.getMySqlUsername(), ConfigValues.getMySqlPassword());

                // Create all the missing tables.
                for (String bankName : BPEconomy.nameList()) {
                    String query = "CREATE TABLE IF NOT EXISTS " + bankName + " (" + GET_TABLE_ARGUMENTS() + ")";
                    connection.prepareStatement(query).execute();
                }
            } catch (SQLException e) {
                BPLogger.Console.error(e, "Could not connect to MySQL database.");
            }
            BPLogger.Console.info("MySQL database successfully connected.");
        }
    }

    public static class SQLite {

        public static void connect() {
            if (connection != null) {
                BPLogger.Console.warn("SQLite is already connected.");
                return;
            }

            File dbFile = new File(BankPlus.INSTANCE().getDataFolder(), "data.db");

            if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();

            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            try {
                connection = DriverManager.getConnection(url);

                for (String bankName : BPEconomy.nameList()) {
                    String query = "CREATE TABLE IF NOT EXISTS " + bankName + " (" + GET_TABLE_ARGUMENTS() + ")";
                    connection.prepareStatement(query).execute();
                }
            } catch (SQLException e) {
                BPLogger.Console.error(e, "Could not connect to SQLite database.");
                return;
            }
            BPLogger.Console.info("SQLite successfully connected.");
        }
    }

    /**
     * Method to retrieve the specified value from the selected player in a particular bank.
     *
     * @param player   The player.
     * @param bankName The bank where to search the value.
     * @param search   The value type.
     * @return A string representing the bank level, debt, interest or money value.
     */
    private static String get(OfflinePlayer player, String bankName, SQLSearch search) {
        String columnName;
        switch (search) {
            case BANK_LEVEL -> columnName = "bank_level";
            case DEBT -> columnName = "debt";
            case INTEREST -> columnName = "interest";
            case MONEY -> columnName = "money";

            default -> {
                BPLogger.Console.warn("Invalid SQLSearch specified as parameter for query.");
                return "";
            }
        }

        String query = "SELECT " + columnName + " FROM " + bankName + " WHERE uuid=? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, player.getUniqueId().toString());

            try (ResultSet set = statement.executeQuery()) {
                if (!set.next()) return "";

                String value = set.getString(columnName);
                return value == null ? "" : value;
            }
        } catch (SQLException e) {
            BPLogger.Console.error(e, "Could not get data for player " + player.getName() + ".");
            return "";
        }
    }

    /**
     * Set or update a value for the specified player in the specified bank.
     * This method automatically creates a new record if missing.
     *
     * @param player   The player.
     * @param bankName The bank where to update the value.
     * @param search   The value to search.
     * @param newValue The new value.
     */
    private static void set(OfflinePlayer player, String bankName, SQLSearch search, String newValue) {
        String columnName;
        switch (search) {
            case BANK_LEVEL -> columnName = "bank_level";
            case DEBT -> columnName = "debt";
            case INTEREST -> columnName = "interest";
            case MONEY -> columnName = "money";

            default -> {
                BPLogger.Console.warn("Invalid SQLSearch specified as parameter for query.");
                return;
            }
        }

        String query;
        if (ConfigValues.isMySqlEnabled())
            query = "INSERT INTO " + bankName + " (uuid, " + columnName + ") VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE " + columnName + "=VALUES(" + columnName + ")";
        else query = "INSERT INTO " + bankName + " (uuid, " + columnName + ") VALUES (?, ?) " +
                "ON CONFLICT(uuid) DO UPDATE SET " + columnName + "=excluded." + columnName;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, player.getUniqueId().toString());
            statement.setString(2, newValue);
            statement.executeUpdate();
        } catch (SQLException e) {
            BPLogger.Console.error(e, "Cannot set " + columnName + " for player " + player.getName() + " in bank " + bankName + ".");
        }
    }
}