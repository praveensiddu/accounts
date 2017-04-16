package accounts.db;

import java.io.IOException;
import java.util.Map;

public class DBFactory
{
    private static DBIfc dbi = null;

    public synchronized static DBIfc createDBIfc()
    {
        if (dbi == null)
        {
            dbi = new DBImpl();
        }
        return dbi;
    }

    public synchronized static DBIfc inst()
    {
        return dbi;
    }

    public static void main(String[] args)
    {
        DBIfc dbIfc = DBFactory.createDBIfc();
        try
        {
            dbIfc.createAndConnectDB();
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Map<String, BankAccount> accounts;
        try
        {
            accounts = dbi.getAccounts();
            for (BankAccount ba : accounts.values())
            {
                System.out.println(ba);
            }
            if (args.length < 2)
            {
                System.out.println("Usage: acname bankname");
                System.exit(-1);
            }
            dbIfc.createBankAccount(args[0], args[1]);
            for (BankAccount ba : accounts.values())
            {
                System.out.println(ba);
            }
        } catch (DBException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
