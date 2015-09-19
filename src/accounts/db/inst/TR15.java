package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR15")
@NamedQueries({ @NamedQuery(name = "TR15.getList", query = "SELECT e FROM TR15 e") })
public class TR15 extends TR
{

}
