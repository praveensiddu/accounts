package accounts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import accounts.db.BankAccount;
import accounts.db.DBException;
import accounts.db.DBFactory;
import accounts.db.DBIfc;
import accounts.db.DBImpl;
import accounts.db.IGroup;
import accounts.db.RealProperty;
import accounts.db.TR;
import accounts.db.TRId;

public class AccountsMainApp
{
    private BankAccount bankAccount = null;

    private static Map<String, Float> trTypeTotal(final ArrayList<TR> art)
    {
        final Map<String, Float> map = new HashMap<String, Float>();
        for (final TR tr : art)
        {
            if (!map.containsKey(tr.getIncomeType()))
            {
                map.put(tr.getIncomeType(), tr.getDebit());
                continue;
            }
            final Float f = map.get(tr.getIncomeType()) + tr.getDebit();
            map.put(tr.getIncomeType(), f);
        }
        return map;

    }

    private static StringBuffer report(final int year, final DBIfc dbIfc) throws DBException
    {
        final Map<String, ArrayList<TR>> propTrMap = new HashMap<String, ArrayList<TR>>();
        final Map<String, ArrayList<TR>> grpTrMap = new HashMap<String, ArrayList<TR>>();
        Map<String, BankAccount> acctMap = dbIfc.getAccounts();
        Map<String, IGroup> groupsMap = dbIfc.getGropusMap();

        // Traverse all bank accounts and all transactions
        for (BankAccount ba : acctMap.values())
        {
            Map<TRId, TR> trMap = dbIfc.getTransactions(ba.getTrTableId());
            // Group the transactions for each property and group
            addToPropertyMap(year, trMap, groupsMap, propTrMap, grpTrMap);
        }
        Map<String, RealProperty> propertyMap = dbIfc.getProperties();
        return reportFromPropMap(propTrMap, grpTrMap, propertyMap, groupsMap);
    }

    private static void addToPropertyMap(final int year, final Map<TRId, TR> trMap, final Map<String, IGroup> groupsMap,
                                         Map<String, ArrayList<TR>> propTrMap, Map<String, ArrayList<TR>> grpTrMap)
    {
        for (final TR tr : trMap.values())
        {
            final Calendar cal = Calendar.getInstance();
            cal.setTime(tr.getDate());

            if (cal.get(Calendar.YEAR) != year)
            {
                continue;
            }
            if (propTrMap.containsKey(tr.getProperty()))
            {
                final ArrayList<TR> arrTr = propTrMap.get(tr.getProperty());
                arrTr.add(tr);
            } else if (grpTrMap.containsKey(tr.getProperty()))
            {
                final ArrayList<TR> arrTr = grpTrMap.get(tr.getProperty());
                arrTr.add(tr);
            } else
            {
                if (groupsMap.containsKey(tr.getProperty()))
                {
                    if (!grpTrMap.containsKey(tr.getProperty()))
                    {
                        final ArrayList<TR> arrTr = new ArrayList<TR>();
                        arrTr.add(tr);
                        grpTrMap.put(tr.getProperty(), arrTr);
                        continue;
                    } else
                    {

                        final ArrayList<TR> arrTr = grpTrMap.get(tr.getProperty());
                        arrTr.add(tr);
                    }
                } else
                {
                    if (!propTrMap.containsKey(tr.getProperty()))
                    {
                        final ArrayList<TR> arrTr = new ArrayList<TR>();
                        arrTr.add(tr);
                        if (tr.getProperty() == null)
                        {// TODO should we allow this?
                            propTrMap.put("null", arrTr);
                        } else
                        {
                            propTrMap.put(tr.getProperty(), arrTr);

                        }
                        continue;
                    } else
                    {

                        final ArrayList<TR> arrTr = propTrMap.get(tr.getProperty());
                        arrTr.add(tr);
                    }
                }
            }
        }
    }

    public static final String               RENT             = "rent";
    public static final String               COMMISSIONS      = "commissions";
    public static final String               INSURANCE        = "insurance";
    public static final String               PROFESSIONALFEES = "professionalfees";
    public static final String               MORTGAGEINTEREST = "mortgageinterest";
    public static final String               REPAIRS          = "repairs";
    public static final String               TAX              = "tax";
    public static final String               UTILITIES        = "utilities";
    public static final String               DEPRECIATION     = "depreciation";
    public static final String               HOA              = "hoa";
    public static final String               PROFIT           = "profit";
    public static final String               OTHER            = "other";
    public static final Map<String, Integer> scheduleEMap;
    public static final String[]             scheduleEAry     = { null, null, null, RENT, null, null, null, null, COMMISSIONS,
            INSURANCE, PROFESSIONALFEES, null, MORTGAGEINTEREST, null, REPAIRS, null, TAX, UTILITIES, DEPRECIATION, HOA, OTHER };
    static
    {
        scheduleEMap = new HashMap<String, Integer>();
        scheduleEMap.put(RENT, 3);
        scheduleEMap.put(COMMISSIONS, 8);
        scheduleEMap.put(INSURANCE, 9);
        scheduleEMap.put(PROFESSIONALFEES, 10);
        scheduleEMap.put(MORTGAGEINTEREST, 12);
        scheduleEMap.put(REPAIRS, 14);
        scheduleEMap.put(TAX, 16);
        scheduleEMap.put(UTILITIES, 17);
        scheduleEMap.put(DEPRECIATION, 18);
        scheduleEMap.put(HOA, 19);
        scheduleEMap.put(OTHER, 20);

    }

    private static StringBuffer reportFromPropMap(final Map<String, ArrayList<TR>> propTrMap,
                                                  final Map<String, ArrayList<TR>> groupTrMap,
                                                  final Map<String, RealProperty> propertyMap, final Map<String, IGroup> groupsMap)
    {
        Map<String, Map<String, Float>> propTrTypeTotalMap = new HashMap<String, Map<String, Float>>();

        for (final String propName : propTrMap.keySet())
        {
            final Map<String, Float> trTypeMap = trTypeTotal(propTrMap.get(propName));
            propTrTypeTotalMap.put(propName, trTypeMap);
        }
        // Start assigning from group to individual
        for (final String grpName : groupsMap.keySet())
        {
            if (!groupTrMap.containsKey(grpName))
            {
                // THere is no transaction in any of the bank accounts assigned
                // to this group
                continue;
            }
            final Map<String, Float> grpTrTypeMap = trTypeTotal(groupTrMap.get(grpName));
            IGroup group = groupsMap.get(grpName);
            if (group.getMembers() == null)
                continue; // group not created properly
            int size = group.getMembers().size();
            for (String propName : group.getMembers())
            {
                Map<String, Float> trTypeMap = propTrTypeTotalMap.get(propName);
                if (trTypeMap == null)
                {
                    trTypeMap = new HashMap<String, Float>();
                    propTrTypeTotalMap.put(propName, trTypeMap);
                }
                for (String category : grpTrTypeMap.keySet())
                {
                    Float f = trTypeMap.get(category);
                    if (f == null)
                    {
                        f = (float) 0;
                    }
                    trTypeMap.put(category, f + (grpTrTypeMap.get(category) / size));
                }
            }
        }
        List<String> listProps = new ArrayList<String>(propTrMap.keySet());
        Collections.sort(listProps);
        // For each property first calculate the totals in each category and
        // then preprare the report
        StringBuffer sb = new StringBuffer();
        for (final String propName : listProps)
        {
            sb.append("\nSchedule E for Property=" + propName + "\n");
            final Map<String, Float> trTypeMap = propTrTypeTotalMap.get(propName);
            int ownerCount = 1;
            RealProperty rp = null;
            if (propertyMap.containsKey(propName))
            {
                rp = propertyMap.get(propName);
                ownerCount = rp.getOwnerCount();
            }
            float totalExpense = 0;
            // Ok to hardcode 18 since tax row numbers do not change
            for (int i = 1; i <= 18; i++)
            {
                if (scheduleEAry[i] == null)
                    continue;
                if (trTypeMap.containsKey(scheduleEAry[i]))
                {
                    if (RENT.equalsIgnoreCase(scheduleEAry[i]))
                    {
                        sb.append("    " + i + " " + scheduleEAry[i] + "="
                                + (trTypeMap.get(scheduleEAry[i]) * 94 / 100 / ownerCount) + "\n");

                    } else
                    {
                        totalExpense += (trTypeMap.get(scheduleEAry[i]) / ownerCount);
                        sb.append("    " + i + " " + scheduleEAry[i] + "=" + (trTypeMap.get(scheduleEAry[i]) / ownerCount) + "\n");
                    }
                }
            }

            if (rp != null)
            {
                int costPlusReno = rp.getCost() + rp.getRenovation();
                double depreciation = (costPlusReno * 3.64 / 100);
                sb.append("    18 depreciation" + "=" + (-(depreciation / ownerCount)) + "\n");
                totalExpense += (-(depreciation / ownerCount));

            }
            float other = 0;

            if (trTypeMap.containsKey("bankfees"))
            {
                // include everything else in the other category
                other = (trTypeMap.get("bankfees") / ownerCount);
                totalExpense += (trTypeMap.get("bankfees") / ownerCount);
            }
            if (trTypeMap.containsKey(HOA))
            {
                other += (trTypeMap.get(HOA) / ownerCount);
                totalExpense += (trTypeMap.get(HOA) / ownerCount);
            }
            if (rp != null)
            {
                if (rp.getLoanClosingCost() > 0)
                {
                    float closingDepreciation = (float) (-(rp.getLoanClosingCost() * 6.67 / 100 / ownerCount));
                    totalExpense += (closingDepreciation);
                    other += (closingDepreciation);
                }
            }
            sb.append("    19 other(hoa +bank fees+loan closing)=" + other + "\n");

            sb.append("    20 total expense" + "=" + (totalExpense) + "\n");
            StringBuffer sbUnown = new StringBuffer();

            for (final String key1 : trTypeMap.keySet())
            {
                if (!scheduleEMap.containsKey(key1) && !"bankfees".equals(key1))
                {
                    sbUnown.append("        " + key1 + "=" + trTypeMap.get(key1) + "\n");
                }
            }
            if (sbUnown.length() > 0)
            {
                sb.append("    Unknown items:\n");
                sb.append(sbUnown);

            }
            sb.append("");
        }
        return sb;
    }

    private static StringBuffer report(final int year, final Map<TRId, TR> trMap)
    {
        final Map<String, ArrayList<TR>> propTrMap = new HashMap<String, ArrayList<TR>>();
        final Map<String, ArrayList<TR>> grpTrMap = new HashMap<String, ArrayList<TR>>();
        Map<String, IGroup> dummyGrpMap = new HashMap<String, IGroup>();
        addToPropertyMap(year, trMap, dummyGrpMap, propTrMap, grpTrMap);
        return reportFromPropMap(propTrMap, grpTrMap, null, null);

    }

    private static void importtr(String file, String accountName) throws DBException, IOException
    {
        DBIfc dbIfc = DBFactory.createDBIfc();
        dbIfc.createAndConnectDB(null);

        if (accountName == null)
        {
            throw new IOException("Account is not present: " + accountName);
        }
        accountName = accountName.toLowerCase().trim();
        if (!dbIfc.getAccounts().containsKey(accountName))
        {
            throw new IOException("Account is not present: " + accountName + ", List=" + dbIfc.getAccounts().keySet());
        }
        BankAccount bankAccount = dbIfc.getAccounts().get(accountName);

        final FileReader fr = new FileReader(file);
        final BufferedReader br = new BufferedReader(fr);
        try
        {
            Map<TRId, TR> trs = new HashMap<TRId, TR>();
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
                TR tr = dbIfc.createCorrespondingTRObj(bankAccount);

                tr.importLine(line);
                // TODO user may have messed up the description.
                trs.put(tr.getTrId(), tr);
            }
            int count = dbIfc.updateTransactions(trs);
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

    private static void exporttr(String file, String accountName) throws DBException, IOException
    {
        DBIfc dbIfc = DBFactory.createDBIfc();

        dbIfc.createAndConnectDB(null);

        if (accountName == null)
        {
            throw new IOException("Account is not present: " + accountName);
        }
        accountName = accountName.toLowerCase().trim();
        if (!dbIfc.getAccounts().containsKey(accountName))
        {
            throw new IOException("Account is not present: " + accountName + ", List=" + dbIfc.getAccounts().keySet());
        }
        BankAccount ba = dbIfc.getAccounts().get(accountName);
        Map<TRId, TR> trMap = dbIfc.getTransactions(ba.getTrTableId());
        StringBuffer sb = new StringBuffer();
        sb.append("#DATE,DESCRIPTION,DEBIT,COMMENT,ISLOCKED,INCOMETYPE,TAXCATEGORY,PROPERTY\n");
        for (final TR tr : trMap.values())
        {
            sb.append(new SimpleDateFormat("MM-dd-yyyy").format(tr.getDate()) + "," + tr.getDescription() + "," + tr.getDebit()
                    + "," + tr.getComment() + "," + tr.isLocked() + "," + tr.getIncomeType() + "," + tr.getTaxCategory() + ","
                    + tr.getProperty() + "\n");
        }
        PrintWriter out = new PrintWriter(file);
        out.println(sb);
        out.close();
    }

    private static DBIfc classifyindb(final TaxConfig tc) throws DBException, IOException
    {
        DBIfc dbIfc = DBFactory.createDBIfc();

        dbIfc.createAndConnectDB(null);
        for (BankAccount ba : dbIfc.getAccounts().values())
        {
            Map<TRId, TR> trMap = dbIfc.getTransactions(ba.getTrTableId());

            final ArrayList<RuleRecord> arr = tc.getAccountsMap().get(ba.getName());
            for (final TR tr : trMap.values())
            {
                for (final RuleRecord rr : arr)
                {
                    if (rr.getDescContains() != null)
                    {
                        if (tr.getDescription().contains(rr.getDescContains()))
                        {
                            tr.setProperty(rr.getProperty());
                            tr.setTaxCategory(rr.getTaxCategory());
                            tr.setIncomeType(rr.getTrType());
                            break;
                        }
                    } else if (rr.getDescStartsWith() != null)
                    {
                        if (tr.getDescription().startsWith(rr.getDescStartsWith()))
                        {
                            tr.setProperty(rr.getProperty());
                            tr.setTaxCategory(rr.getTaxCategory());
                            tr.setIncomeType(rr.getTrType());
                            break;
                        }
                    }
                }
            }
            dbIfc.updateTransactions(trMap);
        }
        return dbIfc;
    }

    private static void classify(final BankStatement bs, final TaxConfig tc)
    {
        for (final String accountname : tc.getAccountsMap().keySet())
        {

            if (!bs.getBankAccount().getName().equals(accountname))
            {
                continue;
            }
            final ArrayList<RuleRecord> arr = tc.getAccountsMap().get(accountname);
            for (final TR tr : bs.getTrs().values())
            {
                for (final RuleRecord rr : arr)
                {
                    if (rr.getDescContains() != null)
                    {
                        if (tr.getDescription().contains(rr.getDescContains()))
                        {
                            tr.setProperty(rr.getProperty());
                            tr.setTaxCategory(rr.getTaxCategory());
                            tr.setIncomeType(rr.getTrType());
                            break;
                        }
                    } else if (rr.getDescStartsWith() != null)
                    {
                        if (tr.getDescription().startsWith(rr.getDescStartsWith()))
                        {
                            tr.setProperty(rr.getProperty());
                            tr.setTaxCategory(rr.getTaxCategory());
                            tr.setIncomeType(rr.getTrType());
                            break;
                        }
                    }
                }

            }
        }

    }

    public static void commit(BankStatement bs) throws DBException
    {
        DBIfc dbIfc = DBFactory.createDBIfc();
        int count = dbIfc.updateTransactions(bs.getTrs());
        System.out.println("updated transaction count=" + count);
    }

    public static void usage(final String err)
    {
        if (err != null && !"".equals(err))
        {
            System.out.println(err);
        }
        System.out.println("Usage: -A action options\n");
        System.out.println("    -A createacs -file <f>");
        System.out.println("    -A deleteacs -file <f>\n");
        System.out.println("    -A createprops -file <f>");
        System.out.println("    -A DELETEPROPS -file <f>\n");
        System.out.println("    -A creategroups -file <f>");
        System.out.println("    -A deletegroups -file <f>\n");
        System.out.println("    -A parse -bankstatement <csvfile> -bankconfig <file> -accountname <n>\n");
        System.out.println("    -A parseandclassify -bankstatement <csvfile> -bankconfig <f> -accountname <n> -taxconfig <f>\n");
        System.out.println("    -A import2db -bankstatement <csvfile> -bankconfig <f> -accountname <n>\n");
        System.out.println("    -A classifyindb -taxconfig <f> -year <yyyy>\n");
        System.out.println("    -A exporttr -accountname <n> -file <f.csv>\n");
        System.out.println("    -A importtr -accountname <n> -file <f.csv>\n");
        System.exit(1);
    }

    public static final String              PARSE            = "parse";
    public static final String              IMPORT2DB        = "import2db";
    public static final String              PARSEANDCLASSIFY = "parseandclassify";
    public static final String              CLASSIFYINDB     = "classifyindb";
    public static final String              EXPORTTR         = "exporttr";
    public static final String              IMPORTTR         = "importtr";

    public static final String              CREATEACS        = "createacs";
    public static final String              DELETEACS        = "deleteacs";
    public static final String              CREATEPROPS      = "createprops";
    public static final String              DELETEPROPS      = "deleteprops";
    public static final String              CREATEGROUPS     = "creategroups";
    public static final String              DELETEGROUPS     = "deletegroups";
    public static final Map<String, String> ALL_OPTS         = new HashMap<String, String>();
    static
    {
        ALL_OPTS.put("A", Getopt.CONTRNT_S);
        ALL_OPTS.put("bankstatement", Getopt.CONTRNT_S);
        ALL_OPTS.put("bankconfig", Getopt.CONTRNT_S);
        ALL_OPTS.put("accountname", Getopt.CONTRNT_S);
        ALL_OPTS.put("taxconfig", Getopt.CONTRNT_S);
        ALL_OPTS.put("year", Getopt.CONTRNT_I);
        ALL_OPTS.put("file", Getopt.CONTRNT_S);
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
            if (CREATEACS.equalsIgnoreCase(action))
            {
                if (argHash.get("file") == null)
                {
                    usage("-file argument is required.");
                }
                List<String> acL = DBImpl.parseAccountFile(argHash.get("file"));
                DBIfc dbi = DBFactory.createDBIfc();
                dbi.createAndConnectDB(null);
                for (String ac : acL)
                {
                    if (dbi.getAccounts() != null && dbi.getAccounts().containsKey(ac))
                    {
                        System.out.println("Account is already present=" + ac);
                    } else
                    {
                        dbi.createBankAccount(ac);
                    }
                }
                for (BankAccount ba : dbi.getAccounts().values())
                {
                    System.out.println(ba);
                }
            } else if (DELETEACS.equalsIgnoreCase(action))
            {
                if (argHash.get("file") == null)
                {
                    usage("-file argument is required.");
                }
                List<String> acL = DBImpl.parseAccountFile(argHash.get("file"));
                DBIfc dbi = DBFactory.createDBIfc();
                dbi.createAndConnectDB(null);
                for (String ac : acL)
                {
                    dbi.deleteBankAccount(ac);
                }
                for (BankAccount ba : dbi.getAccounts().values())
                {
                    System.out.println(ba);
                }
            } else if (CREATEPROPS.equalsIgnoreCase(action))
            {
                if (argHash.get("file") == null)
                {
                    usage("-file argument is required.");
                }
                List<RealProperty> rpL = DBImpl.parsePropFile(argHash.get("file"));
                DBIfc dbi = DBFactory.createDBIfc();
                dbi.createAndConnectDB(null);
                for (RealProperty rp : rpL)
                {
                    dbi.createProperty(rp);
                }
                for (RealProperty rp1 : dbi.getProperties().values())
                {
                    System.out.println(rp1);
                }
            } else if (DELETEPROPS.equalsIgnoreCase(action))
            {
                if (argHash.get("file") == null)
                {
                    usage("-file argument is required.");
                }
                List<RealProperty> rpL = DBImpl.parsePropFile(argHash.get("file"));
                DBIfc dbi = DBFactory.createDBIfc();
                dbi.createAndConnectDB(null);
                for (RealProperty rp : rpL)
                {
                    dbi.deleteProperty(rp.getPropertyName());
                }
                for (RealProperty rp1 : dbi.getProperties().values())
                {
                    System.out.println(rp1);
                }
            } else if (CREATEGROUPS.equalsIgnoreCase(action))
            {
                if (argHash.get("file") == null)
                {
                    usage("-file argument is required.");
                }
                List<IGroup> rpL = DBImpl.parseGroupFile(argHash.get("file"));
                DBIfc dbi = DBFactory.createDBIfc();
                for (IGroup rp : rpL)
                {
                    dbi.createGroup(rp);
                }
                for (IGroup rp1 : dbi.getGropusMap().values())
                {
                    System.out.println(rp1);
                }
            } else if (DELETEGROUPS.equalsIgnoreCase(action))
            {
                if (argHash.get("file") == null)
                {
                    usage("-file argument is required.");
                }
                List<IGroup> rpL = DBImpl.parseGroupFile(argHash.get("file"));
                DBIfc dbi = DBFactory.createDBIfc();
                for (IGroup rp : rpL)
                {
                    dbi.deleteGroup(rp.getName());
                }
                for (IGroup rp1 : dbi.getGropusMap().values())
                {
                    System.out.println(rp1);
                }
            } else if (PARSE.equalsIgnoreCase(action))
            {
                final BankStatement bs = new BankStatement(argHash.get("bankstatement"), argHash.get("bankconfig"),
                        argHash.get("accountname"));
                System.out.println("" + bs);

            } else if (IMPORT2DB.equalsIgnoreCase(action))
            {
                final BankStatement bs = new BankStatement(argHash.get("bankstatement"), argHash.get("bankconfig"),
                        argHash.get("accountname"));

                commit(bs);
            } else if (PARSEANDCLASSIFY.equalsIgnoreCase(action))
            {
                final BankStatement bs = new BankStatement(argHash.get("bankstatement"), argHash.get("bankconfig"),
                        argHash.get("accountname"));
                final TaxConfig tc = new TaxConfig(argHash.get("taxconfig"));
                classify(bs, tc);
                System.out.println("" + bs.toString(true));
                StringBuffer sb = report(2014, bs.getTrs());
                System.out.println("" + sb);

            } else if (CLASSIFYINDB.equalsIgnoreCase(action))
            {
                final TaxConfig tc = new TaxConfig(argHash.get("taxconfig"));
                DBIfc dbIfc = classifyindb(tc);
                StringBuffer sb = report(new Integer(argHash.get("year")).intValue(), dbIfc);
                if (sb.length() == 0)
                {
                    System.out.println("Database is empty");
                } else
                {

                    System.out.println("" + sb);
                }

            } else if (EXPORTTR.equalsIgnoreCase(action))
            {
                if (argHash.get("file") == null || !argHash.get("file").endsWith("csv"))
                {
                    usage("csv file is required as argument.");
                }
                if (argHash.get("accountname") == null)
                {
                    usage("accountname is required as argument.");
                }
                exporttr(argHash.get("file"), argHash.get("accountname"));

            } else if (IMPORTTR.equalsIgnoreCase(action))
            {
                if (argHash.get("file") == null || !argHash.get("file").endsWith("csv"))
                {
                    usage("csv file is required as argument.");
                }
                if (argHash.get("accountname") == null)
                {
                    usage("accountname is required as argument.");
                }
                importtr(argHash.get("file"), argHash.get("accountname"));

            } else
            {
                usage("Invalid action");
            }
        } catch (IOException | DBException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e)
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
