package accounts.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import accounts.Getopt;

public class DBImpl implements DBIfc
{
    private EntityManagerFactory      factory;
    private Map<String, BankAccount>  accountsMap;
    private Map<String, RealProperty> propertiesMap;
    private Map<String, IGroup>       groupsMap;

    private void createOtherDirectories(String dir) throws IOException
    {

        File d = new File(dir);
        if (!d.exists())
        {
            throw new IOException("Directory not present=" + dir);
        }
        String configDir = dir + File.separator + "config/";
        File configDirFile = new File(configDir);
        configDirFile.mkdir();
        if (!configDirFile.exists())
        {
            throw new IOException("Unable to create directory=" + configDir);
        }
        if (getClass().getResource("/accounts/resources/bbt_statement_format.txt") == null)
        {
            throw new IOException(
                    "Internal error. Unable to find resource in package=" + "/accounts/resources/bbt_statement_format.txt");

        }
        File fmtFile = new File(configDir + File.separator + "bbt_statement_format.txt");
        if (!fmtFile.exists())
            Files.copy(new File(getClass().getResource("/accounts/resources/bbt_statement_format.txt").getFile()).toPath(),
                    fmtFile.toPath());
        fmtFile = new File(configDir + File.separator + "dcu_statement_format.txt");
        if (!fmtFile.exists())
            Files.copy(new File(getClass().getResource("/accounts/resources/dcu_statement_format.txt").getFile()).toPath(),
                    fmtFile.toPath());
        fmtFile = new File(configDir + File.separator + "dcu_visa_statement_format.txt");
        if (!fmtFile.exists())
            Files.copy(new File(getClass().getResource("/accounts/resources/dcu_visa_statement_format.txt").getFile()).toPath(),
                    fmtFile.toPath());
        fmtFile = new File(configDir + File.separator + "wellsfargo_statement_format.txt");
        if (!fmtFile.exists())
            Files.copy(new File(getClass().getResource("/accounts/resources/wellsfargo_statement_format.txt").getFile()).toPath(),
                    fmtFile.toPath());
        fmtFile = new File(configDir + File.separator + "amex_statement_format.txt");
        if (!fmtFile.exists())
            Files.copy(new File(getClass().getResource("/accounts/resources/amex_statement_format.txt").getFile()).toPath(),
                    fmtFile.toPath());
        fmtFile = new File(configDir + File.separator + "transaction_types.txt");
        if (!fmtFile.exists())
            Files.copy(new File(getClass().getResource("/accounts/resources/transaction_types.txt").getFile()).toPath(),
                    fmtFile.toPath());
        fmtFile = new File(configDir + File.separator + "tax_category.txt");
        if (!fmtFile.exists())
            Files.copy(new File(getClass().getResource("/accounts/resources/tax_category.txt").getFile()).toPath(),
                    fmtFile.toPath());

    }

    @Override
    public void createAndConnectDB(String dir) throws IOException
    {
        if (factory == null)
        {
            if (dir == null)
            {
                if (System.getProperty("ACCOUNTSDB") == null)
                {
                    throw new IOException(
                            "Set Java system property ACCOUNTSDB to directory where accounts repository is present or to be created");
                } else
                {
                    dir = System.getProperty("ACCOUNTSDB");
                }
            }
            File d = new File(dir);
            if (!d.exists())
            {
                throw new IOException("Directory not present=" + dir);
            }

            Map<String, String> properties = new HashMap<String, String>();
            properties.put("javax.persistence.jdbc.url",
                    "jdbc:derby:" + d.getCanonicalPath().replace("\\", "/") + "/taxdb;create=true");
            factory = Persistence.createEntityManagerFactory("taxaccounting", properties);
            // creates tables if it does not exist
            EntityManager em = factory.createEntityManager();
            em.close();

            createOtherDirectories(dir);
        }
        EntityManager em = factory.createEntityManager();
        List<BankAccount> acList = em.createNamedQuery("BankAccount.getList", BankAccount.class).getResultList();
        List<RealProperty> propList = em.createNamedQuery("RealProperty.getList", RealProperty.class).getResultList();
        List<IGroup> groupList = em.createNamedQuery("IGroup.getList", IGroup.class).getResultList();
        em.close();
        accountsMap = new HashMap<String, BankAccount>();
        for (BankAccount ba : acList)
        {
            accountsMap.put(ba.getName(), ba);
        }
        propertiesMap = new HashMap<String, RealProperty>();
        for (RealProperty prop : propList)
        {
            propertiesMap.put(prop.getPropertyName(), prop);
        }
        groupsMap = new HashMap<String, IGroup>();
        for (IGroup group : groupList)
        {
            groupsMap.put(group.getName(), group);
        }
    }

    public static final int MAX_ACCOUNTS = 100;

    private int getNextTableId()
    {
        boolean[] tableIdArray = new boolean[MAX_ACCOUNTS];
        for (int i = 1; i < MAX_ACCOUNTS; i++)
        {
            tableIdArray[i] = false;
        }
        tableIdArray[0] = true; // don't use 0. Start from 1
        for (BankAccount ba : accountsMap.values())
        {
            tableIdArray[ba.getTrTableId()] = true;
        }

        for (int i = 1; i < MAX_ACCOUNTS; i++)
        {
            if (tableIdArray[i] == false)
                return i;
        }
        return -1;
    }

    @Override
    public void createBankAccount(String accountName, String bankName) throws DBException
    {
        if (accountName == null)
            throw new DBException(DBException.INVALID_INPUT, "account name is null");
        accountName = accountName.trim().toLowerCase();
        if (accountName.isEmpty())
            throw new DBException(DBException.INVALID_INPUT, "account name is empty");
        if (accountsMap.containsKey(accountName))
            throw new DBException(DBException.ENTRY_EXISTS, "account name exists");
        if (accountsMap.size() == MAX_ACCOUNTS)
            throw new DBException(DBException.ACCOUNT_MAX_LIMIT, "Max accounts limit reached");
        BankAccount ba = new BankAccount();
        ba.setName(accountName);
        ba.setBankName(bankName);
        int tableId = getNextTableId();
        ba.setTrTableId(tableId);
        // TR tr = createTR(tableId);

        EntityManager em = factory.createEntityManager();
        try
        {
            em.getTransaction().begin();
            em.persist(ba);
            em.getTransaction().commit();
            accountsMap.put(accountName, ba);
        } finally
        {
            em.close();
        }

    }

    @Override
    public void updateBankAccount(BankAccount bAccount) throws DBException
    {
        EntityManager em = factory.createEntityManager();
        try
        {
            em.getTransaction().begin();
            em.persist(bAccount);
            em.getTransaction().commit();
            accountsMap.put(bAccount.getName(), bAccount);
        } finally
        {
            em.close();
        }
    }

    @Override
    public void deleteBankAccount(String accountName) throws DBException
    {
        if (accountName == null)
            throw new DBException(DBException.INVALID_INPUT, "account name is null");
        accountName = accountName.trim().toLowerCase();
        if (accountName.isEmpty())
            throw new DBException(DBException.INVALID_INPUT, "account name is empty");
        if (!accountsMap.containsKey(accountName))
        {
            return;
            // throw new DBException(DBException.NOTPRESENT,
            // "account not present. Accountname=" + accountName);
        }
        EntityManager em = factory.createEntityManager();
        try
        {
            em.getTransaction().begin();
            BankAccount ba = em.find(BankAccount.class, accountName);
            em.createQuery("DELETE FROM TR" + ba.getTrTableId() + "  e").executeUpdate();

            // em.createQuery("DROP TABLE \"TEST\".\"TR" + ba.getTrTableId() +
            // "\"").executeUpdate();
            em.remove(ba);
            em.getTransaction().commit();
            accountsMap.remove(ba.getName());

        } finally
        {
            em.close();
        }

    }

    @Override
    public void createProperty(RealProperty prop) throws DBException
    {
        if (prop == null)
            throw new DBException(DBException.INVALID_INPUT, "property name is null");
        if (prop.getPropertyName() == null || prop.getPropertyName().isEmpty())
            throw new DBException(DBException.INVALID_INPUT, "property name is empty");
        if (prop.getCost() == -1 || prop.getRenovation() == -1 || prop.getPurchaseDate() == null)
            throw new DBException(DBException.INVALID_INPUT, "properties values are not set.");

        EntityManager em = factory.createEntityManager();
        try
        {
            em.getTransaction().begin();

            RealProperty rp = em.find(RealProperty.class, prop.getPropertyName());
            if (rp == null)
            {
                em.persist(prop);
            } else
            {
                em.merge(prop);
            }
            em.getTransaction().commit();
            propertiesMap.put(prop.getPropertyName(), prop);
        } finally
        {
            em.close();
        }

    }

    @Override
    public void createGroup(IGroup group) throws DBException
    {
        if (group == null)
            throw new DBException(DBException.INVALID_INPUT, "Group name is null");
        if (group.getName() == null || group.getName().isEmpty())
            throw new DBException(DBException.INVALID_INPUT, "group name is empty");

        EntityManager em = factory.createEntityManager();
        try
        {
            em.getTransaction().begin();

            IGroup grp = em.find(IGroup.class, group.getName());
            if (grp == null)
            {
                em.persist(group);
            } else
            {
                em.merge(group);
            }
            em.getTransaction().commit();

            groupsMap.put(group.getName(), group);
        } finally
        {
            em.close();
        }

    }

    @Override
    public void deleteGroup(String name) throws DBException
    {
        if (name == null)
            throw new DBException(DBException.INVALID_INPUT, "group name is null");
        name = name.trim().toLowerCase();
        if (name.isEmpty())
            throw new DBException(DBException.INVALID_INPUT, "group name is empty");
        if (!groupsMap.containsKey(name))
            throw new DBException(DBException.NOTPRESENT, "group not present=" + name);
        EntityManager em = factory.createEntityManager();
        try
        {
            em.getTransaction().begin();
            IGroup ba = em.find(IGroup.class, name);
            em.remove(ba);
            em.getTransaction().commit();
            groupsMap.remove(name);
        } finally
        {
            em.close();
        }

    }

    @Override
    public void updateProperty(RealProperty prop) throws DBException
    {
        throw new DBException(DBException.NOTIMPLEMENTED, "Not implemented");
    }

    @Override
    public void deleteProperty(String name) throws DBException
    {
        if (name == null)
            throw new DBException(DBException.INVALID_INPUT, "property name is null");
        name = name.trim().toLowerCase();
        if (name.isEmpty())
            throw new DBException(DBException.INVALID_INPUT, "property name is empty");
        if (!propertiesMap.containsKey(name))
        {
            return;
            // throw new DBException(DBException.NOTPRESENT,
            // "property not present=" + name);
        }
        EntityManager em = factory.createEntityManager();
        try
        {
            em.getTransaction().begin();
            RealProperty ba = em.find(RealProperty.class, name);
            em.remove(ba);
            em.getTransaction().commit();
            propertiesMap.remove(name);
        } finally
        {
            em.close();
        }

    }

    private String getTRClass(int tableId)
    {
        return "accounts.db.inst.TR" + tableId;
    }

    @Override
    public boolean tableExists(String tablename) throws DBException
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Map<TRId, TR> getTransactions(int tableId) throws DBException
    {

        EntityManager em = factory.createEntityManager();
        List<TR> trList = em.createNamedQuery("TR" + tableId + ".getList", TR.class).getResultList();
        em.close();
        Map<TRId, TR> trMap = new TreeMap<TRId, TR>();
        for (TR tr : trList)
        {
            tr.setTrId();
            TRId trId = tr.getTrId();
            trMap.put(trId, tr);
        }

        return trMap;
    }

    @Override
    public Map<String, BankAccount> getAccounts() throws DBException
    {
        return accountsMap;
    }

    @Override
    public Map<String, RealProperty> getProperties() throws DBException
    {
        return propertiesMap;
    }

    @Override
    public TR createCorrespondingTRObj(BankAccount ba) throws DBException
    {
        TR tr;
        try
        {
            Class<?> clazz = Class.forName(getTRClass(ba.getTrTableId()));
            tr = (TR) clazz.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e1)
        {
            throw new DBException(DBException.INTERNAL_ERR, "Unable to find a necessary class.");
        }
        return tr;
    }

    @Override
    public int updateTransactions(Map<TRId, TR> trs) throws DBException
    {

        EntityManager em = factory.createEntityManager();
        int count = 0;
        try
        {
            em.getTransaction().begin();
            for (TR tr : trs.values())
            {
                TR dbTr = em.find(tr.getClass(), tr.getTrId());
                if (dbTr == null)
                {
                    em.persist(tr);
                } else
                {
                    if (!dbTr.isLocked())
                    {
                        dbTr.copyNonPrimaryFields(tr);
                        em.merge(dbTr);
                    }
                }
                count++;
            }
            em.getTransaction().commit();
        } finally
        {
            em.close();
        }
        return count;

    }

    @Override
    public void updateTransaction(TR tr) throws DBException
    {
        EntityManager em = factory.createEntityManager();
        try
        {
            em.getTransaction().begin();
            TR dbTr = em.find(tr.getClass(), tr.getTrId());
            if (dbTr == null)
            {
                em.persist(tr);
            } else
            {
                if (!dbTr.isLocked())
                {
                    dbTr.copyNonPrimaryFields(tr);
                    em.merge(dbTr);
                }
            }
            em.getTransaction().commit();
        } finally
        {
            em.close();
        }

    }

    public static List<BankAccount> parseAccountFile(String filename) throws IOException, ParseException
    {
        final FileReader fr = new FileReader(filename);
        final BufferedReader br = new BufferedReader(fr);
        List<BankAccount> aL = new ArrayList<BankAccount>();
        try
        {
            for (String line; (line = br.readLine()) != null;)
            {
                line = line.toLowerCase().trim();
                if (line.isEmpty())
                {
                    continue;
                }
                line = line.toLowerCase().trim();
                if (line.isEmpty() || line.startsWith("#"))
                {
                    continue;
                }

                String[] fields = line.split(",");
                if (fields.length != 2)
                {
                    throw new IOException("Invalid account line=" + line + ", expected 2 columns");
                }
                BankAccount ba = new BankAccount();
                ba.setName(fields[0]);
                ba.setBankName(fields[1]);
                aL.add(ba);

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
        return aL;
    }

    public static List<RealProperty> parsePropFile(String filename) throws IOException, ParseException
    {

        final FileReader fr = new FileReader(filename);
        final BufferedReader br = new BufferedReader(fr);
        List<RealProperty> rpL = new ArrayList<RealProperty>();
        try
        {
            for (String line; (line = br.readLine()) != null;)
            {
                line = line.toLowerCase().trim();
                if (line.isEmpty())
                {
                    continue;
                }
                line = line.toLowerCase().trim();
                if (line.isEmpty() || line.startsWith("#"))
                {
                    continue;
                }
                String[] fields = line.split(",", -1);
                if (fields.length != 7)
                {
                    throw new IOException("Invalid property line=" + line);
                }

                RealProperty rp = new RealProperty();
                rp.setPropertyName(fields[0]);
                rp.setCost(new Integer(fields[1]).intValue());
                rp.setLandValue(new Integer(fields[2]).intValue());
                rp.setRenovation(new Integer(fields[3]).intValue());
                rp.setLoanClosingCost(new Integer(fields[4]).intValue());
                rp.setOwnerCount(new Integer(fields[5]).intValue());
                DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
                Date purchaseDate = formatter.parse(fields[6]);

                rp.setPurchaseDate(purchaseDate);
                rpL.add(rp);
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
        return rpL;
    }

    public static List<IGroup> parseGroupFile(String filename) throws IOException, ParseException
    {

        final FileReader fr = new FileReader(filename);
        final BufferedReader br = new BufferedReader(fr);
        List<IGroup> rpL = new ArrayList<IGroup>();
        try
        {
            for (String line; (line = br.readLine()) != null;)
            {
                line = line.toLowerCase().trim();
                if (line.isEmpty())
                {
                    continue;
                }
                line = line.toLowerCase().trim();
                if (line.isEmpty() || line.startsWith("#"))
                {
                    continue;
                }
                String[] fields = line.split(",");
                if (fields.length < 3)
                {
                    throw new IOException("Invalid group line" + line);
                }

                IGroup grp = new IGroup();
                grp.setName(fields[0]);
                List<String> members = new ArrayList<String>();
                for (int i = 1; i < fields.length; i++)
                {
                    String member = fields[i].trim().toLowerCase();
                    if (member.isEmpty())
                        continue;
                    members.add(member);
                }
                grp.setMembers(members);

                rpL.add(grp);
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
        return rpL;
    }

    public static void usage(final String err)
    {
        if (err != null && !"".equals(err))
        {
            System.out.println(err);
        }
        System.out.println("Usage: -A action options\n");
        System.out.println("    -A createac -name <n> -bank <n>\n");
        System.out.println("    -A deleteac -name <n>\n");
        System.out.println("    -A createacs -file <f>\n");
        System.out.println("    -A deleteacs -file <f>\n");
        System.out.println(
                "    -A createprop -name <n> -cost <int> -renovation <int> -purchasedate <mm/dd/yyyy>  [-landvalue <int>] [-closingcost <int>] \n");

        System.out.println("    -A deleteprop -name <n>\n");
        System.out.println("    -A createprops -file <f>\n");
        System.out.println("    -A DELETEPROPS -file <f>\n");
        System.out.println("    -A creategroups -file <f>\n");
        System.out.println("    -A deletegroups -file <f>\n");

        System.exit(1);
    }

    public static final String CREATEAC     = "createac";
    public static final String CREATEACS    = "createacs";
    public static final String DELETEAC     = "deleteac";
    public static final String DELETEACS    = "deleteacs";
    public static final String CREATEPROP   = "createprop";
    public static final String CREATEPROPS  = "createprops";
    public static final String DELETEPROP   = "deleteprop";
    public static final String DELETEPROPS  = "deleteprops";
    public static final String CREATEGROUPS = "creategroups";
    public static final String DELETEGROUPS = "deletegroups";

    public static final Map<String, String> ALL_OPTS = new HashMap<String, String>();

    static
    {
        ALL_OPTS.put("A", Getopt.CONTRNT_S);
        ALL_OPTS.put("name", Getopt.CONTRNT_S);
        ALL_OPTS.put("bank", Getopt.CONTRNT_S);
        ALL_OPTS.put("cost", Getopt.CONTRNT_I);
        ALL_OPTS.put("landvalue", Getopt.CONTRNT_I);
        ALL_OPTS.put("renovation", Getopt.CONTRNT_I);
        ALL_OPTS.put("closingcost", Getopt.CONTRNT_I);
        ALL_OPTS.put("purchasedate", Getopt.CONTRNT_S);
        ALL_OPTS.put("file", Getopt.CONTRNT_S);
    }

    public static void main(final String[] args)
    {

        final Map<String, String> argHash = Getopt.processCommandLineArgL(args, ALL_OPTS, true);
        if (argHash.get(Getopt.ERROR) != null)
        {
            usage(argHash.get(Getopt.ERROR));
        }

        DBImpl dbi = new DBImpl();
        try
        {
            dbi.createAndConnectDB(null);
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        final String action = argHash.get("A");
        try
        {
            if (CREATEAC.equalsIgnoreCase(action))
            {

                dbi.createBankAccount(argHash.get("name"), argHash.get("bank"));
                for (BankAccount ba : dbi.accountsMap.values())
                {
                    System.out.println(ba);
                }
            } else if (CREATEACS.equalsIgnoreCase(action))
            {
                if (argHash.get("file") == null)
                {
                    usage("-file argument is required.");
                }

                List<BankAccount> acL = parseAccountFile(argHash.get("file"));
                for (BankAccount ac : acL)
                {
                    if (!dbi.accountsMap.containsKey(ac.getName()))
                        dbi.createBankAccount(ac.getName(), ac.getBankName());
                }
                for (BankAccount ba : dbi.accountsMap.values())
                {
                    System.out.println(ba);
                }
            } else if (DELETEAC.equalsIgnoreCase(action))
            {

                dbi.deleteBankAccount(argHash.get("name"));
                for (BankAccount ba : dbi.accountsMap.values())
                {
                    System.out.println(ba);
                }
            } else if (DELETEACS.equalsIgnoreCase(action))
            {
                if (argHash.get("file") == null)
                {
                    usage("-file argument is required.");
                }
                List<BankAccount> acL = parseAccountFile(argHash.get("file"));
                for (BankAccount ac : acL)
                {
                    dbi.deleteBankAccount(ac.getName());
                }
                for (BankAccount ba : dbi.accountsMap.values())
                {
                    System.out.println(ba);
                }

            } else if (CREATEPROP.equalsIgnoreCase(action))
            {
                if (argHash.get("name") == null || argHash.get("cost") == null || argHash.get("purchasedate") == null)
                {
                    usage("null values");
                }
                RealProperty rp = new RealProperty();
                rp.setPropertyName(argHash.get("name"));
                rp.setCost(new Integer(argHash.get("cost")).intValue());
                if (argHash.get("landvalue") != null)
                    rp.setLandValue(new Integer(argHash.get("landvalue")).intValue());
                rp.setRenovation(new Integer(argHash.get("renovation")).intValue());
                if (argHash.get("closingcost") != null)
                    rp.setLandValue(new Integer(argHash.get("closingcost")).intValue());
                DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
                Date purchaseDate = formatter.parse(argHash.get("purchasedate"));

                rp.setPurchaseDate(purchaseDate);
                dbi.createProperty(rp);
                for (RealProperty rp1 : dbi.propertiesMap.values())
                {
                    System.out.println(rp1);
                }
            } else if (DELETEPROP.equalsIgnoreCase(action))
            {
                dbi.deleteProperty(argHash.get("name"));

                for (RealProperty rp1 : dbi.propertiesMap.values())
                {
                    System.out.println(rp1);
                }
            } else if (CREATEPROPS.equalsIgnoreCase(action))
            {
                if (argHash.get("file") == null)
                {
                    usage("-file argument is required.");
                }
                List<RealProperty> rpL = parsePropFile(argHash.get("file"));
                for (RealProperty rp : rpL)
                {
                    dbi.createProperty(rp);
                }
                for (RealProperty rp1 : dbi.propertiesMap.values())
                {
                    System.out.println(rp1);
                }
            } else if (DELETEPROPS.equalsIgnoreCase(action))
            {
                if (argHash.get("file") == null)
                {
                    usage("-file argument is required.");
                }
                List<RealProperty> rpL = parsePropFile(argHash.get("file"));
                for (RealProperty rp : rpL)
                {
                    dbi.deleteProperty(rp.getPropertyName());
                }
                for (RealProperty rp1 : dbi.propertiesMap.values())
                {
                    System.out.println(rp1);
                }
            } else if (CREATEGROUPS.equalsIgnoreCase(action))
            {
                if (argHash.get("file") == null)
                {
                    usage("-file argument is required.");
                }
                List<IGroup> rpL = parseGroupFile(argHash.get("file"));
                for (IGroup rp : rpL)
                {
                    dbi.createGroup(rp);
                }
                for (IGroup rp1 : dbi.groupsMap.values())
                {
                    System.out.println(rp1);
                }
            } else if (DELETEGROUPS.equalsIgnoreCase(action))
            {
                if (argHash.get("file") == null)
                {
                    usage("-file argument is required.");
                }
                List<IGroup> rpL = parseGroupFile(argHash.get("file"));
                for (IGroup rp : rpL)
                {
                    dbi.deleteGroup(rp.getName());
                }
                for (IGroup rp1 : dbi.groupsMap.values())
                {
                    System.out.println(rp1);
                }
            } else
            {
                usage("Unknown action");
            }
        } catch (DBException | ParseException | IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, IGroup> getGropusMap() throws DBException
    {
        return groupsMap;
    }

}
