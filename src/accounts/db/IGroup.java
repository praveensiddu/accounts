package accounts.db;

import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries({ @NamedQuery(name = "IGroup.getList", query = "SELECT b FROM IGroup b") })
public class IGroup
{
    @Id
    @Column(length = 40)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "MEMBER", joinColumns = @JoinColumn(name = "GROUP_NAME") )
    private List<String> members;

    public List<String> getMembers()
    {
        return members;
    }

    public void setMembers(List<String> members)
    {
        this.members = members;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        if (name == null)
            return;

        if (!name.startsWith("g_"))
            name = "g_" + name;
        this.name = name.trim().toLowerCase();
    }

    @Override
    public String toString()
    {
        return "Group=" + name + ", members=" + members;
    }

}
