package accounts;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class TR
{
    private Date   date;
    private String desc;
    private float  debit;

    public String getTrType()
    {
        return trType;
    }

    public void setTrType(final String trType)
    {
        this.trType = trType;
    }

    public String getTaxCategory()
    {
        return taxCategory;
    }

    public void setTaxCategory(final String taxCategory)
    {
        this.taxCategory = taxCategory;
    }

    public String getProperty()
    {
        return property;
    }

    public void setProperty(final String property)
    {
        this.property = property;
    }

    private String trType      = null;
    private String taxCategory = null;
    private String property    = null;

    private String[] approxCsvCorrection(final String[] fields)
    {
        final ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < fields.length;)
        {
            if (fields[i].startsWith("\""))
            {
                if (fields[i].endsWith("\""))
                {
                    list.add(fields[i]);
                    i++;
                } else
                {
                    String field = fields[i];
                    i++;
                    for (int j = i; j < fields.length; j++)
                    {
                        if (fields[j].endsWith("\""))
                        {
                            field += "," + fields[j];
                            i++;
                            break;

                        } else
                        {
                            field += "," + fields[j];
                            i++;
                        }
                    }
                    list.add(field);
                }
            } else
            {
                list.add(fields[i]);
                i++;
            }
        }
        return list.toArray(new String[0]);
    }

    public String trimQuote(String floatStr)
    {
        if (floatStr.startsWith("\""))
        {
            floatStr = floatStr.substring(1);
        }
        if (floatStr.endsWith("\""))
        {
            floatStr = floatStr.substring(0, floatStr.length() - 1);
        }
        return floatStr;
    }

    public float getFloatVaue(String floatStr)
    {
        boolean negative = false;

        if (floatStr.startsWith("("))
        {
            floatStr = floatStr.substring(1);
            negative = true;
        }
        if (floatStr.endsWith(")"))
        {
            floatStr = floatStr.substring(0, floatStr.length() - 1);
        }
        if (floatStr.startsWith("$"))
        {
            floatStr = floatStr.substring(1);
        }
        if (negative)
        {
            floatStr = "-" + floatStr;
        }
        return new Float(floatStr).floatValue();
    }

    public TR(String line, final BankConfig bc) throws IOException
    {
        line = line.toLowerCase().trim();
        if (line.isEmpty())
        {
            throw new IOException("Empty transaction");
        }
        String[] fields = line.split(",");
        fields = approxCsvCorrection(fields);

        if (fields.length <= bc.getDateIndex())
        {
            throw new IOException("Invalid transaction line" + line);
        }
        for (int i = 0; i < fields.length; i++)
        {
            fields[i] = trimQuote(fields[i]);
        }
        setDate(new Date(fields[bc.getDateIndex()]));
        if (fields.length > bc.getDescIndex())
        {
            setDesc(fields[bc.getDescIndex()]);
        }
        if (bc.getMemoIndex() > -1 && fields.length > bc.getMemoIndex())
        {
            final String memo = fields[bc.getMemoIndex()].trim();
            if (!memo.isEmpty())
            {
                if (getDesc() == null)
                {
                    setDesc(": memo:" + memo + ":");
                } else
                {
                    setDesc(getDesc() + ": memo:" + memo + ":");
                }

            }
        }
        if (bc.getCheckNoIndex() > -1 && fields.length > bc.getCheckNoIndex())
        {
            final String checkNoStr = fields[bc.getCheckNoIndex()].trim();
            if (!checkNoStr.isEmpty())
            {
                if (getDesc() == null)
                {
                    setDesc(": checkno:" + checkNoStr + ":");
                } else
                {
                    setDesc(getDesc() + ": checkno:" + checkNoStr + ":");
                }
            }
        }
        if (fields.length > bc.getDebitIndex() && !fields[bc.getDebitIndex()].trim().isEmpty())
        {
            Float value = getFloatVaue(fields[bc.getDebitIndex()].trim());

            if (bc.getCreditIndex() != -1)
            {
                // Credit is a separate column. that means debit should always
                // be negative
                if (value > 0)
                {
                    value = -value;
                }
            }

            setDebit(value);
        }
        if (bc.getCreditIndex() > -1 && fields.length > bc.getCreditIndex() && !fields[bc.getCreditIndex()].trim().isEmpty())
        {
            Float value = getFloatVaue(fields[bc.getCreditIndex()].trim());
            if (value < 0)
            {
                value = -value;
            }
            setDebit(value);
        }
        if (bc.getFeesIndex() > -1 && fields.length > bc.getFeesIndex() && !fields[bc.getFeesIndex()].trim().isEmpty())
        {
            Float value = getFloatVaue(fields[bc.getFeesIndex()].trim());
            if (value > 0)
            {
                value = -value;
            }
            setDebit(value);
        }
        if (getDesc() == null)
        {
            throw new IOException("Description is mandatory");
        }
    }

    public static void main(final String[] args)
    {
        // TODO Auto-generated method stub

    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(final Date date)
    {
        this.date = date;
    }

    public String getDesc()
    {
        return desc;
    }

    public void setDesc(final String desc)
    {
        this.desc = desc;
    }

    public float getDebit()
    {
        return debit;
    }

    public void setDebit(final float debit)
    {
        this.debit = debit;
    }

    @Override
    public String toString()
    {
        final StringBuffer sb = new StringBuffer();
        sb.append(new SimpleDateFormat("MM/dd/yyyy").format(date));
        sb.append(", " + debit);
        sb.append(", " + trType);
        sb.append(", " + taxCategory);
        sb.append(", " + property);
        sb.append(", " + desc);

        return sb.toString();

    }
}
