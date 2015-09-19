package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR5")
@NamedQueries({ @NamedQuery(name = "TR5.getList", query = "SELECT e FROM TR5 e") })
public class TR5 extends TR
{

}
