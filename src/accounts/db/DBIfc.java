package accounts.db;

import java.io.IOException;
import java.util.Map;

public interface DBIfc
{
    void createAndConnectDB(String dir) throws IOException;

    void createBankAccount(String accountName, String bankName) throws DBException;

    void updateBankAccount(BankAccount bAccount) throws DBException;

    void deleteBankAccount(String accountName) throws DBException;

    void createProperty(RealProperty prop) throws DBException;

    void updateProperty(RealProperty prop) throws DBException;

    void deleteProperty(String name) throws DBException;

    void createCompany(Company name) throws DBException;

    void updateCompany(Company comp) throws DBException;

    void deleteCompany(String name) throws DBException;

    boolean tableExists(final String tablename) throws DBException;

    Map<String, BankAccount> getAccounts() throws DBException;

    Map<String, RealProperty> getProperties() throws DBException;

    Map<String, Company> getCompanies() throws DBException;

    Map<String, IGroup> getGroupsMap() throws DBException;

    Map<TRId, TR> getTransactions(int tableId) throws DBException;

    TR createCorrespondingTRObj(BankAccount ba) throws DBException;

    int updateTransactions(Map<TRId, TR> trs, boolean checkLocked) throws DBException;

    void updateTransaction(TR tr) throws DBException;

    public void createGroup(IGroup group) throws DBException;

    public void deleteGroup(String name) throws DBException;

    public void deleteTransactions(int tableId) throws DBException;

}
