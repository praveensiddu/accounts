package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR10")
@NamedQueries({ @NamedQuery(name = "TR10.getList", query = "SELECT e FROM TR10 e") })
public class TR10 extends TR
{

}
