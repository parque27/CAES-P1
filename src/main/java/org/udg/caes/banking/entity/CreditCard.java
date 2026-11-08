package org.udg.caes.banking.entity;

public class CreditCard {
  String id;
  long credit;
  long maxCredit;
  private boolean active;

  public CreditCard(String id) {
    this.id = id;
  }

  public long getCredit() {
    return credit;
  }

  public void reset() {
    credit = 0;
  }

  public long getMaxCredit() {
    return maxCredit;
  }

  public void credit(long amount) {
    credit += amount;
  }

  public String getId() {
    return id;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
