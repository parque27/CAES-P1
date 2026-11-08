package org.udg.caes.banking.manager;

import org.udg.caes.banking.entity.Account;
import org.udg.caes.banking.entity.Client;
import org.udg.caes.banking.entity.CreditCard;
import org.udg.caes.banking.exceptions.EntityNotFound;
import org.udg.caes.banking.exceptions.PersistenceException;

import java.util.List;

public interface EntityManager {

  // Gerenic CRUD access
  Object get(String id, Class<?> clazz) throws EntityNotFound;
  void persist(Object o) throws PersistenceException;
  void delete(Object o) throws PersistenceException;

  // Relationship access
  List<CreditCard> getCreditCards(Account acc);

  List<Account> getClientAccounts(Client cl);

  Account getAccountAssociated(CreditCard cc);
}
