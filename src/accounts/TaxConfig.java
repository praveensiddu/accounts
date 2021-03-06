package accounts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TaxConfig
{
    public static final String                 BACCOUNT         = "baccount";
    public static final String                 TRANSACTION_TYPE = "transaction_type";
    public static final String                 APPEND_FILE      = "append_file";
    public static final String                 INCLUDE_FILE     = "include_file";
    public static final String                 DESC_CONTAINS    = "desc_contains";
    public static final String                 DESC_STARTSWITH  = "desc_startswith";
    public static final String                 DEBIT_EQUALS     = "debit_equals";
    public static final String                 CLOSERULE        = "closerule";

    public static final String                 TAX_CATEGORY     = "tax_category";
    public static final String                 PROPERTY         = "property";
    public static final String                 OTHERENTITY      = "otherentity";
    private File                               configFile       = null;

    private Map<String, ArrayList<RuleRecord>> accountsMap      = new HashMap<>();

    public static ArrayList<String> getLines(final String file) throws IOException
    {
        final FileReader fr = new FileReader(file);
        final BufferedReader br = new BufferedReader(fr);
        ArrayList<String> lines = new ArrayList<>();
        try
        {
            int lineno = 1;
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
                String[] fields = line.split("=", -1);
                if (fields.length != 2)
                {
                    throw new IOException(
                            "invalid format at file=" + file + " line=" + lineno + ", Expected format key=value. Found=" + line);
                }

                String key = fields[0].trim().toLowerCase();
                String valueAsIs = fields[1].trim();
                String value = fields[1].trim().toLowerCase();
                if (value.isEmpty())
                {
                    if (!PROPERTY.equals(key) && !OTHERENTITY.equals(key) && !TAX_CATEGORY.equals(key))
                    {
                        throw new IOException("invalid format at file=" + file + " line=" + lineno
                                + ", Expected format key=value. Found=" + line);
                    }
                }
                if (APPEND_FILE.equals(key))
                {
                    File valFile = new File(valueAsIs);
                    if (!valFile.isAbsolute())
                    {
                        valueAsIs = new File(file).getParent() + File.separator + valueAsIs;
                    }
                    lines.addAll(getLines(valueAsIs));
                } else
                {
                    lines.add(line);
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
        return lines;
    }

    public TaxConfig(final String file) throws IOException
    {
        configFile = new File(file);
        ArrayList<String> lines = getLines(file);

        try
        {
            String currentAccount = null;
            String currentProperty = null;
            String currOtherEntity = null;

            RuleRecord rr = new RuleRecord();
            rr.setLineno(1);

            for (Iterator<String> it = lines.iterator(); it.hasNext();)
            {
                String line = it.next().trim();
                if (line.isEmpty())
                {
                    continue;
                }
                if (line.startsWith("#"))
                {
                    continue;
                }
                String[] fields = line.split("=", -1);

                String key = fields[0].trim().toLowerCase();
                String valueAsIs = fields[1].trim();
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
                    currentProperty = "";
                    currOtherEntity = "";
                    // Empty rule. Only bank account name is copied
                    rr = rr.createFresh();
                    rr.setCurrentAccount(currentAccount);
                    continue;
                } else if (TRANSACTION_TYPE.equals(key))
                {
                    // if (currentProperty == null && currOtherEntity == null)
                    // throw new IOException("PROPERTY or OTHERENTITY should be set before TRANSACTION_TYPE");
                    // it is a new record. Create a new record
                    rr = rr.createNew();
                    rr.setProperty(currentProperty);
                    rr.setOtherEntity(currOtherEntity);
                    // rr.setLineno(lineno);

                    rr.setTrType(value);

                    ArrayList<RuleRecord> arr = accountsMap.get(currentAccount);
                    if (arr == null)
                    {
                        arr = new ArrayList<>();
                        accountsMap.put(currentAccount, arr);
                    }
                    arr.add(rr);
                } else if (CLOSERULE.equals(key))
                {
                    // Empty rule. Only bank account name is copied
                    currentProperty = "";
                    currOtherEntity = "";
                    rr = rr.createFresh();
                } else if (INCLUDE_FILE.equals(key))
                {
                    ArrayList<RuleRecord> arr = accountsMap.get(currentAccount);
                    if (arr == null)
                    {
                        arr = new ArrayList<>();
                        accountsMap.put(currentAccount, arr);
                    }
                    File valFile = new File(valueAsIs);
                    if (!valFile.isAbsolute())
                    {
                        valueAsIs = this.getConfigFile().getParent() + File.separator + valueAsIs;
                    }
                    TaxConfigInclude tcf = new TaxConfigInclude(valueAsIs);
                    for (RuleRecord rrinclude : tcf.getRuleRecordList())
                    {
                        rrinclude.setProperty(currentProperty);
                        rrinclude.setOtherEntity(currOtherEntity);
                        rrinclude.setTaxCategory(rr.getTaxCategory());
                        arr.add(rrinclude);
                    }

                } else if (DESC_CONTAINS.equals(key))
                {
                    rr.setDescContains(value);
                } else if (DESC_STARTSWITH.equals(key))
                {
                    rr.setDescStartsWith(value);
                } else if (DEBIT_EQUALS.equals(key))
                {
                    Float debitVal = new Float(value).floatValue();
                    rr.setDebitEquals(debitVal);
                } else if (TAX_CATEGORY.equals(key))
                {
                    rr.setTaxCategory(value);
                } else if (PROPERTY.equals(key))
                {
                    currentProperty = value;
                } else if (OTHERENTITY.equals(key))
                {
                    currOtherEntity = value;
                }

            }
        } finally
        {

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

    public File getConfigFile()
    {
        return configFile;
    }

}
