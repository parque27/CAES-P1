package org.udg.caes.banking.service;

import com.google.inject.Inject;
import org.udg.caes.banking.entity.Account;
import org.udg.caes.banking.entity.Client;
import org.udg.caes.banking.entity.CreditCard;
import org.udg.caes.banking.exceptions.*;
import org.udg.caes.banking.manager.EntityManager;

import java.util.List;

public class ClientService {

  @Inject EntityManager em;
  @Inject AccountService as;

  public Client getClient(String id) throws ClientNotFound {
    try {
      Client c = (Client)em.get(id, Client.class);
      return c;
    } catch (EntityNotFound e) {
      throw new ClientNotFound();
    }
  }

  public void delete(String id) throws ClientNotFound, PersistenceException, AccountActive, NotEnoughBalance {
    Client cl = getClient(id);

    List<Account> accs = em.getClientAccounts(cl);
    for (Account acc: accs) as.delete(acc);
  }

  public long getBalance(Client cl) {
    List<Account> accs = em.getClientAccounts(cl);
    long balance = 0;
    for (Account acc : accs)
      balance += acc.getBalance();
    return balance;
  }

  public long getRealBalance(Client cl) {
    List<Account> accs = em.getClientAccounts(cl);
    long balance = 0;
    for (Account acc : accs) {
      balance += acc.getBalance();
      List<CreditCard> ccs = em.getCreditCards(acc);
      for (CreditCard cc : ccs)
        balance -= cc.getCredit();
    }
    return balance;
  }
}
