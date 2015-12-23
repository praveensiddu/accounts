package accounts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BankStatementFormat
{
    public static final String IGNORE_LINES_STARTSWITH = "ignore_lines_startswith";
    public static final String IGNORE_LINES_CONTAINS   = "ignore_lines_contains";

    public static final String DATE_COl          = "date_col";
    public static final String DESCRIPTION_COL   = "description_col";
    public static final String MEMO_COL          = "memo_col";
    public static final String AMOUNT_DEBIT_COL  = "amount_debit_col";
    public static final String AMOUNT_CREDIT_COL = "amount_credit_col";
    public static final String CHECK_NUMBER_COL  = "check_number_col";
    public static final String FEES_COL          = "fees_col";
    public static final String DATE_FORMAT       = "date_format";

    private List<String> ignLineStartsWith = new ArrayList<String>();

    private List<String> ignLineContains = new ArrayList<String>();

    private int    dateIndex    = -1;
    private int    descIndex    = -1;
    private int    memoIndex    = -1;
    private int    debitIndex   = -1;
    private int    creditIndex  = -1;
    private int    checkNoIndex = -1;
    private int    feesIndex    = -1;
    private String dateFormat;

    public List<String> getIgnLineStartsWith()
    {
        return ignLineStartsWith;
    }

    public void setIgnLineStartsWith(final List<String> ignLineStartsWith)
    {
        this.ignLineStartsWith = ignLineStartsWith;
    }

    public List<String> getIgnLineContains()
    {
        return ignLineContains;
    }

    public void setIgnLineContains(final List<String> ignLineContains)
    {
        this.ignLineContains = ignLineContains;
    }

    public BankStatementFormat(String bankName, String file) throws IOException
    {
        if (file == null)
        {
            if (bankName == null || bankName.isEmpty())
            {
                throw new IOException("Bank name is empty");
            }
            bankName = bankName.toLowerCase();
            if (System.getProperty("ACCOUNTSDB") == null)
            {
                throw new IOException(
                        "Set Java system property ACCOUNTSDB to directory where accounts repository is present or to be created");
            }
            file = System.getProperty("ACCOUNTSDB") + File.separator + "config" + File.separator + bankName
                    + "_statement_format.txt";
        }
        File f = new File(file);
        if (!f.exists())
        {
            throw new IOException("The file defining the bank statement format is missing at this location\n" + file);
        }
        final FileReader fr = new FileReader(file);
        final BufferedReader br = new BufferedReader(fr);
        try
        {
            for (String line; (line = br.readLine()) != null;)
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

                final String[] fields = line.split("=");
                if (fields.length != 2)
                {
                    throw new IOException("Invalid line=" + line);
                }
                String lCaseLine = line.toLowerCase();
                if (lCaseLine.startsWith(DATE_FORMAT))
                {
                    setDateFormat(fields[1].trim());
                } else if (lCaseLine.startsWith(IGNORE_LINES_STARTSWITH))
                {
                    ignLineStartsWith.add(fields[1].trim());
                } else if (lCaseLine.startsWith(IGNORE_LINES_CONTAINS))
                {
                    ignLineContains.add(fields[1].trim());
                } else if (lCaseLine.startsWith(DATE_COl))
                {
                    setDateIndex(new Integer(fields[1].trim()).intValue() - 1);
                } else if (lCaseLine.startsWith(DESCRIPTION_COL))
                {
                    setDescIndex(new Integer(fields[1].trim()).intValue() - 1);
                } else if (lCaseLine.startsWith(MEMO_COL))
                {
                    setMemoIndex(new Integer(fields[1].trim()).intValue() - 1);
                } else if (lCaseLine.startsWith(AMOUNT_DEBIT_COL))
                {
                    setDebitIndex(new Integer(fields[1].trim()).intValue() - 1);
                } else if (lCaseLine.startsWith(AMOUNT_CREDIT_COL))
                {
                    setCreditIndex(new Integer(fields[1].trim()).intValue() - 1);
                } else if (lCaseLine.startsWith(CHECK_NUMBER_COL))
                {
                    setCheckNoIndex(new Integer(fields[1].trim()).intValue() - 1);
                } else if (lCaseLine.startsWith(FEES_COL))
                {
                    setFeesIndex(new Integer(fields[1].trim()).intValue() - 1);
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
        }
    }

    public int getDateIndex()
    {
        return dateIndex;
    }

    public void setDateIndex(final int dateIndex)
    {
        this.dateIndex = dateIndex;
    }

    public int getDescIndex()
    {
        return descIndex;
    }

    public void setDescIndex(final int descIndex)
    {
        this.descIndex = descIndex;
    }

    public int getMemoIndex()
    {
        return memoIndex;
    }

    public void setMemoIndex(final int memoIndex)
    {
        this.memoIndex = memoIndex;
    }

    public int getDebitIndex()
    {
        return debitIndex;
    }

    public void setDebitIndex(final int debitIndex)
    {
        this.debitIndex = debitIndex;
    }

    public int getCreditIndex()
    {
        return creditIndex;
    }

    public void setCreditIndex(final int creditIndex)
    {
        this.creditIndex = creditIndex;
    }

    public int getCheckNoIndex()
    {
        return checkNoIndex;
    }

    public void setCheckNoIndex(final int checkNoIndex)
    {
        this.checkNoIndex = checkNoIndex;
    }

    public int getFeesIndex()
    {
        return feesIndex;
    }

    public void setFeesIndex(final int feesIndex)
    {
        this.feesIndex = feesIndex;
    }

    public String getDateFormat()
    {
        return dateFormat;
    }

    public void setDateFormat(final String dateFormat)
    {
        this.dateFormat = dateFormat;
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
            final BankStatementFormat bc = new BankStatementFormat("dcu", file);
        } catch (final IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
