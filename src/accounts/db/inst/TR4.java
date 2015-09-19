package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR4")
@NamedQueries({ @NamedQuery(name = "TR4.getList", query = "SELECT e FROM TR4 e") })
public class TR4 extends TR
{

}
