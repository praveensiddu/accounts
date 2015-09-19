package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR12")
@NamedQueries({ @NamedQuery(name = "TR12.getList", query = "SELECT e FROM TR12 e") })
public class TR12 extends TR
{

}
