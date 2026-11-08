package org.udg.caes.banking.entity;

import org.udg.caes.banking.exceptions.NotEnoughBalance;

public class Account {
  String id;
  long balance;

  public Account(String id, long balance) {
    this.id = id;
    this.balance = balance;
  }

  public void credit(long amount) {
    balance += amount;
  }

  public void debit(long amount) throws NotEnoughBalance {
    balance -= amount;
    if (balance < 0) {
      balance += amount;
      throw new NotEnoughBalance();
    }
  }

  public long getBalance() {
    return balance;
  }

  public String getId() {
    return id;
  }
}
