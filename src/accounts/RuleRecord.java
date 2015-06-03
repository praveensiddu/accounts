package accounts;

public class RuleRecord
{

    private String currentAccount = null;
    private String trType         = null;
    private String descContains   = null;
    private String descStartsWith = null;
    private String taxCategory    = null;
    private String property       = null;
    private int    lineno         = 0;

    public RuleRecord createNew()
    {
        final RuleRecord rr = new RuleRecord();
        rr.setCurrentAccount(currentAccount);
        rr.setProperty(property);
        rr.setTaxCategory(taxCategory);
        return rr;
    }

    @Override
    public String toString()
    {
        final StringBuffer sb = new StringBuffer();
        sb.append("Account=" + currentAccount);
        if (descContains != null)
        {
            sb.append(", D contains=" + descContains);
        }
        if (descStartsWith != null)
        {
            sb.append(", D startWith=" + descStartsWith);
        }
        sb.append("==> tt=" + trType);
        sb.append(", tc=" + taxCategory);
        sb.append(", p=" + property);
        return sb.toString();

    }

    public String getCurrentAccount()
    {
        return currentAccount;
    }

    public void setCurrentAccount(final String currentAccount)
    {
        this.currentAccount = currentAccount;
    }

    public String getTrType()
    {
        return trType;
    }

    public void setTrType(final String trType)
    {
        this.trType = trType;
    }

    public String getDescContains()
    {
        return descContains;
    }

    public void setDescContains(final String descContains)
    {
        this.descContains = descContains;
    }

    public String getDescStartsWith()
    {
        return descStartsWith;
    }

    public void setDescStartsWith(final String descStartsWith)
    {
        this.descStartsWith = descStartsWith;
    }

    public String getTaxCategory()
    {
        return taxCategory;
    }

    public void setTaxCategory(final String taxCategory)
    {
        this.taxCategory = taxCategory;
    }

    public String getProperty()
    {
        return property;
    }

    public void setProperty(final String property)
    {
        this.property = property;
    }

    public static void main(final String[] args)
    {
        // TODO Auto-generated method stub

    }

    public int getLineno()
    {
        return lineno;
    }

    public void setLineno(final int lineno)
    {
        this.lineno = lineno;
    }

}
