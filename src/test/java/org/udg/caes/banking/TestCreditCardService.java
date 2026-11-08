package org.udg.caes.banking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.udg.caes.banking.entity.Account;
import org.udg.caes.banking.entity.CreditCard;
import org.udg.caes.banking.exceptions.CreditExceeded;
import org.udg.caes.banking.exceptions.NotEnoughBalance;
import org.udg.caes.banking.exceptions.PersistenceException;
import org.udg.caes.banking.manager.EntityManager;
import org.udg.caes.banking.service.CreditCardService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaris de {@link CreditCardService}.
 *
 * L'EntityManager és un mock: així el servei es prova aïllat de la capa de persistència
 * i podem verificar què es persisteix/esborra i forçar errors de persistència.
 * Les entitats (Account, CreditCard) són objectes reals, excepte quan cal un límit
 * de crèdit: CreditCard no permet fixar maxCredit, i per això es fa servir un spy.
 */
@ExtendWith(MockitoExtension.class)
public class TestCreditCardService {

  @Mock EntityManager em;
  @InjectMocks CreditCardService service;

  /** Targeta real (spy) amb el límit de crèdit que indiquem. */
  private CreditCard targetaAmbLimit(long maxCredit) {
    CreditCard cc = spy(new CreditCard("cc1"));
    doReturn(maxCredit).when(cc).getMaxCredit();
    return cc;
  }

  /** Targeta real amb un crèdit ja disposat. */
  private CreditCard targetaAmbCredit(long credit) {
    CreditCard cc = new CreditCard("cc1");
    cc.credit(credit);
    return cc;
  }

  // ---------------------------------------------------------------- credit

  @Test
  void credit_dinsDelLimit_incrementaElCreditIPersisteixLaTargeta() throws Exception {
    CreditCard cc = targetaAmbLimit(100);

    service.credit(cc, 40);

    assertEquals(40, cc.getCredit());
    verify(em).persist(cc);
  }

  @Test
  void credit_finsExactamentElLimit_esPermet() throws Exception {
    // Cas frontera: crèdit + import == límit
    CreditCard cc = targetaAmbLimit(100);
    cc.credit(60);

    service.credit(cc, 40);

    assertEquals(100, cc.getCredit());
    verify(em).persist(cc);
  }

  @Test
  void credit_superaElLimit_llencaCreditExceededINoPersisteix() {
    // Cas frontera: 1 unitat per sobre del límit
    CreditCard cc = targetaAmbLimit(100);
    cc.credit(60);

    assertThrows(CreditExceeded.class, () -> service.credit(cc, 41));

    assertEquals(60, cc.getCredit());
    verifyNoInteractions(em);
  }

  @Test
  void credit_targetaSenseLimitConfigurat_llencaCreditExceeded() {
    // Una CreditCard real sempre té maxCredit = 0, per tant qualsevol import > 0 supera el límit
    CreditCard cc = new CreditCard("cc1");

    assertThrows(CreditExceeded.class, () -> service.credit(cc, 1));

    assertEquals(0, cc.getCredit());
    verifyNoInteractions(em);
  }

  @Test
  void credit_errorDePersistencia_esPropaga() throws Exception {
    CreditCard cc = targetaAmbLimit(100);
    doThrow(new PersistenceException()).when(em).persist(cc);

    assertThrows(PersistenceException.class, () -> service.credit(cc, 40));
  }

  // ---------------------------------------------------------------- charge

  @Test
  void charge_ambSaldoSuficient_carregaElCreditAlCompteIResetejaLaTargeta() throws Exception {
    Account acc = new Account("acc1", 100);
    CreditCard cc = targetaAmbCredit(30);

    service.charge(cc, acc);

    assertEquals(70, acc.getBalance());
    assertEquals(0, cc.getCredit());
    verify(em).persist(cc);
    verify(em).persist(acc);
  }

  @Test
  void charge_ambSaldoIgualAlCredit_deixaElCompteAZero() throws Exception {
    // Cas frontera: saldo == crèdit
    Account acc = new Account("acc1", 30);
    CreditCard cc = targetaAmbCredit(30);

    service.charge(cc, acc);

    assertEquals(0, acc.getBalance());
    assertEquals(0, cc.getCredit());
  }

  @Test
  void charge_ambSaldoInsuficient_llencaNotEnoughBalanceINoModificaRes() {
    // Cas frontera: saldo una unitat per sota del crèdit
    Account acc = new Account("acc1", 29);
    CreditCard cc = targetaAmbCredit(30);

    assertThrows(NotEnoughBalance.class, () -> service.charge(cc, acc));

    assertEquals(29, acc.getBalance());
    assertEquals(30, cc.getCredit());
    verifyNoInteractions(em);
  }

  @Test
  void charge_errorDePersistencia_esPropaga() throws Exception {
    Account acc = new Account("acc1", 100);
    CreditCard cc = targetaAmbCredit(30);
    doThrow(new PersistenceException()).when(em).persist(cc);

    assertThrows(PersistenceException.class, () -> service.charge(cc, acc));
  }

  // ---------------------------------------------------------------- delete

  @Test
  void delete_carregaElCreditAlCompteAssociatIDespresEsborraLaTargeta() throws Exception {
    Account acc = new Account("acc1", 100);
    CreditCard cc = targetaAmbCredit(30);
    when(em.getAccountAssociated(cc)).thenReturn(acc);

    service.delete(cc);

    assertEquals(70, acc.getBalance());
    assertEquals(0, cc.getCredit());
    // Primer es liquida el crèdit (charge) i després s'esborra la targeta
    InOrder ordre = inOrder(em);
    ordre.verify(em).persist(cc);
    ordre.verify(em).persist(acc);
    ordre.verify(em).delete(cc);
  }

  @Test
  void delete_ambSaldoInsuficientAlCompteAssociat_noEsborraLaTargeta() throws Exception {
    Account acc = new Account("acc1", 10);
    CreditCard cc = targetaAmbCredit(30);
    when(em.getAccountAssociated(cc)).thenReturn(acc);

    assertThrows(NotEnoughBalance.class, () -> service.delete(cc));

    verify(em, never()).persist(any());
    verify(em, never()).delete(any());
  }

  @Test
  void delete_errorDePersistenciaEnEsborrar_esPropaga() throws Exception {
    Account acc = new Account("acc1", 100);
    CreditCard cc = new CreditCard("cc1");
    when(em.getAccountAssociated(cc)).thenReturn(acc);
    doThrow(new PersistenceException()).when(em).delete(cc);

    assertThrows(PersistenceException.class, () -> service.delete(cc));
  }

  // ---------------------------------------------------------------- invalidate

  @Test
  void invalidate_desactivaLaTargetaAbansDePersistirLa() throws Exception {
    // CreditCard no té getter d'"active": amb un spy verifiquem la crida a setActive(false)
    CreditCard cc = spy(new CreditCard("cc1"));

    service.invalidate(cc);

    InOrder ordre = inOrder(cc, em);
    ordre.verify(cc).setActive(false);
    ordre.verify(em).persist(cc);
  }

  @Test
  void invalidate_errorDePersistencia_esPropaga() throws Exception {
    CreditCard cc = new CreditCard("cc1");
    doThrow(new PersistenceException()).when(em).persist(cc);

    assertThrows(PersistenceException.class, () -> service.invalidate(cc));
  }
}
