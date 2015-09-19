package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR7")
@NamedQueries({ @NamedQuery(name = "TR7.getList", query = "SELECT e FROM TR7 e") })
public class TR7 extends TR
{

}
