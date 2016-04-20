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
import java.util.List;
import java.util.Map;
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

    private static StringBuffer reportFromPropMap(final Map<String, ArrayList<TR>> propTrMap,
                                                  final Map<String, ArrayList<TR>> groupTrMap,
                                                  final Map<String, RealProperty> propertyMap,
                                                  final Map<String, IGroup> groupsMap)
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
                        sb.append(
                                "    " + i + " " + scheduleEAry[i] + "=" + (trTypeMap.get(scheduleEAry[i]) / ownerCount) + "\n");
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

            if (trTypeMap.containsKey(BANKFEES))
            {
                // include everything else in the other category
                other = (trTypeMap.get(BANKFEES) / ownerCount);
                totalExpense += (trTypeMap.get(BANKFEES) / ownerCount);
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

    private static StringBuffer report(final int year, final Map<TRId, TR> trMap)
    {
        final Map<String, ArrayList<TR>> propTrMap = new HashMap<String, ArrayList<TR>>();
        final Map<String, ArrayList<TR>> grpTrMap = new HashMap<String, ArrayList<TR>>();
        Map<String, IGroup> dummyGrpMap = new HashMap<String, IGroup>();
        addToPropertyMap(year, trMap, dummyGrpMap, propTrMap, grpTrMap);
        return reportFromPropMap(propTrMap, grpTrMap, null, null);

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
            int count = dbIfc.updateTransactions(trs);
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
            }
            System.out.println("Import check succeeded.");
            if (commit)
            {
                for (String bankAccount : changedBaMap.keySet())
                {
                    System.out.println("Committing BankAccount=" + bankAccount);
                    Map<TRId, TR> changedTrMap = changedBaMap.get(bankAccount);
                    int count = dbIfc.updateTransactions(changedTrMap);
                }

            } else
            {
                System.out.println("Run with -commit to commit the changes in excel.");
            }
        }

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

    private static void exportToExcel(String accountName, String file, String filter) throws DBException, IOException
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

        Map<String, BankAccount> baMap = dbIfc.getAccounts();

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
                cell.setCellValue("Property");
                cell.setCellStyle(unlockedCellStyle);
                cell = currentRow.createCell(col++);
                cell.setCellValue("OtherEntity");
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

            }
            DataValidationHelper validationHelper = new XSSFDataValidationHelper(sheet);
            DataValidation trTypeDataValidation = getTrTypeCheckBoxValidation(validationHelper, trMap.size());

            sheet.addValidationData(trTypeDataValidation);

            DataValidation taxCategoryDataValidation = getTaxCategoryCheckBoxValidation(validationHelper, trMap.size());
            sheet.addValidationData(taxCategoryDataValidation);
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
                    if (rr.getDescContains() != null)
                    {
                        if (tr.getDescription().contains(rr.getDescContains()))
                        {
                            tr.setProperty(rr.getProperty());
                            tr.setTaxCategory(rr.getTaxCategory());
                            tr.setTrType(rr.getTrType());
                            break;
                        }
                    } else if (rr.getDescStartsWith() != null)
                    {
                        if (tr.getDescription().startsWith(rr.getDescStartsWith()))
                        {
                            tr.setProperty(rr.getProperty());
                            tr.setTaxCategory(rr.getTaxCategory());
                            tr.setTrType(rr.getTrType());
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
                            tr.setTrType(rr.getTrType());
                            break;
                        }
                    } else if (rr.getDescStartsWith() != null)
                    {
                        if (tr.getDescription().startsWith(rr.getDescStartsWith()))
                        {
                            tr.setProperty(rr.getProperty());
                            tr.setTaxCategory(rr.getTaxCategory());
                            tr.setTrType(rr.getTrType());
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
        Map<TRId, TR> inputTrList = bs.getTrs();
        Map<TRId, TR> dbTrList = dbIfc.getTransactions(bs.getBankAccount().getTrTableId());
        for (TRId trId : inputTrList.keySet())
        {
            if (dbTrList.containsKey(trId))
            {
                // It is already in database.
                continue;
            }
            newTrList.put(trId, inputTrList.get(trId));
        }
        return newTrList;
    }

    public static void checkAndCommit(BankStatement bs, boolean commit) throws DBException
    {
        DBIfc dbIfc = DBFactory.createDBIfc();
        Map<TRId, TR> newTrList = import2DBCheck(bs);
        if (newTrList.size() == 0)
        {
            System.out.println("No new transactions found in the input statement. Hence changes to be committed.");
        } else
        {
            System.out.println("\n\nRecords to be committed:");

            for (TR tr : newTrList.values())
            {
                System.out.println("    " + tr);
            }
            System.out.println("Import check succeeded.");
            if (commit)
            {
                int count = dbIfc.updateTransactions(newTrList);
                System.out.println("updated transaction count=" + count);
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
        System.out.println("    -A parse -bankstatement <csvfile> -accountname <n> [-bankstformat <f>]\n");
        System.out.println(
                "    -A parseandclassify -bankstatement <csvfile> -accountname <n> -taxconfig <f> [-bankstformat <f> ]\n");
        System.out.println("    -A import2db -bankstatement <csvfile> -accountname <n> [-commit]\n");
        System.out.println("    -A classifyindb -taxconfig <f> -year <yyyy>\n");
        System.out.println("    -A exp2excel [-accountname <n>] [-file <f.xlsx>] [-filter \"tr types\"]\n");
        System.out.println("    -A impexcel [-commit] [-accountname <n>] [-file <f.xlsx>]\n");
        System.exit(1);
    }

    public static final String PARSE            = "parse";
    public static final String IMPORT2DB        = "import2db";
    public static final String PARSEANDCLASSIFY = "parseandclassify";
    public static final String CLASSIFYINDB     = "classifyindb";
    public static final String EXP2EXCEL        = "exp2excel";
    public static final String IMPEXCEL         = "impexcel";

    public static final String              CREATEACS    = "createacs";
    public static final String              LISTACS      = "listacs";
    public static final String              DELETEACS    = "deleteacs";
    public static final String              CREATEPROPS  = "createprops";
    public static final String              LISTPROPS    = "listprops";
    public static final String              DELETEPROPS  = "deleteprops";
    public static final String              CREATEGROUPS = "creategroups";
    public static final String              LISTGROUPS   = "listgroups";
    public static final String              DELETEGROUPS = "deletegroups";
    public static final Map<String, String> ALL_OPTS     = new HashMap<String, String>();

    static
    {
        ALL_OPTS.put("A", Getopt.CONTRNT_S);
        ALL_OPTS.put("bankstatement", Getopt.CONTRNT_S);
        ALL_OPTS.put("bankstformat", Getopt.CONTRNT_S);
        ALL_OPTS.put("accountname", Getopt.CONTRNT_S);
        ALL_OPTS.put("taxconfig", Getopt.CONTRNT_S);
        ALL_OPTS.put("year", Getopt.CONTRNT_I);
        ALL_OPTS.put("file", Getopt.CONTRNT_S);
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
                for (IGroup rp1 : dbi.getGropusMap().values())
                {
                    System.out.println(rp1);
                }
            } else if (LISTGROUPS.equalsIgnoreCase(action))
            {
                DBIfc dbi = DBFactory.createDBIfc();
                dbi.createAndConnectDB(null);
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
                dbi.createAndConnectDB(null);
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
                final BankStatement bs = new BankStatement(argHash.get("bankstatement"), argHash.get("accountname"),
                        argHash.get("bankstformat"));
                System.out.println("" + bs);

            } else if (IMPORT2DB.equalsIgnoreCase(action))
            {
                final BankStatement bs = new BankStatement(argHash.get("bankstatement"), argHash.get("accountname"),
                        argHash.get("bankstformat"));

                checkAndCommit(bs, argHash.get("commit") != null);
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
                StringBuffer sb = report(new Integer(argHash.get("year")).intValue(), dbIfc);
                if (sb.length() == 0)
                {
                    System.out.println("Database is empty");
                } else
                {
                    System.out.println("" + sb);
                }

            } else if (EXP2EXCEL.equalsIgnoreCase(action))
            {
                exportToExcel(argHash.get("accountname"), argHash.get("file"), argHash.get("filter"));

            } else if (IMPEXCEL.equalsIgnoreCase(action))
            {
                importFromExcel(argHash.get("accountname"), argHash.get("file"), argHash.get("commit") != null);

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
