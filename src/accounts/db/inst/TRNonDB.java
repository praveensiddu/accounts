package accounts.db.inst;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import accounts.db.TR;

@Entity
@Table(name = "TRNonDB")
@NamedQueries({ @NamedQuery(name = "TRNonDB.getList", query = "SELECT e FROM TRNonDB e") })
public class TRNonDB extends TR
{

}
