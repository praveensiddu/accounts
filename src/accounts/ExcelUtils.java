package accounts;

/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import accounts.db.BankAccount;
import accounts.db.DBException;
import accounts.db.DBFactory;
import accounts.db.RealProperty;
import accounts.db.TR;
import accounts.db.TRId;
import accounts.db.inst.TRNonDB;
import accounts.exp.AccountExp;
import accounts.util.FileUtils;

public class ExcelUtils
{
    private Map<String, Map<TRId, TR>> baMap;

    public ExcelUtils(Map<String, Map<TRId, TR>> baMap)
    {
        this.baMap = baMap;
    }

    public Map<String, Map<TRId, TR>> processAllSheets(String filename, Map<String, BankAccount> baMap) throws IOException,
                                                                                                        DBException
    {
        Map<String, Map<TRId, TR>> excelTrMap = new TreeMap<>();
        FileInputStream file = new FileInputStream(new File(filename));

        // Get the workbook instance for XLS file
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        for (int i = 0; i < workbook.getNumberOfSheets(); i++)
        {
            XSSFSheet sheet = workbook.getSheetAt(i);
            String sheetName = workbook.getSheetName(i);
            Map<TRId, TR> mapTr = new HashMap<>();
            excelTrMap.put(sheetName, mapTr);
            System.out.println("Processing sheet: " + sheetName);
            BankAccount ba = baMap.get(sheetName);
            if (ba == null)
            {
                throw new IOException("Unknown bank account name in excel=" + sheetName);
            }

            // Get iterator to all the rows in current sheet
            Iterator<Row> rowIterator = sheet.iterator();

            for (int rownum = 1; rownum <= sheet.getLastRowNum(); rownum++)
            {
                Row row = sheet.getRow(rownum);

                // Get iterator to all cells of current row

                TR tr = DBFactory.inst().createCorrespondingTRObj(ba);
                tr.setDate(row.getCell(0).getDateCellValue());
                tr.setDescription(row.getCell(1).getStringCellValue());
                tr.setDebit((float) row.getCell(2).getNumericCellValue());
                tr.setComment(row.getCell(3).getStringCellValue());
                tr.setTrType(row.getCell(4).getStringCellValue());
                tr.setTaxCategory(row.getCell(5).getStringCellValue());
                tr.setProperty(row.getCell(6).getStringCellValue());
                tr.setTrId();
                mapTr.put(tr.getTrId(), tr);

            }
        }
        return excelTrMap;

    }

    public Map<String, Map<TRId, TR>> processAllSheets(String filename) throws IOException
    {
        Map<String, Map<TRId, TR>> excelTrMap = new TreeMap<>();
        FileInputStream file = new FileInputStream(new File(filename));

        // Get the workbook instance for XLS file
        XSSFWorkbook workbook = new XSSFWorkbook(file);
        for (int i = 0; i < workbook.getNumberOfSheets(); i++)
        {
            XSSFSheet sheet = workbook.getSheetAt(i);
            String sheetName = workbook.getSheetName(i);
            Map<TRId, TR> mapTr = new HashMap<>();
            excelTrMap.put(sheetName, mapTr);
            System.out.println("Processing sheet: " + sheetName);

            // Get iterator to all the rows in current sheet
            Iterator<Row> rowIterator = sheet.iterator();

            for (int rownum = 1; rownum <= sheet.getLastRowNum(); rownum++)
            {
                Row row = sheet.getRow(rownum);

                // Get iterator to all cells of current row

                TR tr = new TRNonDB();
                tr.setDate(row.getCell(0).getDateCellValue());
                tr.setDescription(row.getCell(1).getStringCellValue());
                tr.setDebit((float) row.getCell(2).getNumericCellValue());
                tr.setComment(row.getCell(3).getStringCellValue());
                tr.setTrType(row.getCell(4).getStringCellValue());
                tr.setTaxCategory(row.getCell(5).getStringCellValue());
                tr.setProperty(row.getCell(6).getStringCellValue());
                tr.setTrId();
                mapTr.put(tr.getTrId(), tr);

            }
        }
        return excelTrMap;

    }

    public static Map<String, Map<TRId, TR>> getBankTransactionMapFromCsv(String dir) throws IOException, ParseException
    {
        Map<String, Map<TRId, TR>> baMap = new TreeMap<>();
        List<Path> listPath = findExportFiles(dir);
        int i = 0;
        for (Path p : listPath)
        {
            Map<TRId, TR> trMap = FileUtils.parseTransactions(dir + File.separator + p.toFile().getName());
            String baName = "";
            if (p.toFile().getName().toLowerCase().startsWith("export_"))
            {
                baName = p.toFile().getName().substring("export_".length(), p.toFile().getName().length() - 4);
            } else
            {
                baName = "Statement" + i++;
            }
            baMap.put(baName, trMap);
        }
        return baMap;
    }

    public static List<Path> findExportFiles(String dir)
    {
        List<Path> listFiles = new ArrayList<>();
        Path directory = Paths.get(dir);
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:export_*.csv");
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory, "export_*.csv"))
        {
            for (Path file : directoryStream)
            {
                if (pathMatcher.matches(file.getFileName()))
                {
                    listFiles.add(file.getFileName());
                }
            }
        } catch (IOException | DirectoryIteratorException ex)
        {
            ex.printStackTrace();
        }
        return listFiles;
    }

    public Map<String, Map<TRId, TR>> importCheck(Map<String, Map<TRId, TR>> excelTrMap,
                                                  Map<String, RealProperty> propMap) throws AccountExp
    {
        Map<String, Map<TRId, TR>> importMap = new TreeMap<>();
        for (String bankAccount : excelTrMap.keySet())
        {
            if (!baMap.containsKey(bankAccount))
            {
                throw new AccountExp(AccountExp.NOTPRESENT, "Bank account was not found in database=" + bankAccount);
            }

            Map<TRId, TR> importTrList = new HashMap<>();
            Map<TRId, TR> excelTrList = excelTrMap.get(bankAccount);
            Map<TRId, TR> dbTrList = baMap.get(bankAccount);
            for (TRId trId : excelTrList.keySet())
            {
                if (!dbTrList.containsKey(trId))
                {
                    for (TRId btrId : dbTrList.keySet())
                    {
                        System.out.println("trid=" + btrId);
                        System.out.println(dbTrList.get(btrId));
                    }
                    throw new AccountExp(AccountExp.NOTPRESENT,
                            "Unable to find this transaction in DB. BankAccount=" + bankAccount + "\nTransaction=" + trId);
                }
                if (!excelTrList.get(trId).equals(dbTrList.get(trId)))
                {
                    TR trToImport = excelTrList.get(trId);
                    if (propMap != null)
                    {
                        if (trToImport.getProperty() != null || !trToImport.getProperty().isEmpty())
                        {
                            if (!propMap.containsKey(trToImport.getProperty()))
                            {
                                throw new AccountExp(AccountExp.NOTPRESENT, "Unknown property=" + trToImport.getProperty()
                                        + " in " + bankAccount + "\nAllowed list=" + propMap.keySet());
                            }
                        }

                    }
                    if (trToImport.getTaxCategory() != null && !trToImport.getTaxCategory().isEmpty())
                    {
                        if (!AccountsUtil.inst().getAllowedTaxCategories().containsKey(trToImport.getTaxCategory()))
                        {
                            throw new AccountExp(AccountExp.NOTPRESENT,
                                    "Unknown tax category=" + trToImport.getTaxCategory() + " in " + bankAccount
                                            + "\nAllowed list=" + AccountsUtil.inst().getAllowedTaxCategories().keySet());
                        }
                    }
                    if (trToImport.getTrType() != null && !trToImport.getTrType().isEmpty())
                    {
                        if (!AccountsUtil.inst().getAllowedTrTypes().containsKey(trToImport.getTrType()))
                        {
                            throw new AccountExp(AccountExp.NOTPRESENT, "Unknown tr type=" + trToImport.getTrType() + " in "
                                    + bankAccount + "\nAllowed list=" + AccountsUtil.inst().getAllowedTrTypes().keySet());
                        }
                    }
                    if (trToImport.getComment() != null && trToImport.getComment().length() > AccountsUtil.COMMENT_MAXLEN)
                    {
                        throw new AccountExp(AccountExp.ACCOUNT_MAX_LIMIT, "Comment is too long=" + trToImport.getComment()
                                + " in " + bankAccount + "\nMax limit=" + AccountsUtil.COMMENT_MAXLEN);
                    }
                    importTrList.put(trId, trToImport);
                }
            }
            if (importTrList.size() > 0)
            {
                importMap.put(bankAccount, importTrList);
            }
        }
        return importMap;
    }

    public static void main(String[] args) throws Exception
    {

        if (args.length < 2)
        {
            System.out.println("Usage: export_dir exportfile.xlsx ");
            System.exit(-1);
        }
        Map<String, Map<TRId, TR>> baMap = getBankTransactionMapFromCsv(args[0]);
        final ExcelUtils howto = new ExcelUtils(baMap);
        Map<String, Map<TRId, TR>> excelTrMap = howto.processAllSheets(args[1]);
        howto.importCheck(excelTrMap, null);
        // howto.processAllSheets(args[0]);
    }
}
