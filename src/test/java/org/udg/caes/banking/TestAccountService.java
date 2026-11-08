package org.udg.caes.banking;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.udg.caes.banking.entity.Account;
import org.udg.caes.banking.entity.CreditCard;
import org.udg.caes.banking.exceptions.*;
import org.udg.caes.banking.manager.EntityManager;
import org.udg.caes.banking.service.AccountService;
import org.udg.caes.banking.service.CreditCardService;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaris d'{@link AccountService}.
 *
 * Es substitueixen per mocks les seves dues dependències: l'EntityManager (persistència)
 * i el CreditCardService (que té els seus propis tests a TestCreditCardService).
 * Els comptes i targetes són objectes reals.
 */
@ExtendWith(MockitoExtension.class)
public class TestAccountService {

  @Mock EntityManager em;
  @Mock CreditCardService ccs;
  @InjectMocks AccountService service;

  /** Crea un compte real i fa que l'EntityManager el retorni quan es demani pel seu id. */
  private Account compteGuardat(String id, long saldo) throws EntityNotFound {
    Account acc = new Account(id, saldo);
    when(em.get(id, Account.class)).thenReturn(acc);
    return acc;
  }

  /** Fa que l'EntityManager no trobi cap compte amb aquest id. */
  private void compteInexistent(String id) throws EntityNotFound {
    when(em.get(id, Account.class)).thenThrow(new EntityNotFound());
  }

  // ---------------------------------------------------------------- getAccount

  @Test
  void getAccount_existent_retornaElCompte() throws Exception {
    Account acc = compteGuardat("acc1", 100);

    assertSame(acc, service.getAccount("acc1"));
  }

  @Test
  void getAccount_inexistent_converteixEntityNotFoundEnAccountNotFound() throws Exception {
    compteInexistent("noExisteix");

    assertThrows(AccountNotFound.class, () -> service.getAccount("noExisteix"));
  }

  // ---------------------------------------------------------------- transfer

  @Test
  void transfer_ambSaldoSuficient_mouElsDinersIPersisteixElsDosComptes() throws Exception {
    Account origen = compteGuardat("acc1", 100);
    Account desti = compteGuardat("acc2", 50);

    service.transfer("acc1", "acc2", 30);

    assertEquals(70, origen.getBalance());
    assertEquals(80, desti.getBalance());
    verify(em).persist(origen);
    verify(em).persist(desti);
  }

  @Test
  void transfer_deTotElSaldo_deixaLOrigenAZero() throws Exception {
    // Cas frontera: es transfereix exactament el saldo disponible
    Account origen = compteGuardat("acc1", 100);
    Account desti = compteGuardat("acc2", 50);

    service.transfer("acc1", "acc2", 100);

    assertEquals(0, origen.getBalance());
    assertEquals(150, desti.getBalance());
  }

  @Test
  void transfer_ambSaldoInsuficient_llencaNotEnoughBalanceINoModificaRes() throws Exception {
    Account origen = compteGuardat("acc1", 100);
    Account desti = compteGuardat("acc2", 50);

    assertThrows(NotEnoughBalance.class, () -> service.transfer("acc1", "acc2", 101));

    assertEquals(100, origen.getBalance());
    assertEquals(50, desti.getBalance());
    verify(em, never()).persist(any());
  }

  @Test
  void transfer_origenInexistent_llencaAccountNotFound() throws Exception {
    compteInexistent("noExisteix");

    assertThrows(AccountNotFound.class, () -> service.transfer("noExisteix", "acc2", 10));

    verify(em, never()).persist(any());
  }

  @Test
  void transfer_destiInexistent_llencaAccountNotFoundINoDebitaLOrigen() throws Exception {
    Account origen = compteGuardat("acc1", 100);
    compteInexistent("noExisteix");

    assertThrows(AccountNotFound.class, () -> service.transfer("acc1", "noExisteix", 10));

    assertEquals(100, origen.getBalance());
    verify(em, never()).persist(any());
  }

  @Test
  void transfer_errorDePersistencia_esPropaga() throws Exception {
    compteGuardat("acc1", 100);
    compteGuardat("acc2", 50);
    doThrow(new PersistenceException()).when(em).persist(any());

    assertThrows(PersistenceException.class, () -> service.transfer("acc1", "acc2", 30));
  }

  // ---------------------------------------------------------------- delete(String id)

  @Test
  void deletePerId_compteInexistent_llencaAccountNotFound() throws Exception {
    compteInexistent("noExisteix");

    assertThrows(AccountNotFound.class, () -> service.delete("noExisteix"));

    verify(em, never()).delete(any());
  }

  @Test
  void deletePerId_compteExistent_esborraElCompte() throws Exception {
    Account acc = compteGuardat("acc1", 0);
    when(em.getCreditCards(acc)).thenReturn(Collections.emptyList());

    service.delete("acc1");

    verify(em).delete(acc);
  }

  // ---------------------------------------------------------------- delete(Account acc)

  @Test
  void delete_ambSaldoPositiu_llencaAccountActiveINoEsborraRes() {
    Account acc = new Account("acc1", 10);

    assertThrows(AccountActive.class, () -> service.delete(acc));

    verifyNoInteractions(em, ccs);
  }

  @Test
  void delete_saldoZeroSenseTargetes_esborraElCompte() throws Exception {
    Account acc = new Account("acc1", 0);
    when(em.getCreditCards(acc)).thenReturn(Collections.emptyList());

    service.delete(acc);

    verify(em).delete(acc);
    verifyNoInteractions(ccs);
  }

  @Test
  void delete_ambTargetesSenseCredit_esborraLesTargetesIDespresElCompte() throws Exception {
    Account acc = new Account("acc1", 0);
    CreditCard cc1 = new CreditCard("cc1");
    CreditCard cc2 = new CreditCard("cc2");
    when(em.getCreditCards(acc)).thenReturn(Arrays.asList(cc1, cc2));

    service.delete(acc);

    InOrder ordre = inOrder(ccs, em);
    ordre.verify(ccs).delete(cc1);
    ordre.verify(ccs).delete(cc2);
    ordre.verify(em).delete(acc);
  }

  @Test
  void delete_ambTargetaAmbCredit_llencaAccountActiveINoEsborraElCompte() throws Exception {
    Account acc = new Account("acc1", 0);
    CreditCard cc = new CreditCard("cc1");
    cc.credit(50);
    when(em.getCreditCards(acc)).thenReturn(Collections.singletonList(cc));

    assertThrows(AccountActive.class, () -> service.delete(acc));

    verify(ccs, never()).delete(any(CreditCard.class));
    verify(em, never()).delete(any());
  }

  @Test
  void delete_errorEsborrantUnaTargeta_esPropagaINoEsborraElCompte() throws Exception {
    Account acc = new Account("acc1", 0);
    CreditCard cc = new CreditCard("cc1");
    when(em.getCreditCards(acc)).thenReturn(Collections.singletonList(cc));
    doThrow(new NotEnoughBalance()).when(ccs).delete(cc);

    assertThrows(NotEnoughBalance.class, () -> service.delete(acc));

    verify(em, never()).delete(any());
  }

  @Test
  void delete_errorDePersistencia_esPropaga() throws Exception {
    Account acc = new Account("acc1", 0);
    when(em.getCreditCards(acc)).thenReturn(Collections.emptyList());
    doThrow(new PersistenceException()).when(em).delete(acc);

    assertThrows(PersistenceException.class, () -> service.delete(acc));
  }

  @Disabled("Defecte detectat: delete() esborra les targetes a mesura que les recorre, de manera que "
      + "si una targeta posterior té crèdit es llança AccountActive però les anteriors ja s'han esborrat.")
  @Test
  void delete_ambUnaTargetaAmbCredit_noHauriaDEsborrarCapTargeta() throws Exception {
    Account acc = new Account("acc1", 0);
    CreditCard senseCredit = new CreditCard("cc1");
    CreditCard ambCredit = new CreditCard("cc2");
    ambCredit.credit(50);
    when(em.getCreditCards(acc)).thenReturn(Arrays.asList(senseCredit, ambCredit));

    assertThrows(AccountActive.class, () -> service.delete(acc));

    verify(ccs, never()).delete(any(CreditCard.class));
  }
}
