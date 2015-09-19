package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR18")
@NamedQueries({ @NamedQuery(name = "TR18.getList", query = "SELECT e FROM TR18 e") })
public class TR18 extends TR
{

}
