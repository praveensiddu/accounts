package accounts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import accounts.db.BankAccount;
import accounts.db.DBException;
import accounts.db.DBFactory;
import accounts.db.DBIfc;
import accounts.db.TR;
import accounts.db.TRId;

public class BankStatement
{
    private BankStatementFormat bc          = null;
    private BankAccount         bankAccount = null;
    private DBIfc               dbIfc;

    private Map<TRId, TR> trs = new HashMap<TRId, TR>();

    private final Map<String, TR> mkUniqDescMap = new HashMap<String, TR>();

    public BankStatement(final String filename, String accountName, final String bankStFormat) throws IOException, DBException
    {
        dbIfc = DBFactory.createDBIfc();

        if (accountName == null)
        {
            throw new IOException("Account name is null: " + accountName);
        }
        if (filename == null)
        {
            throw new IOException("Statement file name is null: " + filename);
        }
        accountName = accountName.toLowerCase().trim();
        dbIfc.createAndConnectDB(null);
        if (!dbIfc.getAccounts().containsKey(accountName))
        {
            throw new IOException("Account is not present: " + accountName + ", List=" + dbIfc.getAccounts().keySet());
        }
        bankAccount = dbIfc.getAccounts().get(accountName);
        if (bankAccount.getBankName() == null || bankAccount.getBankName().isEmpty())
        {
            throw new IOException(
                    "Bank name is not set for: " + accountName + ". Please delete and recreate the account with bank name");
        }
        bc = new BankStatementFormat(bankAccount.getBankName(), bankStFormat);

        final FileReader fr = new FileReader(filename);
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
                if (skipLine(line))
                {
                    continue;
                }
                TR tr = dbIfc.createCorrespondingTRObj(bankAccount);

                tr.init(line, bc);
                makeUnique(tr);
                trs.put(tr.getTrId(), tr);
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

    public Map<TRId, TR> getTrs()
    {
        return trs;
    }

    public void setTrs(final Map<TRId, TR> trs)
    {
        this.trs = trs;
    }

    private boolean skipLine(final String line)
    {
        for (final String s : bc.getIgnLineContains())
        {
            if (line.contains(s))
            {
                return true;
            }
        }
        for (final String s : bc.getIgnLineStartsWith())
        {
            if (line.startsWith(s))
            {
                return true;
            }
        }
        return false;
    }

    public void makeUnique(final TR tr)
    {
        String key2 = "" + tr.getDate();
        if (tr.getDescription() != null)
        {
            key2 += ", DESC=" + tr.getDescription();
        }
        if (mkUniqDescMap.containsKey(key2))
        {
            String key1 = key2;
            int i = 1;
            for (; mkUniqDescMap.containsKey(key1); i++)
            {
                key1 = key2 + ", MKUNIQ" + i;
            }
            tr.setDescription(tr.getDescription() + ", MKUNIQ" + i);
            mkUniqDescMap.put(key1, tr);
        } else
        {
            mkUniqDescMap.put(key2, tr);
        }

    }

    @Override
    public String toString()
    {
        return toString(false);
    }

    public static void trToString(final TR tr, final boolean nullOnly, StringBuffer sb)
    {
        if (nullOnly)
        {
            if (tr.getTaxCategory() != null)
            {
                return;
            }
        }
        sb.append(tr.toString() + "\n");
    }

    public String toString(final boolean nullOnly)
    {
        final StringBuffer sb = new StringBuffer();
        for (final TR tr : this.getTrs().values())
        {
            trToString(tr, nullOnly, sb);
        }
        return sb.toString();
    }

    public static void usage(final String err)
    {
        if (err != null && !"".equals(err))
        {
            System.out.println(err);
        }
        System.out.println("Usage: -A action options\n");
        System.out.println("    -A parse -bankstatement <csvfile> -accountname <n> [-bankstformat <file>] \n");
        System.exit(1);
    }

    public static final String              PARSE    = "parse";
    public static final Map<String, String> ALL_OPTS = new HashMap<String, String>();

    static
    {
        ALL_OPTS.put("A", Getopt.CONTRNT_S);
        ALL_OPTS.put("bankstatement", Getopt.CONTRNT_S);
        ALL_OPTS.put("bankstformat", Getopt.CONTRNT_S);
        ALL_OPTS.put("accountname", Getopt.CONTRNT_S);
    }

    public static void main(final String[] args)
    {

        final Map<String, String> argHash = Getopt.processCommandLineArgL(args, ALL_OPTS, true);
        if (argHash.get(Getopt.ERROR) != null)
        {
            usage(argHash.get(Getopt.ERROR));
        }

        final String action = argHash.get("A");
        try
        {
            if (PARSE.equalsIgnoreCase(action))
            {
                final BankStatement bs = new BankStatement(argHash.get("bankstatement"), argHash.get("accountname"),
                        argHash.get("bankstformat"));
                System.out.println("" + bs);

            } else
            {
                usage("Invalid action");
            }
        } catch (IOException | DBException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public BankAccount getBankAccount()
    {
        return bankAccount;
    }

    public void setBankAccount(BankAccount bankAccount)
    {
        this.bankAccount = bankAccount;
    }

}
