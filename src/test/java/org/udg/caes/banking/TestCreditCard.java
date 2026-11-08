package org.udg.caes.banking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.udg.caes.banking.entity.CreditCard;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaris de l'entitat {@link CreditCard}.
 *
 * Es prova l'estat inicial i els mètodes que modifiquen el crèdit (credit i reset),
 * que són els que fa servir CreditCardService.
 *
 * setActive() no es pot comprovar aquí: la classe no té cap getter de "active".
 * La seva crida es verifica a TestCreditCardService (mètode invalidate).
 */
public class TestCreditCard {

  private CreditCard cc;

  @BeforeEach
  void setUp() {
    cc = new CreditCard("cc1");
  }

  @Test
  void constructor_targetaNovaSenseCreditNiLimit() {
    assertEquals("cc1", cc.getId());
    assertEquals(0, cc.getCredit());
    // No hi ha cap constructor ni setter per fixar el límit: sempre val 0
    assertEquals(0, cc.getMaxCredit());
  }

  @Test
  void credit_acumulaElCreditDisposat() {
    cc.credit(50);
    cc.credit(30);
    assertEquals(80, cc.getCredit());
  }

  @Test
  void reset_posaElCreditAZero() {
    cc.credit(50);
    cc.reset();
    assertEquals(0, cc.getCredit());
  }
}
