package accounts.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries({ @NamedQuery(name = "Company.getList", query = "SELECT e FROM Company e") })
public class Company
{
    @Id
    @Column(length = 30)
    private String name;
    private float  rentPercentage = -1;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        if (name == null)
            return;
        if (!name.startsWith("c_"))
            name = "c_" + name;

        this.name = name.trim().toLowerCase();
    }

    @Override
    public String toString()
    {
        return "Company=" + name + ", rentPercentage=" + rentPercentage;
    }

    public static void main(String[] args)
    {
        // TODO Auto-generated method stub

    }

    public float getRentPercentage()
    {
        return rentPercentage;
    }

    public void setRentPercentage(float rentPercentage)
    {
        this.rentPercentage = rentPercentage;
    }

}
