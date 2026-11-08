package org.udg.caes.banking.FAKE;

import org.udg.caes.banking.entity.Account;
import org.udg.caes.banking.entity.Client;
import org.udg.caes.banking.entity.CreditCard;
import org.udg.caes.banking.exceptions.EntityNotFound;
import org.udg.caes.banking.exceptions.PersistenceException;
import org.udg.caes.banking.manager.EntityManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityManagerImpl implements EntityManager {
  Map<String, Client> clients = new HashMap<String, Client>();
  Map<String, Account> accounts = new HashMap<String, Account>();
  Map<String, CreditCard> creditCards = new HashMap<String, CreditCard>();

  Map<String, List<Account>> client2account = new HashMap<String, List<Account>>();
  Map<String, List<CreditCard>> account2cc = new HashMap<String, List<CreditCard>>();
  Map<String, Account> cc2account = new HashMap<String, Account>();

  public Object get(String id, Class<?> clazz) throws EntityNotFound {
    Object o = null;
    if (clazz == Client.class) o =  clients.get(id);
    if (clazz == Account.class) o =  accounts.get(id);
    if (clazz == CreditCard.class) o =  creditCards.get(id);
    if (o == null) throw new EntityNotFound();
    return o;
  }

  public void persist(Object o) throws PersistenceException {

  }

  public void delete(Object o) throws PersistenceException {
    if (o instanceof Client) clients.remove(((Client)o).getId());
    else if (o instanceof Account) o =  accounts.remove(((Account)o).getId());
    else if (o instanceof CreditCard) o =  creditCards.remove(((CreditCard)o).getId());
    else throw new PersistenceException();
  }

  public List<CreditCard> getCreditCards(Account acc) {
    if (!account2cc.containsKey(acc.getId()))
      account2cc.put(acc.getId(), new ArrayList<CreditCard>());
    return account2cc.get(acc.getId());
  }

  public List<Account> getClientAccounts(Client cl) {
    if (!client2account.containsKey(cl.getId()))
      client2account.put(cl.getId(), new ArrayList<Account>());
    return client2account.get(cl.getId());
  }

  public Account getAccountAssociated(CreditCard cc) {
    return cc2account.get(cc.getId());
  }

  public void addAccount(Account acc, Client cl) {
    accounts.put(acc.getId(), acc);
    if (!client2account.containsKey(cl.getId()))
      client2account.put(cl.getId(), new ArrayList<Account>());
    client2account.get(cl.getId()).add(acc);
  }

  public void addClient(Client cl) {
    clients.put(cl.getId(), cl);
  }

  public void addCreditCard(CreditCard cc, Account acc) {
    creditCards.put(cc.getId(), cc);
    if (!account2cc.containsKey(acc.getId()))
      account2cc.put(acc.getId(), new ArrayList<CreditCard>());
    account2cc.get(acc.getId()).add(cc);
    cc2account.put(cc.getId(), acc);

  }


}
