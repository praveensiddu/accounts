package accounts.db;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.Embeddable;

@Embeddable
public class TRId implements Serializable, Comparable<TRId>
{

    private static final long serialVersionUID = 1L;

    private Date   date;
    private String description;

    private float debit;

    public TRId()
    {
    }

    public TRId(Date date, String description, float debit)
    {
        this.date = date;
        this.description = description;
        this.debit = debit;
    }

    @Override
    public int hashCode()
    {
        int hashcode = 0;
        if (date != null)
            hashcode += date.hashCode();
        if (description != null)
            hashcode += description.hashCode();
        return hashcode;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;

        }
        if (!(obj instanceof TRId))
            return false;
        TRId trId = (TRId) obj;
        if (date == null)
        {
            if (trId.getDate() != null)
                return false;
        } else
        {
            if (!date.equals(trId.getDate()))
                return false;
        }
        if (description == null)
        {
            if (trId.getDescription() != null)
                return false;
        } else
        {
            if (!description.equals(trId.getDescription()))
                return false;
        }
        // approximate check
        if ((debit - trId.getDebit() > 1) || (trId.getDebit() - debit > 1))
        {
            return false;
        }
        return true;

    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public float getDebit()
    {
        return debit;
    }

    public void setDebit(float debit)
    {
        this.debit = debit;
    }

    @Override
    public String toString()
    {
        final StringBuffer sb = new StringBuffer();

        sb.append(new SimpleDateFormat("MM-dd-yyyy").format(date));
        sb.append(", " + description);
        sb.append(", " + debit);

        return sb.toString();
    }

    @Override
    public int compareTo(TRId o)
    {
        TRId newTrId = o;
        int val = this.date.compareTo(newTrId.getDate());
        if (val == 0)
        {
            val = this.description.compareTo(newTrId.getDescription());
            if (val == 0)
            {
                if (this.debit > newTrId.getDebit())
                {
                    return 1;
                } else if (this.debit < newTrId.getDebit())
                {
                    return -1;
                }
                return 0;
            }
        } else
        {
            return val;
        }
        return val;
    }

}
