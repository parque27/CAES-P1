package org.udg.caes.banking;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.udg.caes.banking.entity.Account;
import org.udg.caes.banking.entity.Client;
import org.udg.caes.banking.entity.CreditCard;
import org.udg.caes.banking.exceptions.AccountActive;
import org.udg.caes.banking.exceptions.ClientNotFound;
import org.udg.caes.banking.exceptions.EntityNotFound;
import org.udg.caes.banking.manager.EntityManager;
import org.udg.caes.banking.service.AccountService;
import org.udg.caes.banking.service.ClientService;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaris de {@link ClientService}.
 *
 * Es substitueixen per mocks l'EntityManager i l'AccountService (que té els seus propis
 * tests a TestAccountService). Clients, comptes i targetes són objectes reals.
 * Alguns escenaris reprodueixen les dades del Main (clients cl1 i cl2).
 */
@ExtendWith(MockitoExtension.class)
public class TestClientService {

  @Mock EntityManager em;
  @Mock AccountService as;
  @InjectMocks ClientService service;

  /** Crea un client real i fa que l'EntityManager el retorni quan es demani pel seu id. */
  private Client clientGuardat(String id) throws EntityNotFound {
    Client cl = new Client(id);
    when(em.get(id, Client.class)).thenReturn(cl);
    return cl;
  }

  /** Targeta real amb un crèdit ja disposat. */
  private CreditCard targeta(String id, long credit) {
    CreditCard cc = new CreditCard(id);
    cc.credit(credit);
    return cc;
  }

  // ---------------------------------------------------------------- getClient

  @Test
  void getClient_existent_retornaElClient() throws Exception {
    Client cl = clientGuardat("cl1");

    assertSame(cl, service.getClient("cl1"));
  }

  @Test
  void getClient_inexistent_converteixEntityNotFoundEnClientNotFound() throws Exception {
    when(em.get("noExisteix", Client.class)).thenThrow(new EntityNotFound());

    assertThrows(ClientNotFound.class, () -> service.getClient("noExisteix"));
  }

  // ---------------------------------------------------------------- delete

  @Test
  void delete_clientInexistent_llencaClientNotFound() throws Exception {
    when(em.get("noExisteix", Client.class)).thenThrow(new EntityNotFound());

    assertThrows(ClientNotFound.class, () -> service.delete("noExisteix"));

    verifyNoInteractions(as);
  }

  @Test
  void delete_clientAmbComptes_esborraTotsElsSeusComptes() throws Exception {
    Client cl = clientGuardat("cl1");
    Account acc1 = new Account("acc1", 0);
    Account acc2 = new Account("acc2", 0);
    when(em.getClientAccounts(cl)).thenReturn(Arrays.asList(acc1, acc2));

    service.delete("cl1");

    verify(as).delete(acc1);
    verify(as).delete(acc2);
  }

  @Test
  void delete_clientSenseComptes_noEsborraCapCompte() throws Exception {
    Client cl = clientGuardat("cl1");
    when(em.getClientAccounts(cl)).thenReturn(Collections.emptyList());

    service.delete("cl1");

    verifyNoInteractions(as);
  }

  @Test
  void delete_compteActiu_propagaAccountActiveIAturaElProces() throws Exception {
    Client cl = clientGuardat("cl1");
    Account actiu = new Account("acc1", 100);
    Account buit = new Account("acc2", 0);
    when(em.getClientAccounts(cl)).thenReturn(Arrays.asList(actiu, buit));
    doThrow(new AccountActive()).when(as).delete(actiu);

    assertThrows(AccountActive.class, () -> service.delete("cl1"));

    verify(as, never()).delete(buit);
  }

  @Disabled("Defecte detectat: ClientService.delete() esborra els comptes del client "
      + "però mai esborra el client (no crida em.delete(client)).")
  @Test
  void delete_hauriaDEsborrarTambeElClient() throws Exception {
    Client cl = clientGuardat("cl1");
    when(em.getClientAccounts(cl)).thenReturn(Collections.emptyList());

    service.delete("cl1");

    verify(em).delete(cl);
  }

  // ---------------------------------------------------------------- getBalance

  @Test
  void getBalance_clientSenseComptes_retornaZero() {
    Client cl = new Client("cl1");
    when(em.getClientAccounts(cl)).thenReturn(Collections.emptyList());

    assertEquals(0, service.getBalance(cl));
  }

  @Test
  void getBalance_sumaElsSaldosIIgnoraLesTargetes() {
    // Dades del client cl1 del Main: 100 + 400 = 500
    Client cl = new Client("cl1");
    when(em.getClientAccounts(cl)).thenReturn(Arrays.asList(new Account("acc11", 100), new Account("acc12", 400)));

    assertEquals(500, service.getBalance(cl));
    verify(em, never()).getCreditCards(any());
  }

  // ---------------------------------------------------------------- getRealBalance

  @Test
  void getRealBalance_clientSenseComptes_retornaZero() {
    Client cl = new Client("cl1");
    when(em.getClientAccounts(cl)).thenReturn(Collections.emptyList());

    assertEquals(0, service.getRealBalance(cl));
  }

  @Test
  void getRealBalance_comptesSenseTargetes_coincideixAmbElSaldo() {
    Client cl = new Client("cl1");
    Account acc1 = new Account("acc1", 100);
    Account acc2 = new Account("acc2", 400);
    when(em.getClientAccounts(cl)).thenReturn(Arrays.asList(acc1, acc2));
    when(em.getCreditCards(acc1)).thenReturn(Collections.emptyList());
    when(em.getCreditCards(acc2)).thenReturn(Collections.emptyList());

    assertEquals(500, service.getRealBalance(cl));
  }

  @Test
  void getRealBalance_restaElCreditDeLesTargetesDeCadaCompte() {
    // Dades del client cl1 del Main: 100 + 400 - 50 = 450
    Client cl = new Client("cl1");
    Account acc11 = new Account("acc11", 100);
    Account acc12 = new Account("acc12", 400);
    when(em.getClientAccounts(cl)).thenReturn(Arrays.asList(acc11, acc12));
    when(em.getCreditCards(acc11)).thenReturn(Collections.singletonList(targeta("cc111", 50)));
    when(em.getCreditCards(acc12)).thenReturn(Collections.emptyList());

    assertEquals(450, service.getRealBalance(cl));
  }

  @Test
  void getRealBalance_diversesTargetesAlMateixCompte() {
    // Dades del client cl2 del Main: 300 - 110 - 90 = 100
    Client cl = new Client("cl2");
    Account acc21 = new Account("acc21", 300);
    when(em.getClientAccounts(cl)).thenReturn(Collections.singletonList(acc21));
    when(em.getCreditCards(acc21)).thenReturn(Arrays.asList(targeta("cc211", 110), targeta("cc212", 90)));

    assertEquals(100, service.getRealBalance(cl));
  }

  @Test
  void getRealBalance_potSerNegatiuSiElCreditSuperaElSaldo() {
    Client cl = new Client("cl1");
    Account acc = new Account("acc1", 50);
    when(em.getClientAccounts(cl)).thenReturn(Collections.singletonList(acc));
    when(em.getCreditCards(acc)).thenReturn(Collections.singletonList(targeta("cc1", 80)));

    assertEquals(-30, service.getRealBalance(cl));
  }
}
