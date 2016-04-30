package accounts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import accounts.db.BankAccount;
import accounts.db.Company;
import accounts.db.DBException;
import accounts.db.DBFactory;
import accounts.db.DBIfc;
import accounts.db.DBImpl;
import accounts.db.IGroup;
import accounts.db.RealProperty;
import accounts.db.TR;
import accounts.db.TRId;
import accounts.exp.AccountExp;

public class AccountsMainApp
{
    private BankAccount bankAccount = null;

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
    public static final String               BANKFEES         = "bankfees";
    public static final String               PROFIT           = "profit";
    public static final String               OTHER            = "other";
    public static final Map<String, Integer> scheduleEMap;
    public static final String[]             scheduleEAry     = { null, null, null, RENT, null, null, null, null, COMMISSIONS,
            INSURANCE, PROFESSIONALFEES, null, MORTGAGEINTEREST, null, REPAIRS, null, TAX, UTILITIES, DEPRECIATION, HOA, OTHER };

    public static final Map<String, String> TRTYPE_MAP = new HashMap<>();

    static
    {
        TRTYPE_MAP.put("rent", "");
        TRTYPE_MAP.put("commissions", "");
        TRTYPE_MAP.put("insurance", "");
        TRTYPE_MAP.put("professionalfees", "");
        TRTYPE_MAP.put("mortgageinterest", "");
        TRTYPE_MAP.put("repairs", "");
        TRTYPE_MAP.put("tax", "");
        TRTYPE_MAP.put("utilities", "");
        TRTYPE_MAP.put("depreciation", "");
        TRTYPE_MAP.put("hoa", "");
        TRTYPE_MAP.put("bankfees", "");
        TRTYPE_MAP.put("ignore", "");
    }

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

    private static Map<String, Float> trTypeTotal(final ArrayList<TR> art)
    {
        final Map<String, Float> map = new HashMap<String, Float>();
        for (final TR tr : art)
        {
            if (!map.containsKey(tr.getTrType()))
            {
                map.put(tr.getTrType(), tr.getDebit());
                continue;
            }
            final Float f = map.get(tr.getTrType()) + tr.getDebit();
            map.put(tr.getTrType(), f);
        }
        return map;

    }

    private static StringBuffer report(final int year, final DBIfc dbIfc, Map<String, Float[]> propTable,
                                       Map<String, ArrayList<TR>> companyTrMap,
                                       Map<String, ArrayList<TR>> otherTrMap) throws DBException
    {
        final Map<String, ArrayList<TR>> propTrMap = new HashMap<String, ArrayList<TR>>();
        final Map<String, ArrayList<TR>> grpTrMap = new HashMap<String, ArrayList<TR>>();
        Map<String, BankAccount> acctMap = dbIfc.getAccounts();
        Map<String, IGroup> groupsMap = dbIfc.getGroupsMap();

        // Traverse all bank accounts and all transactions
        for (BankAccount ba : acctMap.values())
        {
            Map<TRId, TR> trMap = dbIfc.getTransactions(ba.getTrTableId());
            // Group the transactions for each property and group
            addToPropertyMap(year, trMap, groupsMap, propTrMap, grpTrMap, companyTrMap, otherTrMap);
        }

        StringBuffer sb = reportFromPropMap(propTrMap, grpTrMap, dbIfc, groupsMap, propTable);
        sb.append(reportFromCompanyMap(companyTrMap));
        sb.append(reportFromOtherMap(otherTrMap));

        return sb;
    }

    private static void addToPropertyMap(final int year, final Map<TRId, TR> trMap, final Map<String, IGroup> groupsMap,
                                         Map<String, ArrayList<TR>> propTrMap, Map<String, ArrayList<TR>> grpTrMap,
                                         Map<String, ArrayList<TR>> companyTrMap, final Map<String, ArrayList<TR>> otherTrMap)
    {
        for (final TR tr : trMap.values())
        {
            final Calendar cal = Calendar.getInstance();
            cal.setTime(tr.getDate());

            if (cal.get(Calendar.YEAR) != year)
            {
                continue;
            }
            if ("rental".equalsIgnoreCase(tr.getTaxCategory()) && (tr.getProperty() != null && !tr.getProperty().isEmpty()))
            {
                // This transaction belongs to a rental property
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
                        if (grpTrMap.containsKey(tr.getProperty()))
                        {
                            final ArrayList<TR> arrTr = grpTrMap.get(tr.getProperty());
                            arrTr.add(tr);
                        } else
                        {
                            final ArrayList<TR> arrTr = new ArrayList<TR>();
                            arrTr.add(tr);
                            grpTrMap.put(tr.getProperty(), arrTr);
                            continue;
                        }
                    } else
                    {
                        final ArrayList<TR> arrTr = new ArrayList<TR>();
                        arrTr.add(tr);
                        propTrMap.put(tr.getProperty(), arrTr);
                        continue;
                    }
                }
            } else if ("company".equalsIgnoreCase(tr.getTaxCategory()))
            {
                //
                if (companyTrMap.containsKey(tr.getOtherEntity()))
                {
                    final ArrayList<TR> arrTr = companyTrMap.get(tr.getOtherEntity());
                    arrTr.add(tr);
                } else
                {
                    final ArrayList<TR> arrTr = new ArrayList<TR>();
                    arrTr.add(tr);
                    companyTrMap.put(tr.getOtherEntity(), arrTr);
                    continue;
                }
            } else if ("realestate".equalsIgnoreCase(tr.getTaxCategory()))
            {
                //
                if (companyTrMap.containsKey("realestate"))
                {
                    final ArrayList<TR> arrTr = companyTrMap.get("realestate");
                    arrTr.add(tr);
                } else
                {
                    final ArrayList<TR> arrTr = new ArrayList<TR>();
                    arrTr.add(tr);
                    companyTrMap.put("realestate", arrTr);
                    continue;
                }
            } else
            {
                if (tr.getOtherEntity() != null && !tr.getOtherEntity().isEmpty())
                {
                    if (otherTrMap.containsKey(tr.getOtherEntity()))
                    {
                        final ArrayList<TR> arrTr = otherTrMap.get(tr.getOtherEntity());
                        arrTr.add(tr);
                    } else
                    {
                        final ArrayList<TR> arrTr = new ArrayList<TR>();
                        arrTr.add(tr);
                        otherTrMap.put(tr.getOtherEntity(), arrTr);
                        continue;
                    }

                }
            }
        }
    }

    private static StringBuffer reportFromOtherMap(final Map<String, ArrayList<TR>> otherTrMap)
    {

        Map<String, Map<String, Float>> trTypeTotalMap = new HashMap<String, Map<String, Float>>();

        for (final String name : otherTrMap.keySet())
        {
            final Map<String, Float> trTypeMap = trTypeTotal(otherTrMap.get(name));
            trTypeTotalMap.put(name, trTypeMap);
        }
        List<String> listCompanies = new ArrayList<String>(otherTrMap.keySet());
        Collections.sort(listCompanies);
        // For each company first calculate the totals in each category and
        // then prepare the report
        StringBuffer sb = new StringBuffer();
        for (final String name : listCompanies)
        {
            sb.append("\nReport for other entity=" + name + "\n");
            final Map<String, Float> trTypeMap = trTypeTotalMap.get(name);
            final Map<String, Float> trTypeMapSorted = new TreeMap<String, Float>(trTypeMap);
            for (String trType : trTypeMapSorted.keySet())
            {
                sb.append("    " + trType + "=" + trTypeMapSorted.get(trType) + "\n");
            }
            sb.append("");
        }
        return sb;
    }

    private static StringBuffer reportFromCompanyMap(final Map<String, ArrayList<TR>> companyTrMap)
    {

        Map<String, Map<String, Float>> trTypeTotalMap = new HashMap<String, Map<String, Float>>();

        for (final String name : companyTrMap.keySet())
        {
            final Map<String, Float> trTypeMap = trTypeTotal(companyTrMap.get(name));
            trTypeTotalMap.put(name, trTypeMap);
        }
        List<String> listCompanies = new ArrayList<String>(companyTrMap.keySet());
        Collections.sort(listCompanies);
        // For each company first calculate the totals in each category and
        // then prepare the report
        StringBuffer sb = new StringBuffer();
        for (final String name : listCompanies)
        {
            sb.append("\nReport for company=" + name + "\n");
            final Map<String, Float> trTypeMap = trTypeTotalMap.get(name);
            final Map<String, Float> trTypeMapSorted = new TreeMap<String, Float>(trTypeMap);
            for (String trType : trTypeMapSorted.keySet())
            {
                sb.append("    " + trType + "=" + trTypeMapSorted.get(trType) + "\n");
            }
            sb.append("");
        }
        return sb;
    }

    private static StringBuffer reportFromPropMap(final Map<String, ArrayList<TR>> propTrMap,
                                                  final Map<String, ArrayList<TR>> groupTrMap, final DBIfc dbIfc,
                                                  final Map<String, IGroup> groupsMap,
                                                  Map<String, Float[]> propTable) throws DBException
    {

        final Map<String, RealProperty> propertyMap = dbIfc.getProperties();
        Map<String, Map<String, Float>> propTrTypeTotalMap = new HashMap<String, Map<String, Float>>();

        for (final String propName : propTrMap.keySet())
        {
            final Map<String, Float> trTypeMap = trTypeTotal(propTrMap.get(propName));
            propTrTypeTotalMap.put(propName, trTypeMap);
        }
        // Start assigning from group to individual
        for (final String grpName : groupsMap.keySet())
        {
            IGroup group = groupsMap.get(grpName);
            if (group.getMembers() == null)
                continue; // group not created properly
            if (!groupTrMap.containsKey(grpName))
            {
                // THere is no transaction in any of the bank accounts assigned
                // to this group
                continue;
            }
            final Map<String, Float> grpTrTypeMap = trTypeTotal(groupTrMap.get(grpName));
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
            Float[] taxValues = new Float[22];
            propTable.put(propName, taxValues);
            sb.append("\nSchedule E for Property=" + propName + "\n");
            final Map<String, Float> trTypeMap = propTrTypeTotalMap.get(propName);
            int ownerCount = 1;
            RealProperty rp = null;
            if (propertyMap.containsKey(propName))
            {
                rp = propertyMap.get(propName);
                ownerCount = rp.getOwnerCount();
            }
            float rentPercentage = 94;
            if (rp.getPropMgmtCompany() != null)
            {
                if (dbIfc.getCompanies().containsKey(rp.getPropMgmtCompany()))
                {
                    Company comp = dbIfc.getCompanies().get(rp.getPropMgmtCompany());
                    if (comp != null && comp.getRentPercentage() > 0)
                    {
                        rentPercentage = 100 - comp.getRentPercentage();
                    }
                }
            }
            float totalExpense = 0;
            // Ok to hardcode 18 since tax row numbers do not change
            for (int i = 1; i < 18; i++)
            {
                if (scheduleEAry[i] == null)
                    continue;
                float value = 0;
                if (trTypeMap.containsKey(scheduleEAry[i]))
                {
                    if (RENT.equalsIgnoreCase(scheduleEAry[i]))
                    {
                        value = (trTypeMap.get(scheduleEAry[i]) * rentPercentage / 100 / ownerCount);

                    } else
                    {
                        value = (trTypeMap.get(scheduleEAry[i]) / ownerCount);
                        totalExpense += value;
                    }
                    sb.append("    " + i + " " + scheduleEAry[i] + "=" + value + "\n");
                }
                taxValues[i] = value;
            }

            if (rp != null)
            {
                int costPlusReno = rp.getCost() + rp.getRenovation();
                double depreciation = (costPlusReno * 3.64 / 100);
                double value = (-(depreciation / ownerCount));
                sb.append("    18 depreciation" + "=" + value + "\n");
                totalExpense += value;
                taxValues[18] = (float) value;

            }
            float bankfees = 0;
            float hoa = 0;
            float closingDepreciation = 0;

            if (trTypeMap.containsKey(BANKFEES))
            {
                // include everything else in the other category
                bankfees = (trTypeMap.get(BANKFEES) / ownerCount);
            }
            if (trTypeMap.containsKey(HOA))
            {
                hoa += (trTypeMap.get(HOA) / ownerCount);
            }
            if (rp != null)
            {
                if (rp.getLoanClosingCost() > 0)
                {
                    closingDepreciation = (float) (-(rp.getLoanClosingCost() * 6.67 / 100 / ownerCount));
                }
            }
            float valueItem19 = (bankfees + hoa + closingDepreciation);
            totalExpense += (valueItem19);
            taxValues[19] = (float) valueItem19;
            sb.append("    19 other(hoa +bank fees+loan closing)" + bankfees + "+" + hoa + "+" + closingDepreciation + " ="
                    + valueItem19 + "\n");

            sb.append("    20 total expense" + "=" + (totalExpense) + "\n");
            StringBuffer sbUnown = new StringBuffer();

            for (final String key1 : trTypeMap.keySet())
            {
                if (!scheduleEMap.containsKey(key1) && !BANKFEES.equals(key1))
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

    private static StringBuffer report(final int year, final Map<TRId, TR> trMap) throws DBException
    {
        final Map<String, ArrayList<TR>> propTrMap = new HashMap<String, ArrayList<TR>>();
        final Map<String, ArrayList<TR>> grpTrMap = new HashMap<String, ArrayList<TR>>();
        final Map<String, ArrayList<TR>> companyTrMap = new HashMap<String, ArrayList<TR>>();
        final Map<String, ArrayList<TR>> otherTrMap = new HashMap<String, ArrayList<TR>>();
        Map<String, IGroup> dummyGrpMap = new HashMap<String, IGroup>();
        addToPropertyMap(year, trMap, dummyGrpMap, propTrMap, grpTrMap, companyTrMap, otherTrMap);

        Map<String, Float[]> propTable = new TreeMap<String, Float[]>();
        StringBuffer sb = reportFromPropMap(propTrMap, grpTrMap, null, null, propTable);
        sb.append(reportFromCompanyMap(companyTrMap));
        sb.append(reportFromOtherMap(otherTrMap));

        return sb;

    }

    public static Map<TRId, TR> readTransactions(DBIfc dbIfc, BankAccount ba, String file) throws IOException, DBException,
                                                                                           ParseException
    {
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
                TR tr = dbIfc.createCorrespondingTRObj(ba);

                tr.importLine(line);
                // TODO user may have messed up the description.
                trs.put(tr.getTrId(), tr);
            }
            return trs;
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

    private static void importFromCsv(String accountName, String file) throws DBException, IOException, ParseException
    {
        DBIfc dbIfc = DBFactory.createDBIfc();
        dbIfc.createAndConnectDB(null);
        if (accountName == null && file != null)
        {
            throw new IOException("Account cannot be null when file name is specified.");
        }
        if (accountName != null)
        {
            accountName = accountName.toLowerCase().trim();
            if (!dbIfc.getAccounts().containsKey(accountName))
            {
                throw new IOException("Account is not present: " + accountName + ", List=" + dbIfc.getAccounts().keySet());
            }
        }
        for (BankAccount ba : dbIfc.getAccounts().values())
        {
            if (accountName != null && !ba.getName().equalsIgnoreCase(accountName))
            {
                continue;
            }

            String outfile = null;
            if (file == null)
            {
                String dir = System.getProperty("ACCOUNTSDB") + File.separator + "exporttr";
                File dirFile = new File(dir);
                if (!dirFile.isDirectory())
                {
                    dirFile.mkdir();
                }
                outfile = dir + File.separator + "export_" + ba.getName() + ".csv";
            } else
            {
                outfile = file;
                if (!outfile.endsWith(".csv"))
                {
                    outfile += ".csv";
                }
            }
            System.out.println("Reading transactions for " + ba.getName());
            Map<TRId, TR> trs = readTransactions(dbIfc, ba, outfile);
            int count = dbIfc.updateTransactions(trs, false);
            System.out.println("Updated transactions for " + ba.getName() + ", Count=" + count);
        }
        System.out.println("Import transactions completed ");

    }

    private static void importFromExcel(String accountName, String file, boolean commit) throws DBException, IOException,
                                                                                         ParseException, AccountExp
    {
        DBIfc dbIfc = DBFactory.createDBIfc();
        AccountsUtil.createInstance();
        dbIfc.createAndConnectDB(null);
        if (accountName == null && file != null)
        {
            throw new IOException("Account cannot be null when file name is specified.");
        }
        if (accountName != null)
        {
            accountName = accountName.toLowerCase().trim();
            if (!dbIfc.getAccounts().containsKey(accountName))
            {
                throw new IOException("Account is not present: " + accountName + ", List=" + dbIfc.getAccounts().keySet());
            }
        }

        Map<String, Map<TRId, TR>> baMap = new TreeMap<>();

        for (BankAccount ba : dbIfc.getAccounts().values())
        {
            Map<TRId, TR> trMap = dbIfc.getTransactions(ba.getTrTableId());
            baMap.put(ba.getName(), trMap);
        }

        String excelFile = null;
        if (file == null)
        {
            String dir = System.getProperty("ACCOUNTSDB") + File.separator + "exporttr";
            File dirFile = new File(dir);
            if (!dirFile.isDirectory())
            {
                dirFile.mkdir();
            }
            excelFile = dir + File.separator + "export_allaccounts.xlsx";
        } else
        {
            excelFile = file;
            if (!excelFile.endsWith(".xlsx"))
            {
                excelFile += ".xlsx";
            }
        }

        ExcelUtils eu = new ExcelUtils(baMap);
        Map<String, Map<TRId, TR>> excelTrMap = eu.processAllSheets(excelFile, dbIfc.getAccounts());
        Map<String, RealProperty> propMap = dbIfc.getProperties();
        Map<String, Map<TRId, TR>> changedBaMap = eu.importCheck(excelTrMap, propMap);

        if (changedBaMap.size() == 0)
        {
            System.out.println("There are no changes to be committed");
        } else
        {
            System.out.println("\n\nRecords to be committed:");
            for (String bankAccount : changedBaMap.keySet())
            {
                System.out.println("BankAccount=" + bankAccount);
                Map<TRId, TR> changedTrMap = changedBaMap.get(bankAccount);

                for (TR tr : changedTrMap.values())
                {
                    System.out.println("    " + tr);
                }
                System.out.println("Number of changed records=" + changedTrMap.size());
            }
            System.out.println("Import check succeeded.");
            if (commit)
            {
                for (String bankAccount : changedBaMap.keySet())
                {
                    System.out.println("Committing BankAccount=" + bankAccount);
                    Map<TRId, TR> changedTrMap = changedBaMap.get(bankAccount);
                    for (TR tr : changedTrMap.values())
                    {
                        // To Mark that it was manually updated and should not be changed by automatic classification
                        tr.setLocked(true);
                    }
                    int count = dbIfc.updateTransactions(changedTrMap, false);
                    System.out.println("Number of records updated=" + count);
                }

            } else
            {
                System.out.println("Run with -commit to commit the changes in excel.");
            }
        }

    }

    private static DataValidation getPropertyCheckBoxValidation(DataValidationHelper validationHelper,
                                                                int rows) throws DBException
    {
        DBIfc dbIfc = DBFactory.createDBIfc();

        Map<String, RealProperty> map = new TreeMap<String, RealProperty>(dbIfc.getProperties());
        Map<String, IGroup> gMap = dbIfc.getGroupsMap();
        for (String gName : gMap.keySet())
        {
            map.put(gName, null);
        }
        Map<String, Company> compMap = dbIfc.getCompanies();
        for (String compName : compMap.keySet())
        {
            map.put(compName, null);
        }
        String[] allowedTaxCategoryAry = map.keySet().toArray(new String[0]);
        DataValidationConstraint propertyConstraint = validationHelper.createExplicitListConstraint(allowedTaxCategoryAry);

        CellRangeAddressList taxCategoryCellList = new CellRangeAddressList(1, rows + 1, 6, 6);
        DataValidation propDataValidation = validationHelper.createValidation(propertyConstraint, taxCategoryCellList);
        propDataValidation.setSuppressDropDownArrow(true);
        propDataValidation.setShowErrorBox(true);
        propDataValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        // dataValidation.createPromptBox("Title", "Message Text");
        // dataValidation.setShowPromptBox(true);

        propDataValidation.createErrorBox("Box Title", "Please select");

        return propDataValidation;
    }

    private static DataValidation getTaxCategoryCheckBoxValidation(DataValidationHelper validationHelper, int rows)
    {
        Map<String, String> map = new TreeMap<String, String>(AccountsUtil.inst().getAllowedTaxCategories());
        String[] allowedTaxCategoryAry = map.keySet().toArray(new String[0]);
        DataValidationConstraint taxCategoryConstraint = validationHelper.createExplicitListConstraint(allowedTaxCategoryAry);

        CellRangeAddressList taxCategoryCellList = new CellRangeAddressList(1, rows + 1, 5, 5);
        DataValidation taxCategoryDataValidation = validationHelper.createValidation(taxCategoryConstraint, taxCategoryCellList);
        taxCategoryDataValidation.setSuppressDropDownArrow(true);
        taxCategoryDataValidation.setShowErrorBox(true);
        taxCategoryDataValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        // dataValidation.createPromptBox("Title", "Message Text");
        // dataValidation.setShowPromptBox(true);

        taxCategoryDataValidation.createErrorBox("Box Title", "Please select");

        return taxCategoryDataValidation;
    }

    private static DataValidation getTrTypeCheckBoxValidation(DataValidationHelper validationHelper, int rows)
    {
        Map<String, String> map = new TreeMap<String, String>(AccountsUtil.inst().getAllowedTrTypes());
        String[] allowedTrTypesAry = map.keySet().toArray(new String[0]);
        DataValidationConstraint trTypeConstraint = validationHelper.createExplicitListConstraint(allowedTrTypesAry);

        CellRangeAddressList trTypeCellList = new CellRangeAddressList(1, rows + 1, 4, 4);
        DataValidation trTypeDataValidation = validationHelper.createValidation(trTypeConstraint, trTypeCellList);
        trTypeDataValidation.setSuppressDropDownArrow(true);
        trTypeDataValidation.setShowErrorBox(true);
        trTypeDataValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        // dataValidation.createPromptBox("Title", "Message Text");
        // dataValidation.setShowPromptBox(true);

        trTypeDataValidation.createErrorBox("Box Title", "Please select");

        return trTypeDataValidation;
    }

    private static void deleteTrs(String accountName) throws DBException, IOException
    {
        DBIfc dbi = DBFactory.createDBIfc();
        dbi.createAndConnectDB(null);
        BankAccount ba = dbi.getAccounts().get(accountName);
        dbi.deleteTransactions(ba.getTrTableId());
    }

    private static void createCompanySummarySheet(XSSFWorkbook workBook, final Map<String, ArrayList<TR>> companyTrMap,
                                                  final Map<String, ArrayList<TR>> otherTrMap)
    {
        XSSFSheet sheet = workBook.createSheet("CompanySummary");

        Map<String, Map<String, Float>> trTypeTotalMap = new HashMap<String, Map<String, Float>>();

        Set<String> setOfCategories = new LinkedHashSet<>();

        for (final String name : companyTrMap.keySet())
        {
            final Map<String, Float> trTypeMap = trTypeTotal(companyTrMap.get(name));
            trTypeTotalMap.put(name, trTypeMap);
            setOfCategories.addAll(trTypeMap.keySet());
        }
        for (final String name : otherTrMap.keySet())
        {
            final Map<String, Float> trTypeMap = trTypeTotal(otherTrMap.get(name));
            trTypeTotalMap.put(name, trTypeMap);
            setOfCategories.addAll(trTypeMap.keySet());
        }
        List<String> listCompanies = new ArrayList<String>(companyTrMap.keySet());
        Collections.sort(listCompanies);

        List<String> listOtherEntities = new ArrayList<String>(otherTrMap.keySet());
        Collections.sort(listOtherEntities);

        List<String> listCategories = new ArrayList<String>(setOfCategories);
        Collections.sort(listCategories);

        {
            XSSFRow currentRow = sheet.createRow(0);
            {
                Cell cell = currentRow.createCell(0);
                cell.setCellValue("Name");
            }
            int col = 1;
            for (String colName : listCategories)

            {
                Cell cell = currentRow.createCell(col++);
                cell.setCellValue(colName);
            }
        }

        int rowNum = 0;
        for (String compName : listCompanies)
        {
            rowNum++;
            XSSFRow currentRow = sheet.createRow(rowNum);
            {
                Cell cell = null;
                cell = currentRow.createCell(0);
                cell.setCellValue(compName);
            }
            final Map<String, Float> compTrTypeMap = trTypeTotalMap.get(compName);
            int col = 1;
            for (String colName : listCategories)
            {

                Cell cell = null;
                cell = currentRow.createCell(col++);
                if (compTrTypeMap.get(colName) != null)
                    cell.setCellValue(compTrTypeMap.get(colName));
            }

        }
        // Create dummy rows

        for (int i = 0; i < 5; i++)
        {
            rowNum++;
            XSSFRow currentRow = sheet.createRow(rowNum);
        }

        for (String otherName : listOtherEntities)
        {
            rowNum++;
            XSSFRow currentRow = sheet.createRow(rowNum);
            {
                Cell cell = null;
                cell = currentRow.createCell(0);
                cell.setCellValue(otherName);
            }
            final Map<String, Float> otherTrTypeMap = trTypeTotalMap.get(otherName);
            int col = 1;
            for (String colName : listCategories)
            {

                Cell cell = null;
                cell = currentRow.createCell(col++);
                if (otherTrTypeMap.get(colName) != null)
                    cell.setCellValue(otherTrTypeMap.get(colName));
            }

        }

        sheet.setColumnWidth(0, 3000);
        sheet.setColumnWidth(1, 3000);
        sheet.setColumnWidth(2, 3000);
        sheet.setColumnWidth(3, 3000);
        sheet.setColumnWidth(4, 3000);
        sheet.setColumnWidth(5, 3000);
        sheet.setColumnWidth(6, 3000);
        sheet.setColumnWidth(7, 3000);
        sheet.setColumnWidth(8, 3000);
        sheet.setColumnWidth(9, 3000);
        sheet.setColumnWidth(10, 3000);
    }

    private static void createRentalSummarySheet(XSSFWorkbook workBook, Map<String, Float[]> propTable)
    {
        XSSFSheet sheet = workBook.createSheet("RentalSummary");
        {
            XSSFRow currentRow = sheet.createRow(0);

            {
                Cell cell = currentRow.createCell(0);
                cell.setCellValue("Property");
            }
            int col = 1;

            for (int i = 0; i < scheduleEAry.length; i++)
            {
                if (scheduleEAry[i] == null)
                    continue;
                Cell cell = currentRow.createCell(col++);
                cell.setCellValue(i + " " + scheduleEAry[i]);
            }
            Cell cell = currentRow.createCell(col++);
            cell.setCellValue("Profit");
        }

        int rowNum = 0;
        for (String propName : propTable.keySet())
        {
            rowNum++;
            XSSFRow currentRow = sheet.createRow(rowNum);
            Float[] values = propTable.get(propName);
            {
                Cell cell = currentRow.createCell(0);
                cell.setCellValue(propName);
            }
            float totalProfit = 0;

            int col = 1;
            for (int i = 0; i < scheduleEAry.length; i++)
            {
                if (scheduleEAry[i] == null)
                    continue;
                Cell cell = currentRow.createCell(col++);
                if (values[i] != null)
                {
                    totalProfit += values[i];
                    cell.setCellValue(values[i]);
                }
            }
            Cell cell = currentRow.createCell(col++);
            cell.setCellValue(totalProfit);

        }
        sheet.setColumnWidth(0, 3000);
        sheet.setColumnWidth(1, 3000);
        sheet.setColumnWidth(2, 3000);
        sheet.setColumnWidth(3, 3000);
        sheet.setColumnWidth(4, 3000);
        sheet.setColumnWidth(5, 3000);
        sheet.setColumnWidth(6, 3000);
        sheet.setColumnWidth(7, 3000);
        sheet.setColumnWidth(8, 3000);
        sheet.setColumnWidth(9, 3000);
        sheet.setColumnWidth(10, 3000);
        sheet.setColumnWidth(11, 3000);
    }

    private static void exportToExcel(Map<String, Float[]> propTable, final Map<String, ArrayList<TR>> companyTrMap,
                                      final Map<String, ArrayList<TR>> otherTrMap, String accountName, String file,
                                      String filter) throws DBException, IOException
    {
        DBIfc dbIfc = DBFactory.createDBIfc();

        dbIfc.createAndConnectDB(null);

        if (accountName != null)
        {
            accountName = accountName.toLowerCase().trim();
            if (!dbIfc.getAccounts().containsKey(accountName))
            {
                throw new IOException("Account is not present: " + accountName + ", List=" + dbIfc.getAccounts().keySet());
            }
        }
        AccountsUtil.createInstance(); // Initialization

        XSSFWorkbook workBook = new XSSFWorkbook();
        CellStyle unlockedCellStyle = workBook.createCellStyle();
        unlockedCellStyle.setLocked(false);
        unlockedCellStyle.setVerticalAlignment(CellStyle.VERTICAL_TOP);
        unlockedCellStyle.setWrapText(true);

        CellStyle wrapAlignCellStyle = workBook.createCellStyle();
        wrapAlignCellStyle.setWrapText(true);
        wrapAlignCellStyle.setVerticalAlignment(CellStyle.VERTICAL_TOP);
        CellStyle topAlignCellStyle = workBook.createCellStyle();
        topAlignCellStyle.setVerticalAlignment(CellStyle.VERTICAL_TOP);
        createRentalSummarySheet(workBook, propTable);
        createCompanySummarySheet(workBook, companyTrMap, otherTrMap);

        Map<String, BankAccount> baMap = new TreeMap<String, BankAccount>(dbIfc.getAccounts());

        for (BankAccount ba : baMap.values())
        {
            if (accountName != null && !ba.getName().equalsIgnoreCase(accountName))
            {
                continue;
            }
            String baName = ba.getName();

            XSSFSheet sheet = workBook.createSheet(baName);
            {
                XSSFRow currentRow = sheet.createRow(0);
                int col = 0;
                Cell cell = null;
                cell = currentRow.createCell(col++);
                cell.setCellValue("Date");
                cell = currentRow.createCell(col++);
                cell.setCellValue("Description");
                cell = currentRow.createCell(col++);
                cell.setCellValue("Debit");
                cell = currentRow.createCell(col++);
                cell.setCellValue("Comment");
                cell = currentRow.createCell(col++);
                cell.setCellValue("Transaction Type");
                cell.setCellStyle(unlockedCellStyle);
                cell = currentRow.createCell(col++);
                cell.setCellValue("Tax Category");
                cell.setCellStyle(unlockedCellStyle);
                cell = currentRow.createCell(col++);
                cell.setCellValue("Property or Company");
                cell.setCellStyle(unlockedCellStyle);
                cell = currentRow.createCell(col++);
                cell.setCellValue("OtherEntity");
                cell.setCellStyle(unlockedCellStyle);
                cell = currentRow.createCell(col++);
                cell.setCellValue("ManuallyUpdated");
                cell.setCellStyle(unlockedCellStyle);
            }

            CellStyle dateCellStyle = workBook.createCellStyle();
            dateCellStyle.setDataFormat(workBook.getCreationHelper().createDataFormat().getFormat("MM/dd/yyyy"));
            dateCellStyle.setVerticalAlignment(CellStyle.VERTICAL_TOP);

            Map<TRId, TR> trMap = dbIfc.getTransactions(ba.getTrTableId());

            int RowNum = 0;
            for (TR tr : trMap.values())
            {
                if (filter != null)
                {
                    if (filter.equals(tr.getTrType()))
                    {
                        continue;
                    }
                }
                RowNum++;
                XSSFRow currentRow = sheet.createRow(RowNum);
                int col = 0;
                Cell cell = null;
                cell = currentRow.createCell(col++);
                cell.setCellType(Cell.CELL_TYPE_NUMERIC);
                cell.setCellValue(tr.getDate());
                cell.setCellStyle(dateCellStyle);

                cell = currentRow.createCell(col++);
                cell.setCellType(Cell.CELL_TYPE_STRING);
                cell.setCellValue(tr.getDescription());
                cell.setCellStyle(wrapAlignCellStyle);

                cell = currentRow.createCell(col++);
                cell.setCellType(Cell.CELL_TYPE_NUMERIC);
                cell.setCellValue(tr.getDebit());
                cell.setCellStyle(topAlignCellStyle);

                cell = currentRow.createCell(col++);
                cell.setCellValue(tr.getComment());
                cell.setCellType(Cell.CELL_TYPE_STRING);
                cell.setCellStyle(unlockedCellStyle);

                cell = currentRow.createCell(col++);
                cell.setCellValue(tr.getTrType());
                cell.setCellStyle(unlockedCellStyle);

                cell = currentRow.createCell(col++);
                cell.setCellValue(tr.getTaxCategory());
                cell.setCellStyle(unlockedCellStyle);

                cell = currentRow.createCell(col++);
                cell.setCellValue(tr.getProperty());
                cell.setCellStyle(unlockedCellStyle);

                cell = currentRow.createCell(col++);
                cell.setCellValue(tr.getOtherEntity());
                cell.setCellStyle(unlockedCellStyle);

                cell = currentRow.createCell(col++);
                if (tr.isLocked())
                {
                    cell.setCellValue("YES");
                }
                cell.setCellStyle(unlockedCellStyle);

            }
            DataValidationHelper validationHelper = new XSSFDataValidationHelper(sheet);
            DataValidation trTypeDataValidation = getTrTypeCheckBoxValidation(validationHelper, trMap.size());

            sheet.addValidationData(trTypeDataValidation);

            DataValidation taxCategoryDataValidation = getTaxCategoryCheckBoxValidation(validationHelper, trMap.size());
            sheet.addValidationData(taxCategoryDataValidation);

            DataValidation propertyDataValidation = getPropertyCheckBoxValidation(validationHelper, trMap.size());
            sheet.addValidationData(propertyDataValidation);

            sheet.setAutoFilter(CellRangeAddress.valueOf("A1:N1"));
            sheet.lockDeleteColumns(true);
            sheet.lockDeleteRows(true);
            // sheet.lockFormatCells(true);
            // sheet.lockFormatColumns(true);
            // sheet.lockFormatRows(true);
            sheet.lockInsertColumns(true);
            sheet.lockInsertRows(true);
            sheet.lockAutoFilter(false);

            sheet.setColumnWidth(0, 3000);
            sheet.setColumnWidth(1, 14000);
            sheet.setColumnWidth(2, 3000);
            sheet.setColumnWidth(3, 14000);
            sheet.setColumnWidth(4, 7000);
            sheet.setColumnWidth(5, 4000);
            sheet.setColumnWidth(6, 6000);
            sheet.setColumnWidth(7, 6000);
            sheet.protectSheet("password");

            // Locks the whole sheet sheet.enableLocking();

        }
        workBook.lockStructure();
        String outFile = null;
        if (file == null)
        {
            String dir = System.getProperty("ACCOUNTSDB") + File.separator + "exporttr";
            File dirFile = new File(dir);
            if (!dirFile.isDirectory())
            {
                dirFile.mkdir();
            }
            outFile = dir + File.separator + "export_allaccounts.xlsx";
        }
        File outFileHandle = new File(outFile);
        if (outFileHandle.exists())
        {
            DateFormat df = new SimpleDateFormat("MMddyyyy_HHmmss");
            Date today = Calendar.getInstance().getTime();
            String reportDate = df.format(today);
            outFileHandle.renameTo(new File(outFile + "_" + reportDate + "_old.xlsx"));
        }

        FileOutputStream fileOutputStream = new FileOutputStream(outFile);
        workBook.write(fileOutputStream);
        fileOutputStream.close();
        System.out.println("Exported transactions to=" + outFile);

    }

    private static void exportToCsv(String accountName, String file) throws DBException, IOException
    {
        DBIfc dbIfc = DBFactory.createDBIfc();

        dbIfc.createAndConnectDB(null);

        if (accountName == null && file != null)
        {
            throw new IOException("Account cannot be null when file name is specified ");
        }
        if (accountName != null)
        {
            accountName = accountName.toLowerCase().trim();
            if (!dbIfc.getAccounts().containsKey(accountName))
            {
                throw new IOException("Account is not present: " + accountName + ", List=" + dbIfc.getAccounts().keySet());
            }
        }
        for (BankAccount ba : dbIfc.getAccounts().values())
        {
            if (accountName != null && !ba.getName().equalsIgnoreCase(accountName))
            {
                continue;
            }

            Map<TRId, TR> trMap = dbIfc.getTransactions(ba.getTrTableId());
            StringBuffer sb = new StringBuffer();
            sb.append("#DATE,DESCRIPTION,DEBIT,COMMENT,ISLOCKED,TRTYPE,TAXCATEGORY,PROPERTY\n");
            for (final TR tr : trMap.values())
            {
                sb.append(tr.toCsv() + "\n");
            }
            String outfile = null;
            if (file == null)
            {
                String dir = System.getProperty("ACCOUNTSDB") + File.separator + "exporttr";
                File dirFile = new File(dir);
                if (!dirFile.isDirectory())
                {
                    dirFile.mkdir();
                }
                outfile = dir + File.separator + "export_" + ba.getName() + ".csv";
            } else
            {
                outfile = file;
                if (!outfile.endsWith(".csv"))
                {
                    outfile += ".csv";
                }
            }
            PrintWriter out = new PrintWriter(outfile);
            out.println(sb);
            out.close();
        }
        System.out.println("Export transactions completed.");
    }

    private static List<BankAccount> importMultiple2DB(final String dir, boolean commit) throws ParseException, IOException,
                                                                                         DBException
    {
        DBIfc dbIfc = DBFactory.createDBIfc();

        dbIfc.createAndConnectDB(null);

        List<BankAccount> foundStmts = new ArrayList<BankAccount>();

        for (BankAccount ba : dbIfc.getAccounts().values())
        {
            boolean found = false;
            File stmtFile = new File(dir + File.separator + ba.getName() + ".csv");
            if (stmtFile.isFile())
            {
                found = true;
                final BankStatement bs = new BankStatement(stmtFile.getAbsolutePath(), ba.getName(), null);

                checkAndCommit(bs, commit);
            }

            File stmtAddendumFile = new File(dir + File.separator + ba.getName() + "_addendum.csv");
            if (stmtAddendumFile.isFile())
            {
                found = true;
                final BankStatement bs = new BankStatement(stmtAddendumFile.getAbsolutePath(), ba.getName(), null);

                checkAndCommit(bs, commit);
            }
            if (found)
                foundStmts.add(ba);
        }
        return foundStmts;
    }

    private static DBIfc classifyindb(final TaxConfig tc) throws DBException, IOException
    {
        DBIfc dbIfc = DBFactory.createDBIfc();

        dbIfc.createAndConnectDB(null);
        for (BankAccount ba : dbIfc.getAccounts().values())
        {
            Map<TRId, TR> trMap = dbIfc.getTransactions(ba.getTrTableId());

            final ArrayList<RuleRecord> arr = tc.getAccountsMap().get(ba.getName());
            if (arr == null)
            {
                continue;
            }
            for (final TR tr : trMap.values())
            {
                for (final RuleRecord rr : arr)
                {
                    String propertyInRR = rr.getProperty();
                    String otherEntityInRR = rr.getProperty();
                    String taxCategoryInRR = rr.getProperty();
                    String trTypeInRR = rr.getProperty();
                    if (propertyInRR == null)
                        propertyInRR = "";
                    if (otherEntityInRR == null)
                        otherEntityInRR = "";
                    if (taxCategoryInRR == null)
                        taxCategoryInRR = "";
                    if (trTypeInRR == null)
                        trTypeInRR = "";

                    if (rr.getDescContains() != null)
                    {
                        if (tr.getDescription().contains(rr.getDescContains()))
                        {
                            tr.setProperty(propertyInRR);
                            tr.setOtherEntity(otherEntityInRR);
                            tr.setTaxCategory(taxCategoryInRR);
                            tr.setTrType(trTypeInRR);
                            break;
                        }
                    } else if (rr.getDescStartsWith() != null)
                    {
                        if (tr.getDescription().startsWith(rr.getDescStartsWith()))
                        {
                            tr.setProperty(propertyInRR);
                            tr.setOtherEntity(otherEntityInRR);
                            tr.setTaxCategory(taxCategoryInRR);
                            tr.setTrType(trTypeInRR);
                            break;
                        }
                    } else if (rr.getDebitEquals() != 0)
                    {
                        if (tr.getDebit() == rr.getDebitEquals())
                        {
                            tr.setProperty(propertyInRR);
                            tr.setOtherEntity(otherEntityInRR);
                            tr.setTaxCategory(taxCategoryInRR);
                            tr.setTrType(trTypeInRR);
                            break;
                        }
                    }
                }
            }
            dbIfc.updateTransactions(trMap, true);
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
                    String propertyInRR = rr.getProperty();
                    String otherEntityInRR = rr.getProperty();
                    String taxCategoryInRR = rr.getProperty();
                    String trTypeInRR = rr.getProperty();
                    if (propertyInRR == null)
                        propertyInRR = "";
                    if (otherEntityInRR == null)
                        otherEntityInRR = "";
                    if (taxCategoryInRR == null)
                        taxCategoryInRR = "";
                    if (trTypeInRR == null)
                        trTypeInRR = "";

                    if (rr.getDescContains() != null)
                    {
                        if (tr.getDescription().contains(rr.getDescContains()))
                        {
                            tr.setProperty(propertyInRR);
                            tr.setOtherEntity(otherEntityInRR);
                            tr.setTaxCategory(taxCategoryInRR);
                            tr.setTrType(trTypeInRR);
                            break;
                        }
                    } else if (rr.getDescStartsWith() != null)
                    {
                        if (tr.getDescription().startsWith(rr.getDescStartsWith()))
                        {
                            tr.setProperty(propertyInRR);
                            tr.setOtherEntity(otherEntityInRR);
                            tr.setTaxCategory(taxCategoryInRR);
                            tr.setTrType(trTypeInRR);
                            break;
                        }
                    }
                }

            }
        }

    }

    public static Map<TRId, TR> import2DBCheck(BankStatement bs) throws DBException
    {
        DBIfc dbIfc = DBFactory.createDBIfc();

        Map<TRId, TR> newTrList = new HashMap<>();
        Map<TRId, TR> inputTrMap = bs.getTrs();
        Map<TRId, TR> dbTrMap = dbIfc.getTransactions(bs.getBankAccount().getTrTableId());
        for (TRId trId : inputTrMap.keySet())
        {
            if (dbTrMap.containsKey(trId))
            {
                // It is already in database.
                continue;
            }
            newTrList.put(trId, inputTrMap.get(trId));
        }
        return newTrList;
    }

    public static void checkAndCommit(BankStatement bs, boolean commit) throws DBException
    {
        DBIfc dbIfc = DBFactory.createDBIfc();
        Map<TRId, TR> newTrList = import2DBCheck(bs);
        if (newTrList.size() == 0)
        {
            System.out.println("No new transactions found in the input statement. Hence no changes to be committed. Bank="
                    + bs.getBankAccount().getName());
        } else
        {
            System.out.println("\n\nRecords in the input for " + bs.getBankAccount().getName() + " :");

            for (TR tr : newTrList.values())
            {
                System.out.println("    " + tr);
            }
            System.out.println("Import check succeeded. Number of entries in input=" + newTrList.size());
            if (commit)
            {
                int count = dbIfc.updateTransactions(newTrList, true);
                System.out.println("Committed transaction count=" + count);
            } else
            {

                System.out.println("Run with -commit to commit the changes.");
            }
        }

    }

    public static void usage(final String err)
    {
        if (err != null && !"".equals(err))
        {
            System.out.println(err);
        }
        System.out.println("Usage: -A action options\n");
        System.out.println("    -A createacs -file <csv>");
        System.out.println("    -A listacs");
        System.out.println("    -A deleteacs -file <csv>\n");
        System.out.println("    -A createprops -file <csv>");
        System.out.println("    -A listprops");
        System.out.println("    -A DELETEPROPS -file <csv>\n");
        System.out.println("    -A creategroups -file <csv>");
        System.out.println("    -A deletegroups -file <csv>\n");

        System.out.println("    -A createcompanies -file <csv>");
        System.out.println("    -A listcompanies");
        System.out.println("    -A DELETECOMPANIES -file <csv>\n");

        System.out.println("    -A deletetrs -accountname <n>\n");
        System.out.println("    (Unit test only)-A parse -bankstatement <csvfile> -accountname <n> [-bankstformat <f>]\n");
        System.out.println(
                "    (unit test only)-A parseandclassify -bankstatement <csvfile> -accountname <n> -taxconfig <f> [-bankstformat <f> ]\n");
        System.out.println("    -A import2db {-dir <dir> | {-bankstatement <csv> -accountname <n>} } [-commit]");
        System.out.println("        dir contains csv files with name accountname.csv or accountname_addendum.csv");
        System.out.println("        Recommended to keep all statements under $ACCOUNTSDB/bank_stmts directory\n");
        System.out.println("    -A classifyindb -taxconfig <f> -year <yyyy>\n");
        System.out.println("    -A exp2excel -year <yyyy> [-accountname <n>] [-file <f.xlsx>] [-filter \"tr types\"]\n");
        System.out.println("    -A impexcel [-commit] [-accountname <n>] [-file <f.xlsx>]\n");
        System.out.println("    -A classify_exp -taxconfig <f> -year <yyyy>\n");

        System.exit(1);
    }

    public static final String PARSE            = "parse";
    public static final String IMPORT2DB        = "import2db";
    public static final String PARSEANDCLASSIFY = "parseandclassify";
    public static final String CLASSIFYINDB     = "classifyindb";
    public static final String EXP2EXCEL        = "exp2excel";
    public static final String IMPEXCEL         = "impexcel";
    public static final String CLASSIFY_EXP     = "classify_exp";

    public static final String DELETETRS = "deletetrs";

    public static final String              CREATEACS       = "createacs";
    public static final String              LISTACS         = "listacs";
    public static final String              DELETEACS       = "deleteacs";
    public static final String              CREATEPROPS     = "createprops";
    public static final String              LISTPROPS       = "listprops";
    public static final String              DELETEPROPS     = "deleteprops";
    public static final String              CREATEGROUPS    = "creategroups";
    public static final String              LISTGROUPS      = "listgroups";
    public static final String              DELETEGROUPS    = "deletegroups";
    public static final String              CREATECOMPANIES = "createcompanies";
    public static final String              LISTCOMPANIES   = "listcompanies";
    public static final String              DELETECOMPANIES = "deletecompanies";
    public static final Map<String, String> ALL_OPTS        = new HashMap<String, String>();

    static
    {
        ALL_OPTS.put("A", Getopt.CONTRNT_S);
        ALL_OPTS.put("bankstatement", Getopt.CONTRNT_S);
        ALL_OPTS.put("bankstformat", Getopt.CONTRNT_S);
        ALL_OPTS.put("accountname", Getopt.CONTRNT_S);
        ALL_OPTS.put("taxconfig", Getopt.CONTRNT_S);
        ALL_OPTS.put("year", Getopt.CONTRNT_I);
        ALL_OPTS.put("file", Getopt.CONTRNT_S);
        ALL_OPTS.put("dir", Getopt.CONTRNT_S);
        ALL_OPTS.put("commit", Getopt.CONTRNT_NOARG);
        ALL_OPTS.put("filter", Getopt.CONTRNT_S);

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
                List<BankAccount> acL = DBImpl.parseAccountFile(argHash.get("file"));
                DBIfc dbi = DBFactory.createDBIfc();
                dbi.createAndConnectDB(null);
                int initialCount = dbi.getAccounts().size();
                for (BankAccount ac : acL)
                {
                    if (dbi.getAccounts() != null && dbi.getAccounts().containsKey(ac.getName()))
                    {
                        System.out.println("Account is already present=" + ac.getName());
                    } else
                    {
                        dbi.createBankAccount(ac.getName(), ac.getBankName());
                    }
                }
                for (BankAccount ba : dbi.getAccounts().values())
                {
                    System.out.println(ba);
                }
                System.out.println("Number of accounts created=" + (dbi.getAccounts().size() - initialCount));
            } else if (LISTACS.equalsIgnoreCase(action))
            {
                DBIfc dbi = DBFactory.createDBIfc();
                dbi.createAndConnectDB(null);
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
                List<BankAccount> acL = DBImpl.parseAccountFile(argHash.get("file"));
                DBIfc dbi = DBFactory.createDBIfc();
                dbi.createAndConnectDB(null);
                int initialCount = dbi.getAccounts().size();
                for (BankAccount ac : acL)
                {
                    dbi.deleteBankAccount(ac.getName());
                }
                for (BankAccount ba : dbi.getAccounts().values())
                {
                    System.out.println(ba);
                }
                System.out.println("Number of accounts deleted=" + (initialCount - dbi.getAccounts().size()));
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
            } else if (LISTPROPS.equalsIgnoreCase(action))
            {
                DBIfc dbi = DBFactory.createDBIfc();
                dbi.createAndConnectDB(null);
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
                dbi.createAndConnectDB(null);
                for (IGroup rp : rpL)
                {
                    dbi.createGroup(rp);
                }
                for (IGroup rp1 : dbi.getGroupsMap().values())
                {
                    System.out.println(rp1);
                }
            } else if (LISTGROUPS.equalsIgnoreCase(action))
            {
                DBIfc dbi = DBFactory.createDBIfc();
                dbi.createAndConnectDB(null);
                for (IGroup rp1 : dbi.getGroupsMap().values())
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
                dbi.createAndConnectDB(null);
                for (IGroup rp : rpL)
                {
                    dbi.deleteGroup(rp.getName());
                }
                for (IGroup rp1 : dbi.getGroupsMap().values())
                {
                    System.out.println(rp1);
                }
            } else if (CREATECOMPANIES.equalsIgnoreCase(action))
            {
                if (argHash.get("file") == null)
                {
                    usage("-file argument is required.");
                }
                List<Company> rpL = DBImpl.parseCompanyFile(argHash.get("file"));
                DBIfc dbi = DBFactory.createDBIfc();
                dbi.createAndConnectDB(null);
                for (Company rp : rpL)
                {
                    dbi.createCompany(rp);
                }
                for (Company rp1 : dbi.getCompanies().values())
                {
                    System.out.println(rp1);
                }
                System.out.println("Successfully created companies.");
            } else if (LISTCOMPANIES.equalsIgnoreCase(action))
            {
                DBIfc dbi = DBFactory.createDBIfc();
                dbi.createAndConnectDB(null);
                for (Company rp1 : dbi.getCompanies().values())
                {
                    System.out.println(rp1);
                }
            } else if (DELETECOMPANIES.equalsIgnoreCase(action))
            {
                if (argHash.get("file") == null)
                {
                    usage("-file argument is required.");
                }
                List<Company> rpL = DBImpl.parseCompanyFile(argHash.get("file"));
                DBIfc dbi = DBFactory.createDBIfc();
                dbi.createAndConnectDB(null);
                for (Company rp : rpL)
                {
                    dbi.deleteCompany(rp.getName());
                }
                for (Company rp1 : dbi.getCompanies().values())
                {
                    System.out.println(rp1);
                }
            } else if (PARSE.equalsIgnoreCase(action))
            {
                final BankStatement bs = new BankStatement(argHash.get("bankstatement"), argHash.get("accountname"),
                        argHash.get("bankstformat"));
                System.out.println("" + bs);

            } else if (IMPORT2DB.equalsIgnoreCase(action))
            {
                if (argHash.get("bankstatement") != null && argHash.get("dir") != null)
                {
                    usage("options -bankstatement and -dir are mutually exclusive\n"
                            + "\nWhen -dir -s used the name of the statement files should match the bank name.");
                }
                if (argHash.get("dir") != null)
                {
                    List<BankAccount> foundList = importMultiple2DB(argHash.get("dir"), argHash.get("commit") != null);
                    System.out.println("FoundStatements for banks=" + foundList);
                    DBIfc dbi = DBFactory.createDBIfc();
                    System.out.println("Number of bank accounts in DB=" + dbi.getAccounts().size());
                    System.out.println("Number of bank statements imported=" + foundList.size());

                } else
                {
                    final BankStatement bs = new BankStatement(argHash.get("bankstatement"), argHash.get("accountname"),
                            argHash.get("bankstformat"));

                    checkAndCommit(bs, argHash.get("commit") != null);
                }

            } else if (PARSEANDCLASSIFY.equalsIgnoreCase(action))
            {
                final BankStatement bs = new BankStatement(argHash.get("bankstatement"), argHash.get("accountname"),
                        argHash.get("bankstformat"));
                final TaxConfig tc = new TaxConfig(argHash.get("taxconfig"));
                classify(bs, tc);
                System.out.println("" + bs.toString(true));
                StringBuffer sb = report(2014, bs.getTrs());
                System.out.println("" + sb);

            } else if (CLASSIFYINDB.equalsIgnoreCase(action))
            {
                if (argHash.get("taxconfig") == null)
                {
                    usage("-taxconfig argument is required.");
                }
                if (argHash.get("year") == null)
                {
                    usage("-year argument is required.");
                }
                final TaxConfig tc = new TaxConfig(argHash.get("taxconfig"));
                DBIfc dbIfc = classifyindb(tc);
                Map<String, Float[]> propTable = new TreeMap<String, Float[]>();

                final Map<String, ArrayList<TR>> companyTrMap = new HashMap<String, ArrayList<TR>>();
                final Map<String, ArrayList<TR>> otherTrMap = new HashMap<String, ArrayList<TR>>();
                StringBuffer sb = report(new Integer(argHash.get("year")).intValue(), dbIfc, propTable, companyTrMap, otherTrMap);
                if (sb.length() == 0)
                {
                    System.out.println("Database is empty");
                } else
                {
                    System.out.println("" + sb);
                }

            } else if (EXP2EXCEL.equalsIgnoreCase(action))
            {
                if (argHash.get("year") == null)
                {
                    usage("-year argument is required.");
                }
                DBIfc dbIfc = DBFactory.createDBIfc();
                dbIfc.createAndConnectDB(null);
                Map<String, Float[]> propTable = new TreeMap<String, Float[]>();
                final Map<String, ArrayList<TR>> companyTrMap = new HashMap<String, ArrayList<TR>>();
                final Map<String, ArrayList<TR>> otherTrMap = new HashMap<String, ArrayList<TR>>();
                StringBuffer sb = report(new Integer(argHash.get("year")).intValue(), dbIfc, propTable, companyTrMap, otherTrMap);
                if (sb.length() == 0)
                {
                    System.out.println("Database is empty");
                } else
                {
                    System.out.println("" + sb);
                }
                exportToExcel(propTable, companyTrMap, otherTrMap, argHash.get("accountname"), argHash.get("file"),
                        argHash.get("filter"));

            } else if (IMPEXCEL.equalsIgnoreCase(action))
            {
                importFromExcel(argHash.get("accountname"), argHash.get("file"), argHash.get("commit") != null);

            } else if (CLASSIFY_EXP.equalsIgnoreCase(action))
            {
                if (argHash.get("taxconfig") == null)
                {
                    usage("-taxconfig argument is required.");
                }
                if (argHash.get("year") == null)
                {
                    usage("-year argument is required.");
                }
                final TaxConfig tc = new TaxConfig(argHash.get("taxconfig"));
                DBIfc dbIfc = classifyindb(tc);
                Map<String, Float[]> propTable = new TreeMap<String, Float[]>();
                final Map<String, ArrayList<TR>> companyTrMap = new HashMap<String, ArrayList<TR>>();
                final Map<String, ArrayList<TR>> otherTrMap = new HashMap<String, ArrayList<TR>>();
                StringBuffer sb = report(new Integer(argHash.get("year")).intValue(), dbIfc, propTable, companyTrMap, otherTrMap);
                if (sb.length() == 0)
                {
                    System.out.println("Database is empty");
                } else
                {
                    System.out.println("" + sb);
                }
                exportToExcel(propTable, companyTrMap, otherTrMap, argHash.get("accountname"), argHash.get("file"),
                        argHash.get("filter"));

            } else if (DELETETRS.equalsIgnoreCase(action))
            {
                if (argHash.get("accountname") == null)
                {
                    usage("-accountname argument is required.");
                }
                deleteTrs(argHash.get("accountname"));
                System.out.println("Delete transactions completed");

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
        } catch (AccountExp e)
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
