package accounts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AccountsUtil
{
    private static AccountsUtil inst_;

    public static AccountsUtil createInstance() throws IOException
    {
        if (inst_ == null)
        {
            inst_ = new AccountsUtil();
        }
        return inst_;
    }

    private AccountsUtil() throws IOException
    {
        if (System.getProperty("ACCOUNTSDB") == null)
        {
            throw new IOException(
                    "Set Java system property ACCOUNTSDB to directory where accounts repository is present or to be created");
        }
        readAllowedTypes(allowedTrTypes, "transaction_types.txt");
        readAllowedTypes(allowedTaxCategories, "tax_category.txt");
    }

    public static final int     COMMENT_MAXLEN     = 100;            // should be same as in DB.
    public static final int     OTHERENTITY_MAXLEN = 50;             // should be same as in DB.
    private Map<String, String> allowedTrTypes     = new HashMap<>();

    public Map<String, String> getAllowedTrTypes()
    {
        return allowedTrTypes;
    }

    public void setAllowedTrTypes(Map<String, String> allowedTrTypes)
    {
        this.allowedTrTypes = allowedTrTypes;
    }

    public Map<String, String> getAllowedTaxCategories()
    {
        return allowedTaxCategories;
    }

    public void setAllowedTaxCategories(Map<String, String> allowedTaxCategories)
    {
        this.allowedTaxCategories = allowedTaxCategories;
    }

    private Map<String, String> allowedTaxCategories = new HashMap<>();

    public static AccountsUtil inst()
    {
        return inst_;
    }

    public static String getConfigDir()
    {
        return System.getProperty("ACCOUNTS_DATA") + File.separator + "config";
    }

    private void readAllowedTypes(Map<String, String> map, String configFile) throws IOException
    {
        String file = getConfigDir() + File.separator + configFile;

        File f = new File(file);
        if (!f.exists())
        {
            throw new IOException("The file defining the transaction types is missing at this location" + file);
        }
        final FileReader fr = new FileReader(file);
        final BufferedReader br = new BufferedReader(fr);
        try
        {
            for (String line; (line = br.readLine()) != null;)
            {
                line = line.toLowerCase().trim();
                if (line.isEmpty())
                {
                    continue;
                }
                if (line.startsWith("#"))
                {
                    continue;
                }
                map.put(line, "");

            }
        } finally
        {
            if (br != null)
            {
                try
                {
                    br.close();
                } catch (final IOException e)
                {
                    // Ignore
                }
            }
        }
    }

    public static void main(final String[] args)
    {
        try
        {
            String file = null;
            if (args.length > 0)
            {
                file = args[0];
            }
            final AccountsUtil au = AccountsUtil.createInstance();
        } catch (final IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
