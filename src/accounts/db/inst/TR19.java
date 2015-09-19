package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR19")
@NamedQueries({ @NamedQuery(name = "TR19.getList", query = "SELECT e FROM TR19 e") })
public class TR19 extends TR
{

}
