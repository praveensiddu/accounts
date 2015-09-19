package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR8")
@NamedQueries({ @NamedQuery(name = "TR8.getList", query = "SELECT e FROM TR8 e") })
public class TR8 extends TR
{

}
