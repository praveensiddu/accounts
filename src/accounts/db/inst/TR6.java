package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR6")
@NamedQueries({ @NamedQuery(name = "TR6.getList", query = "SELECT e FROM TR6 e") })
public class TR6 extends TR
{

}
