package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR3")
@NamedQueries({ @NamedQuery(name = "TR3.getList", query = "SELECT e FROM TR3 e") })
public class TR3 extends TR
{

}
