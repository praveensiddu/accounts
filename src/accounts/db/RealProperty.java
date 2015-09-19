package accounts.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries({ @NamedQuery(name = "RealProperty.getList", query = "SELECT e FROM RealProperty e") })
public class RealProperty
{
    @Id
    @Column(length = 30)
    private String         propertyName;
    private int            landValue       = -1;
    private int            cost            = -1;
    private int            renovation      = -1;
    private int            loanClosingCost = -1;
    private int            ownerCount      = 1;
    private java.util.Date purchaseDate;

    public String getPropertyName()
    {
        return propertyName;
    }

    public void setPropertyName(String propertyName)
    {
        if (propertyName == null)
            return;
        this.propertyName = propertyName.trim().toLowerCase();
    }

    public int getLandValue()
    {
        return landValue;
    }

    public void setLandValue(int landValue)
    {
        this.landValue = landValue;
    }

    public int getCost()
    {
        return cost;
    }

    public void setCost(int cost)
    {
        this.cost = cost;
    }

    public int getRenovation()
    {
        return renovation;
    }

    public void setRenovation(int renovation)
    {
        this.renovation = renovation;
    }

    public java.util.Date getPurchaseDate()
    {
        return purchaseDate;
    }

    public void setPurchaseDate(java.util.Date purchaseDate)
    {
        this.purchaseDate = purchaseDate;
    }

    @Override
    public String toString()
    {
        return "Prop=" + propertyName + ", land=" + landValue + ", cost=" + cost + ", renovation=" + renovation + ", pDate="
                + purchaseDate;
    }

    public static void main(String[] args)
    {
        // TODO Auto-generated method stub

    }

    public int getLoanClosingCost()
    {
        return loanClosingCost;
    }

    public void setLoanClosingCost(int loanClosingCost)
    {
        this.loanClosingCost = loanClosingCost;
    }

    public int getOwnerCount()
    {
        return ownerCount;
    }

    public void setOwnerCount(int ownerCount)
    {
        this.ownerCount = ownerCount;
    }

}
