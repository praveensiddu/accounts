package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR20")
@NamedQueries({ @NamedQuery(name = "TR20.getList", query = "SELECT e FROM TR20 e") })
public class TR20 extends TR
{

}
