package accounts;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class DBImpl
{
    private static final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

    public Connection createDB(String dir, String dbname) throws MalformedURLException, SQLException
    {
        if (dbname == null)
        {
            dbname = "taxdb";
        }
        if (dir == null)
        {
            dir = "E:/temp/DERBYTUTOR/";
        } else if (!dir.endsWith("/"))
        {
            dir += "/";
        }
        dir += dbname;

        final String connectionURL = "jdbc:derby:" + dir + ";create=true";
        final Connection conn = DriverManager.getConnection(connectionURL);
        return conn;

    }

    public void createTransactionTable(final Connection con, final String acname) throws SQLException
    {
        final String tableName = "TRANSACTIONS_" + acname;

        if (!tableExists(con, tableName))
        {
            final Statement s = con.createStatement();
            final String createString = "CREATE TABLE  "
                    + tableName
                    + " (DATE DATE NOT NULL, DESCRIPTION VARCHAR(100) NOT NULL, AMOUNT_DEBIT FLOAT NOT NULL, Annotation VARCHAR(50), TransactionType VARCHAR(20), TaxCategory VARCHAR(20), Property VARCHAR(20), PRIMARY KEY (DATE, DESCRIPTION,AMOUNT_DEBIT))";

            s.execute(createString);
            s.close();
        } else
        {
            final Statement s = con.createStatement();
            final String createString = "DROP TABLE  " + tableName;

            s.execute(createString);
            s.close();
        }
    }

    public void importBankStatement(final Connection con, final BankStatement bs) throws IOException, SQLException
    {

        if (!accountExists(con, bs.getAccountName()))
        {
            throw new IOException("Account not present=" + bs.getAccountName());
        }
        final String tableName = "TRANSACTIONS_" + bs.getAccountName();
        if (!tableExists(con, tableName))
        {// This cannot happen in practice
            throw new IOException("Transaction table not present for account=" + bs.getAccountName());

        }

        for (final TR tr : bs.getTrs())
        {

            final PreparedStatement psInsert = con.prepareStatement("insert into " + tableName
                    + " (DATE, DESCRIPTION, AMOUNT_DEBIT) " + "VALUES ( " + "?, ?, ?" + ")");

            final java.sql.Date sqlDate = new java.sql.Date(tr.getDate().getTime());
            psInsert.setDate(1, sqlDate);
            psInsert.setString(2, tr.getDesc());
            psInsert.setFloat(3, tr.getDebit());

            try
            {
                psInsert.executeUpdate();
            } catch (final SQLIntegrityConstraintViolationException ex)
            {
                throw new IOException("constraint violation" + tr.getDesc());
            }
            psInsert.close();

        }

    }

    public boolean tableExists(final Connection con, final String tablename) throws SQLException
    {
        final DatabaseMetaData meta = con.getMetaData();
        final ResultSet res = meta.getTables(null, null, tablename.toUpperCase(), new String[] { "TABLE" });
        if (!res.next())
        {
            res.close();
            return false;
        }
        res.close();
        return true;
    }

    public void createRealPropertyTable(final Connection con) throws SQLException
    {

        final DatabaseMetaData meta = con.getMetaData();
        final ResultSet res = meta.getTables(null, null, "REALPROPERTY", new String[] { "TABLE" });

        if (!res.next())
        {
            final Statement s = con.createStatement();
            final String createString = "CREATE TABLE REALPROPERTY  "
                    + "(NAME VARCHAR(32) NOT NULL CONSTRAINT PROPERTYNAME_PK PRIMARY KEY, IMPROVEMENT INT, LAND INT, RENOVATION INT, loanclosingcost INT) ";

            s.execute(createString);
            s.close();
        } else
        {

            do
            {

                System.out.println("   " + res.getString("TABLE_CAT") + ", " + res.getString("TABLE_SCHEM") + ", "
                        + res.getString("TABLE_NAME") + ", " + res.getString("TABLE_TYPE") + ", " + res.getString("REMARKS"));
            } while (res.next());
        }
        res.close();
    }

    public void createAccountsTable(final Connection con) throws SQLException
    {

        final DatabaseMetaData meta = con.getMetaData();
        final ResultSet res = meta.getTables(null, null, "BACCOUNTS", new String[] { "TABLE" });

        if (!res.next())
        {
            final Statement s = con.createStatement();
            final String createString = "CREATE TABLE BACCOUNTS  "
                    + "(ACCOUNT_NAME VARCHAR(32) NOT NULL CONSTRAINT BACCOUNTS_PK PRIMARY KEY) ";
            s.execute(createString);
            s.close();
        } else
        {

            do
            {

                System.out.println("   " + res.getString("TABLE_CAT") + ", " + res.getString("TABLE_SCHEM") + ", "
                        + res.getString("TABLE_NAME") + ", " + res.getString("TABLE_TYPE") + ", " + res.getString("REMARKS"));
            } while (res.next());
        }
        res.close();
    }

    public boolean accountExists(final Connection con, String acname) throws SQLException, IOException
    {
        if (acname == null)
        {
            throw new IOException("Account name is null");
        }
        acname = acname.trim().toLowerCase();
        if (acname.isEmpty())
        {
            throw new IOException("Account name is null");
        }
        final PreparedStatement psQuery = con.prepareStatement("select ACCOUNT_NAME from BACCOUNTS where ACCOUNT_NAME = (?)");
        psQuery.setString(1, acname);

        final ResultSet accounts = psQuery.executeQuery();

        if (!accounts.next())
        {
            psQuery.close();
            accounts.close();
            return false;
        }
        psQuery.close();
        accounts.close();
        return true;
    }

    public void listAccounts(final Connection con) throws SQLException
    {
        final Statement s = con.createStatement();
        final ResultSet accounts = s.executeQuery("select ACCOUNT_NAME from BACCOUNTS order by ACCOUNT_NAME");

        while (accounts.next())
        {
            System.out.println(" Name " + accounts.getString(1));
        }
        // Close the resultSet
        accounts.close();
        s.close();
    }

    public void createAccountsName(final Connection con, String acname) throws SQLException, IOException
    {
        if (acname == null)
        {
            throw new IOException("Account name is null");
        }
        acname = acname.trim().toLowerCase();
        if (acname.isEmpty())
        {
            throw new IOException("Account name is null");
        }

        final PreparedStatement psInsert = con.prepareStatement("insert into BACCOUNTS(ACCOUNT_NAME) values (?)");
        psInsert.setString(1, acname);
        try
        {
            psInsert.executeUpdate();
        } catch (final SQLIntegrityConstraintViolationException ex)
        {
            throw new IOException("constraint violation" + acname);
        }
        psInsert.close();
        createTransactionTable(con, acname);
        listAccounts(con);

    }

    public static void usage(final String err)
    {
        if (err != null && !"".equals(err))
        {
            System.out.println(err);
        }
        System.out.println("Usage: -A action options\n");
        System.exit(1);
    }

    public static final String              CREATEDB      = "createdb";
    public static final String              ACCOUNTSTABLE = "accountstable";
    public static final String              CREATEAC      = "createac";
    public static final String              CREATETR      = "createtr";
    public static final String              CREATEPROP    = "createprop";
    public static final Map<String, String> ALL_OPTS      = new HashMap<String, String>();
    static
    {
        ALL_OPTS.put("A", Getopt.CONTRNT_S);
        ALL_OPTS.put("name", Getopt.CONTRNT_S);
    }

    public static void main(final String[] args)
    {
        final Map<String, String> argHash = Getopt.processCommandLineArgL(args, ALL_OPTS, true);
        if (argHash.get(Getopt.ERROR) != null)
        {
            usage(argHash.get(Getopt.ERROR));
        }
        System.out.println("option A=" + argHash.get("A"));
        final String action = argHash.get("A");
        final DBImpl dbi = new DBImpl();
        if (CREATEDB.equalsIgnoreCase(action))
        {
            try
            {
                final Connection conn = dbi.createDB(null, null);
                conn.close();
            } catch (MalformedURLException | SQLException e)
            {
                e.printStackTrace();
            }
        } else if (ACCOUNTSTABLE.equalsIgnoreCase(action))
        {
            try
            {
                final Connection conn = dbi.createDB(null, null);
                dbi.createAccountsTable(conn);
                conn.close();
            } catch (MalformedURLException | SQLException e)
            {
                e.printStackTrace();
            }
        } else if (CREATEAC.equalsIgnoreCase(action))
        {
            try
            {
                final Connection conn = dbi.createDB(null, null);
                final String acname = argHash.get("name");
                dbi.createAccountsName(conn, acname);
                conn.close();
            } catch (SQLException | IOException e)
            {
                e.printStackTrace();
            }
        } else if (CREATETR.equalsIgnoreCase(action))
        {
            try
            {
                final Connection conn = dbi.createDB(null, null);
                final String acname = argHash.get("name");
                dbi.createTransactionTable(conn, acname);
                conn.close();
            } catch (SQLException | IOException e)
            {
                e.printStackTrace();
            }
        } else if (CREATEPROP.equalsIgnoreCase(action))
        {
            try
            {
                final Connection conn = dbi.createDB(null, null);
                dbi.createRealPropertyTable(conn);
                conn.close();
            } catch (SQLException | IOException e)
            {
                e.printStackTrace();
            }
        } else
        {
            usage("Invalid action -A");
        }
        if (DRIVER.equals("org.apache.derby.jdbc.EmbeddedDriver"))
        {
            boolean gotSQLExc = false;
            try
            {
                DriverManager.getConnection("jdbc:derby:;shutdown=true");
            } catch (final SQLException se)
            {
                if (se.getSQLState().equals("XJ015"))
                {
                    gotSQLExc = true;
                }
            }
            if (!gotSQLExc)
            {
                System.out.println("Database did not shut down normally");
            } else
            {
                System.out.println("Database shut down normally");
            }
        }

    }

}
