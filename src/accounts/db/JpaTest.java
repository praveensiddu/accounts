package accounts.db;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class JpaTest
{

    private static final String         PERSISTENCE_UNIT_NAME = "taxaccounting";
    private static EntityManagerFactory factory;

    public static void main(String[] args)
    {
        Map properties = new HashMap<String, String>();
        properties.put("javax.persistence.jdbc.url", "jdbc:derby:E:/temp/DERBYTUTOR1/" + "taxdb;create=true");
        factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, properties);

        EntityManager em = factory.createEntityManager();
        // Read the existing entries and write to console
        /*
                Query q = em.createQuery("SELECT u FROM BankAccount u");
                List<BankAccount> userList = q.getResultList();
                for (BankAccount user : userList)
                {
                    System.out.println(user.getName());
                }
                System.out.println("Size: " + userList.size());
        */
        // Create new user
        em.getTransaction().begin();
        BankAccount user = new BankAccount();
        user.setName("hahd Johnson");
        em.persist(user);
        em.getTransaction().commit();

        em.close();

    }

}
