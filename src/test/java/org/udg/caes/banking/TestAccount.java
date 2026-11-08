package org.udg.caes.banking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.udg.caes.banking.entity.Account;
import org.udg.caes.banking.exceptions.NotEnoughBalance;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaris de l'entitat {@link Account}.
 *
 * L'únic mètode amb lògica és debit(): resta l'import i, si el saldo queda negatiu,
 * desfà l'operació i llança NotEnoughBalance. Es proven els dos camins i el cas frontera
 * (deixar el compte exactament a zero). També es proven el constructor i credit()
 * perquè els serveis en depenen directament.
 */
public class TestAccount {

  private Account acc;

  @BeforeEach
  void setUp() {
    acc = new Account("acc1", 100);
  }

  @Test
  void constructor_inicialitzaIdentificadorISaldo() {
    assertEquals("acc1", acc.getId());
    assertEquals(100, acc.getBalance());
  }

  @Test
  void credit_incrementaElSaldo() {
    acc.credit(50);
    assertEquals(150, acc.getBalance());
  }

  @Test
  void debit_ambSaldoSuficient_decrementaElSaldo() throws NotEnoughBalance {
    acc.debit(30);
    assertEquals(70, acc.getBalance());
  }

  @Test
  void debit_deTotElSaldo_deixaElCompteAZero() throws NotEnoughBalance {
    // Cas frontera: el saldo resultant és 0, que no és negatiu
    acc.debit(100);
    assertEquals(0, acc.getBalance());
  }

  @Test
  void debit_superiorAlSaldo_llencaNotEnoughBalanceINoModificaElSaldo() {
    // Cas frontera: 1 unitat més del que hi ha al compte
    assertThrows(NotEnoughBalance.class, () -> acc.debit(101));
    // L'operació s'ha de desfer: el saldo queda igual que abans
    assertEquals(100, acc.getBalance());
  }
}
