package accounts.db;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries({ @NamedQuery(name = "Owner.getList", query = "SELECT e FROM Owner e") })
public class Owner
{
    @Id
    @Column(length = 40)
    private String       name;
    private List<String> ownedBanks;
    private List<String> ownedProperties;
    private List<String> ownedCompanies;

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

    public List<String> getOwnedBanks()
    {
        return ownedBanks;
    }

    public void setOwnedBanks(List<String> ownedBanks)
    {
        this.ownedBanks = ownedBanks;
    }

    public static List<String> pipeSepFeilds(String pSepStr)
    {
        if (pSepStr == null)
            return null;
        pSepStr = pSepStr.trim().toLowerCase();
        if (pSepStr.isEmpty())
            return null;
        String fields[] = pSepStr.split("\\|");
        List<String> al = new ArrayList<>();
        for (String field : fields)
        {
            al.add(field.trim());
        }
        return al;
    }

    public void setOwnedBanks(String ownedBanks)
    {
        this.ownedBanks = pipeSepFeilds(ownedBanks);
    }

    public List<String> getOwnedProperties()
    {
        return ownedProperties;
    }

    public void setOwnedProperties(List<String> ownedProperties)
    {
        this.ownedProperties = ownedProperties;
    }

    public void setOwnedProperties(String ownedProperties)
    {
        this.ownedProperties = pipeSepFeilds(ownedProperties);
    }

    public List<String> getOwnedCompanies()
    {
        return ownedCompanies;
    }

    public void setOwnedCompanies(List<String> ownedCompanies)
    {
        this.ownedCompanies = ownedCompanies;
    }

    public void setOwnedCompanies(String ownedCompanies)
    {
        this.ownedCompanies = pipeSepFeilds(ownedCompanies);
    }

    @Override
    public String toString()
    {
        return "User=" + name;
    }

}
