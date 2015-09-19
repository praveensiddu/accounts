package accounts.db;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import accounts.BankConfig;

@Entity
@IdClass(TRId.class)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class TR
{

    public TR()
    {
    }

    public TR(TRId trId)
    {
        date = trId.getDate();
        description = trId.getDescription();
        debit = trId.getDebit();
        this.trId = trId;
    }

    @Id
    @AttributeOverrides({ @AttributeOverride(name = "date", column = @Column(name = "DATE")),
            @AttributeOverride(name = "description", column = @Column(name = "DESCRIPTION")),
            @AttributeOverride(name = "debit", column = @Column(name = "DEBIT")) })
    @Temporal(TemporalType.DATE)
    private Date    date;

    @Id
    private String  description;
    @Column(length = 100)
    private String  comment;

    @Id
    private float   debit;
    private boolean locked;

    @Column(length = 20)
    private String  incomeType;
    @Column(length = 25)
    private String  taxCategory;
    @Column(length = 50)
    private String  property;

    public void copyNonPrimaryFields(TR tr)
    {
        setIncomeType(tr.getIncomeType());
        setTaxCategory(tr.getTaxCategory());
        setProperty(tr.getProperty());
        setComment(tr.getComment());
        setLocked(tr.isLocked());
    }

    @Transient
    private TRId trId = new TRId();

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
        this.trId.setDescription(description);
    }

    public void setTrId()
    {
        trId.setDescription(description);
        trId.setDate(date);
        trId.setDebit(debit);

    }

    public String getIncomeType()
    {
        return incomeType;
    }

    public void setIncomeType(final String incomeType)
    {
        if (incomeType == null)
            return;
        this.incomeType = incomeType.trim().toLowerCase();
    }

    public String getTaxCategory()
    {
        return taxCategory;
    }

    public void setTaxCategory(final String taxCategory)
    {
        if (taxCategory == null)
            return;
        this.taxCategory = taxCategory.trim().toLowerCase();
    }

    private static String[] approxCsvCorrection(final String[] fields)
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

    public static String trimQuote(String floatStr)
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

    private static float getFloatVaue(String floatStr)
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

    public void importLine(String line) throws IOException
    {

        line = line.toLowerCase().trim();
        if (line.isEmpty())
        {
            throw new IOException("Empty transaction");
        }
        String[] fields = line.split(",");
        fields = approxCsvCorrection(fields);
        if (fields.length != 8)
        {
            throw new IOException("Invalid transaction line" + line);
        }
        // #DATE,DESCRIPTION,DEBIT,COMMENT,ISLOCKED,INCOMETYPE,TAXCATEGORY,PROPERTY
        setDate(new Date(fields[0]));
        setDescription(fields[1]);
        Float value = getFloatVaue(fields[2].trim());
        setDebit(value);
        String tempStr = fields[3];
        if (tempStr != null)
            tempStr = tempStr.toLowerCase().trim();
        if (!"null".equals(tempStr))
        {
            setComment(tempStr);
        }
        tempStr = fields[4];
        if (tempStr != null)
            tempStr = tempStr.toLowerCase().trim();
        if ("true".equals(tempStr))
        {
            setLocked(true);
        }

        tempStr = fields[5];
        if (tempStr != null)
            tempStr = tempStr.toLowerCase().trim();
        if (!"null".equals(tempStr))
        {
            setIncomeType(tempStr);
        }

        tempStr = fields[6];
        if (tempStr != null)
            tempStr = tempStr.toLowerCase().trim();
        if (!"null".equals(tempStr))
        {
            setTaxCategory(tempStr);
        }

        tempStr = fields[7];
        if (tempStr != null)
            tempStr = tempStr.toLowerCase().trim();
        if (!"null".equals(tempStr))
        {
            setProperty(tempStr);
        }
    }

    public void init(String line, final BankConfig bc) throws IOException
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
            setDescription(fields[bc.getDescIndex()]);
        }
        if (bc.getMemoIndex() > -1 && fields.length > bc.getMemoIndex())
        {
            final String memo = fields[bc.getMemoIndex()].trim();
            if (!memo.isEmpty())
            {
                if (getDescription() == null)
                {
                    setDescription(": memo:" + memo + ":");
                } else
                {
                    setDescription(getDescription() + ": memo:" + memo + ":");
                }

            }
        }
        if (bc.getCheckNoIndex() > -1 && fields.length > bc.getCheckNoIndex())
        {
            final String checkNoStr = fields[bc.getCheckNoIndex()].trim();
            if (!checkNoStr.isEmpty())
            {
                if (getDescription() == null)
                {
                    setDescription(": checkno:" + checkNoStr + ":");
                } else
                {
                    setDescription(getDescription() + ": checkno:" + checkNoStr + ":");
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
        if (getDescription() == null)
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
        this.trId.setDate(date);
    }

    public float getDebit()
    {
        return debit;
    }

    public void setDebit(final float debit)
    {
        this.debit = debit;
        this.trId.setDebit(debit);
    }

    public TRId getTrId()
    {
        return trId;
    }

    @Override
    public String toString()
    {
        final StringBuffer sb = new StringBuffer();
        sb.append(new SimpleDateFormat("MM/dd/yyyy").format(date));
        sb.append(", " + debit);
        sb.append(", " + incomeType);
        sb.append(", " + taxCategory);
        sb.append(", " + property);
        sb.append(", " + description);

        return sb.toString();

    }

    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public boolean isLocked()
    {
        return locked;
    }

    public void setLocked(boolean locked)
    {
        this.locked = locked;
    }

    public String getProperty()
    {
        return property;
    }

    public void setProperty(String property)
    {
        if (property == null)
            return;
        this.property = property;
    }
}
