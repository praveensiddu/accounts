package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR16")
@NamedQueries({ @NamedQuery(name = "TR16.getList", query = "SELECT e FROM TR16 e") })
public class TR16 extends TR
{

}
