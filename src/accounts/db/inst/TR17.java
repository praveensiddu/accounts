package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR17")
@NamedQueries({ @NamedQuery(name = "TR17.getList", query = "SELECT e FROM TR17 e") })
public class TR17 extends TR
{

}
