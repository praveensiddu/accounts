package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TR2")
@NamedQueries({ @NamedQuery(name = "TR2.getList", query = "SELECT e FROM TR2 e") })
public class TR2 extends TR
{

}
