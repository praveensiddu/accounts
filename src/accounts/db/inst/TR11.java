package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR11")
@NamedQueries({ @NamedQuery(name = "TR11.getList", query = "SELECT e FROM TR11 e") })
public class TR11 extends TR
{

}
