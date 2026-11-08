package org.udg.caes.banking.FAKE;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.udg.caes.banking.entity.Account;
import org.udg.caes.banking.entity.Client;
import org.udg.caes.banking.entity.CreditCard;
import org.udg.caes.banking.exceptions.ClientNotFound;
import org.udg.caes.banking.manager.EntityManager;
import org.udg.caes.banking.service.AccountService;
import org.udg.caes.banking.service.ClientService;
import org.udg.caes.banking.service.CreditCardService;

import java.text.MessageFormat;

class BankModule extends AbstractModule {

  ClientService cs = new ClientService();
  AccountService as = new AccountService();
  CreditCardService ccs = new CreditCardService();
  EntityManagerImpl em = new EntityManagerImpl();

  BankModule() {
    Client cl1 = new Client("cl1");
    Client cl2 = new Client("cl2");
    Account acc11 = new Account("acc11", 100);
    Account acc12 = new Account("acc12", 400);
    Account acc21 = new Account("acc21", 300);
    CreditCard cc111 = new CreditCard("cc111");
    CreditCard cc211 = new CreditCard("cc211");
    CreditCard cc212 = new CreditCard("cc212");

    em.addClient(cl1);
    em.addClient(cl2);
    em.addAccount(acc11, cl1);
    em.addAccount(acc12, cl1);
    em.addAccount(acc21, cl2);
    em.addCreditCard(cc111, acc11);
    em.addCreditCard(cc211, acc21);
    em.addCreditCard(cc212, acc21);

    cc111.credit(50);
    cc211.credit(110);
    cc212.credit(90);
  }

  @Override
  protected void configure() {
    bind(ClientService.class).toInstance(cs);
    bind(AccountService.class).toInstance(as);
    bind(CreditCardService.class).toInstance(ccs);
    bind(EntityManager.class).toInstance(em);
  }
}


public class Main {

  static public void main(String [] args) throws ClientNotFound {

    Injector injector = Guice.createInjector(new BankModule());
    ClientService cs = injector.getInstance(ClientService.class);

    System.out.println(MessageFormat.format("Client 1 balance : {0}, Real balance: {1}",
        cs.getBalance(cs.getClient("cl1")),
        cs.getRealBalance(cs.getClient("cl1"))));

    System.out.println(MessageFormat.format("Client 2 balance : {0}, Real balance: {1}",
        cs.getBalance(cs.getClient("cl2")),
        cs.getRealBalance(cs.getClient("cl2"))));
  }
}
