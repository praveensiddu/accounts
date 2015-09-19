package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR1")
@NamedQueries({ @NamedQuery(name = "TR1.getList", query = "SELECT e FROM TR1 e") })
public class TR1 extends TR
{

}
