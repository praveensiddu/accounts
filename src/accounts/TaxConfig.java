package accounts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TaxConfig
{
    public static final String                 BACCOUNT         = "baccount";
    public static final String                 TRANSACTION_TYPE = "transaction_type";
    public static final String                 INCLUDE_FILE     = "include_file";
    public static final String                 DESC_CONTAINS    = "desc_contains";
    public static final String                 DESC_STARTSWITH  = "desc_startswith";

    public static final String                 TAX_CATEGORY     = "tax_category";
    public static final String                 PROPERTY         = "property";

    public static final String                 RENTAL           = "rental";
    public static final String                 PERSONAL         = "personal";
    public static final String                 REALESTATEAGENT  = "realestateagent";
    private Map<String, ArrayList<RuleRecord>> accountsMap      = new HashMap<String, ArrayList<RuleRecord>>();

    public TaxConfig(final String file) throws IOException
    {
        final FileReader fr = new FileReader(file);
        final BufferedReader br = new BufferedReader(fr);
        try
        {
            String currentAccount = null;
            String currentProperty = null;

            int lineno = 1;
            RuleRecord rr = new RuleRecord();
            rr.setLineno(1);
            for (String line; (line = br.readLine()) != null; lineno++)
            {
                line = line.trim();
                if (line.isEmpty())
                {
                    continue;
                }
                if (line.startsWith("#"))
                {
                    continue;
                }
                String[] fields = line.split("=");
                if (fields.length != 2)
                {
                    throw new IOException("invalid format at line=" + lineno + ", Expected format key=value. Found=" + line);
                }

                String key = fields[0].trim().toLowerCase();
                String value = fields[1].trim().toLowerCase();
                if (currentAccount == null)
                {
                    if (!BACCOUNT.equals(key))
                    {
                        throw new IOException("First entry in tax config should be baccount");
                    }
                }

                if (BACCOUNT.equals(key))
                {
                    currentAccount = value;
                    continue;
                } else if (TRANSACTION_TYPE.equals(key))
                {
                    if (currentProperty == null)
                        throw new IOException("PROPERTY should be set before TRANSACTION_TYPE");
                    // it is a new record. Create a new record
                    rr = rr.createNew();
                    rr.setProperty(currentProperty);
                    rr.setLineno(lineno);

                    rr.setTrType(value);

                    ArrayList<RuleRecord> arr = accountsMap.get(currentAccount);
                    if (arr == null)
                    {
                        arr = new ArrayList<RuleRecord>();
                        accountsMap.put(currentAccount, arr);
                    }
                    arr.add(rr);
                } else if (INCLUDE_FILE.equals(key))
                {

                    ArrayList<RuleRecord> arr = accountsMap.get(currentAccount);
                    if (arr == null)
                    {
                        arr = new ArrayList<RuleRecord>();
                        accountsMap.put(currentAccount, arr);
                    }
                    TaxConfigInclude tcf = new TaxConfigInclude(value);
                    for (RuleRecord rrinclude : tcf.getRuleRecordList())
                    {
                        rrinclude.setProperty(rr.getProperty());
                        arr.add(rrinclude);
                    }

                } else if (DESC_CONTAINS.equals(key))
                {
                    rr.setDescContains(value);
                } else if (DESC_STARTSWITH.equals(key))
                {
                    rr.setDescStartsWith(value);
                } else if (TAX_CATEGORY.equals(key))
                {
                    rr.setTaxCategory(value);
                } else if (PROPERTY.equals(key))
                {
                    currentProperty = value;
                }

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
            if (fr != null)
            {
                try
                {
                    fr.close();
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
            final TaxConfig bc = new TaxConfig(args[0]);
            for (final String key : bc.accountsMap.keySet())
            {
                System.out.println("Account=" + key);
                for (final RuleRecord rr : bc.accountsMap.get(key))
                {
                    System.out.println(rr);
                }
            }
        } catch (final IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public Map<String, ArrayList<RuleRecord>> getAccountsMap()
    {
        return accountsMap;
    }

    public void setAccountsMap(final Map<String, ArrayList<RuleRecord>> accountsMap)
    {
        this.accountsMap = accountsMap;
    }

}
