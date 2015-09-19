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

    public static void main(String[] args)
    {
        DBIfc dbIfc = DBFactory.createDBIfc();
        try
        {
            dbIfc.createAndConnectDB("E:/temp/DERBYTUTOR/");
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
            dbIfc.createBankAccount(args[0]);
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
