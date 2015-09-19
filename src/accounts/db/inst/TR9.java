package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR9")
@NamedQueries({ @NamedQuery(name = "TR9.getList", query = "SELECT e FROM TR9 e") })
public class TR9 extends TR
{

}
