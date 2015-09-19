package accounts.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries({ @NamedQuery(name = "BankAccount.getList", query = "SELECT e FROM BankAccount e") })
public class BankAccount
{
    @Id
    @Column(length = 40)
    private String name;
    private int    trTableId;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {

        if (name == null)
            return;
        this.name = name.trim().toLowerCase();
    }

    public int getTrTableId()
    {
        return trTableId;
    }

    public void setTrTableId(int trTableId)
    {
        this.trTableId = trTableId;
    }

    @Override
    public String toString()
    {
        return "Account=" + name + ", tableId=" + trTableId;
    }

}
