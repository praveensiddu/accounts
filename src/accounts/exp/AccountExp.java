package accounts.exp;

public class AccountExp extends Exception
{
    public int getCode()
    {
        return code;
    }

    public void setCode(int code)
    {
        this.code = code;
    }

    public String getMsg()
    {
        return msg;
    }

    public void setMsg(String msg)
    {
        this.msg = msg;
    }

    @Override
    public String toString()
    {
        return "Code=" + code + ", Msg=" + msg;
    }

    private int             code;
    private String          msg;
    public static final int INVALID_INPUT     = 1;
    public static final int ENTRY_EXISTS      = 2;
    public static final int ACCOUNT_MAX_LIMIT = 3;
    public static final int NOTPRESENT        = 4;
    public static final int NOTIMPLEMENTED    = 5;
    public static final int INTERNAL_ERR      = 6;

    public AccountExp(int code, String msg)
    {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static void main(String[] args)
    {
        // TODO Auto-generated method stub

    }

}
