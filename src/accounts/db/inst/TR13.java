package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR13")
@NamedQueries({ @NamedQuery(name = "TR13.getList", query = "SELECT e FROM TR13 e") })
public class TR13 extends TR
{

}
