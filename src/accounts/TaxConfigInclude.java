package accounts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TaxConfigInclude
{
    private List<RuleRecord> arr = new ArrayList<RuleRecord>();

    public List<RuleRecord> getRuleRecordList()
    {
        return arr;
    }

    public TaxConfigInclude(final String file) throws IOException
    {
        final FileReader fr = new FileReader(file);
        final BufferedReader br = new BufferedReader(fr);
        try
        {
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

                if (TaxConfig.BACCOUNT.equals(key))
                {
                    throw new IOException("BACCOUNT should not be in include file");
                } else if (TaxConfig.TRANSACTION_TYPE.equals(key))
                {
                    // it is a new record. Create a new record
                    RuleRecord rrtemp = rr.createNew();
                    rr = rrtemp;
                    rr.setLineno(lineno);

                    rr.setTrType(value);
                    arr.add(rr);
                } else if (TaxConfig.INCLUDE_FILE.equals(key))
                {
                    TaxConfigInclude tcf = new TaxConfigInclude(value);
                    for (RuleRecord rrinclude : tcf.getRuleRecordList())
                    {
                        arr.add(rrinclude);
                    }

                } else if (TaxConfig.DESC_CONTAINS.equals(key))
                {
                    rr.setDescContains(value);
                } else if (TaxConfig.DESC_STARTSWITH.equals(key))
                {
                    rr.setDescStartsWith(value);
                } else if (TaxConfig.TAX_CATEGORY.equals(key))
                {
                    rr.setTaxCategory(value);
                } else if (TaxConfig.PROPERTY.equals(key))
                {

                    throw new IOException("PROPERTY should not be in include file");
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
            final TaxConfigInclude bc = new TaxConfigInclude(args[0]);

            for (final RuleRecord rr : bc.arr)
            {
                System.out.println(rr);
            }
        } catch (final IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
