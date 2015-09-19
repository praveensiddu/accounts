package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR14")
@NamedQueries({ @NamedQuery(name = "TR14.getList", query = "SELECT e FROM TR14 e") })
public class TR14 extends TR
{

}
