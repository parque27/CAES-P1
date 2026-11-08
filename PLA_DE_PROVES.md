# Pla de proves - Pràctica 1 CAES

## Què es prova i què no

| Classe | Es prova? | Motiu |
|---|---|---|
| `AccountService` | Sí | Lògica de negoci principal: transferències i esborrat amb regles (saldo, targetes amb crèdit). |
| `CreditCardService` | Sí | Control del límit de crèdit, liquidació de la targeta contra el compte, esborrat i invalidació. |
| `ClientService` | Sí | Càlcul de saldo i saldo real, esborrat en cascada dels comptes. |
| `Account` | Sí | `debit()` té lògica pròpia (comprova el saldo i desfà l'operació). |
| `CreditCard` | Sí | Estat del crèdit (`credit`, `reset`) del qual depèn `CreditCardService`. |
| `Client` | Mínimament | No té lògica; un sol test perquè `getId()` no l'executa cap altre test. |
| `EntityManager` | No | És una interfície sense codi: als tests se substitueix per un *mock*. |
| Excepcions | No | Classes buides; s'instancien indirectament des dels altres tests. |
| Package `FAKE` | No | L'enunciat diu explícitament que no s'ha de testejar. També s'exclou de l'informe de JaCoCo. |

## Estratègia

Cada test case prova **una sola classe** de forma aïllada (tests unitaris). Per aconseguir-ho, les dependències de la classe sota prova se substitueixen per *mocks* de Mockito: l'`EntityManager` a tots els serveis, i a més l'`AccountService` dins `ClientService` i el `CreditCardService` dins `AccountService`. Els mocks s'injecten amb `@InjectMocks`, que funciona tot i que els camps `@Inject` són de visibilitat de paquet i els tests són a `org.udg.caes.banking`, tal com demana l'enunciat.

Les entitats (`Account`, `CreditCard`, `Client`) es fan servir com a objectes reals, perquè són senzilles i això permet comprovar l'estat final (saldos i crèdits). L'única excepció és quan cal una targeta amb límit de crèdit: `CreditCard` no té cap manera de fixar `maxCredit`, i per això es fa servir un *spy* que retorna el límit desitjat.

Per dissenyar els casos s'han fet servir particions d'equivalència (cas correcte, entitat inexistent, saldo o crèdit insuficient, error de persistència) i valors límit (operacions que deixen el saldo exactament a 0, crèdit exactament igual al límit, una unitat per sobre o per sota). A més del resultat, es verifiquen les interaccions amb la persistència: què es persisteix o s'esborra, què **no** s'ha de tocar quan hi ha un error, i l'ordre de les operacions quan és rellevant (per exemple, liquidar una targeta abans d'esborrar-la).

## Inventari de tests

| Test case | Tests | Cobreix |
|---|---|---|
| `TestAccount` | 5 | Constructor, `credit`, `debit` (correcte, frontera a 0, saldo insuficient amb rollback). |
| `TestCreditCard` | 3 | Estat inicial, acumulació de crèdit, `reset`. |
| `TestClient` | 1 | Constructor i `getId`. |
| `TestCreditCardService` | 14 | `credit` (dins el límit, frontera, excés, targeta sense límit, error persistència), `charge` (correcte, frontera, saldo insuficient, error), `delete` (liquidació i esborrat en ordre, saldo insuficient, error), `invalidate` (desactiva abans de persistir, error). |
| `TestAccountService` | 16 + 1 desactivat | `getAccount` (trobat / no trobat), `transfer` (correcte, frontera, saldo insuficient, origen i destí inexistents, error), `delete(String)` i `delete(Account)` (saldo positiu, sense targetes, targetes sense crèdit en ordre, targeta amb crèdit, propagació d'errors). |
| `TestClientService` | 13 + 1 desactivat | `getClient`, `delete` (inexistent, amb comptes, sense comptes, compte actiu que atura el procés), `getBalance` i `getRealBalance` (sense comptes, sense targetes, amb targetes, saldo real negatiu). Alguns casos reprodueixen les dades del `Main`. |

## Defectes detectats al codi

Els dos primers estan documentats com a tests `@Disabled`: descriuen el comportament esperat, fallarien amb el codi actual, i no es poden arreglar perquè `src/main/java` no es pot modificar. Maven els compta com a *skipped* i el build continua acabant amb `BUILD SUCCESS`.

1. **`ClientService.delete` no esborra el client.** Esborra els seus comptes però mai crida `em.delete(client)`, a diferència dels altres `delete`.
2. **Esborrat parcial (no atòmic).** `AccountService.delete(Account)` esborra les targetes a mesura que les recorre; si una de posterior té crèdit, llança `AccountActive` però les anteriors ja s'han esborrat. Passa el mateix a `ClientService.delete` amb els comptes.
3. **`maxCredit` no es pot configurar.** `CreditCard` no té constructor ni setter per al límit, que sempre val 0; amb una targeta real, `CreditCardService.credit` falla sempre per a qualsevol import positiu (`credit_targetaSenseLimitConfigurat_llencaCreditExceeded`).
4. **L'estat `active` no serveix de res.** No té getter, les targetes noves neixen inactives (`false`) i cap operació comprova si la targeta és activa: es pot carregar crèdit a una targeta invalidada.
5. **No es validen imports negatius.** `Account.credit/debit`, `transfer` i `CreditCardService.credit` accepten imports negatius, que inverteixen el sentit de l'operació.
6. **Sense transaccions.** A `transfer` i `charge`, si falla la segona crida a `persist`, la primera entitat ja s'ha persistit i les dades queden inconsistents.
7. **`CreditCardService.delete` amb una targeta sense compte associat** provoca un `NullPointerException`, perquè `getAccountAssociated` retorna `null`.

## Execució

```
mvn test
```

No cal modificar el `pom.xml` proporcionat, que ja inclou JUnit 5, Mockito, Surefire i JaCoCo (Java 21). Amb `mvn test` passa el següent:

- Surefire executa les classes `Test*.java`. Per això els test cases es diuen `TestAccountService`, etc.
- JaCoCo genera l'informe a `target/site/jacoco/index.html`, amb el package `FAKE` exclòs.
- L'objectiu `check` de JaCoCo comprova dues regles de cobertura (excepte al package `exceptions`). La primera és que cap classe pot quedar sense executar. La segona és que cada classe ha de tenir com a mínim un 50 % d'instruccions cobertes.

Els tests executen tots els mètodes i branques de les entitats i dels serveis, de manera que la cobertura esperada és del 100 % i les dues regles es compleixen amb marge.
