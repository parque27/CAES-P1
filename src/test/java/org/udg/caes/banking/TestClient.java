package org.udg.caes.banking;

import org.junit.jupiter.api.Test;
import org.udg.caes.banking.entity.Client;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaris de l'entitat {@link Client}.
 *
 * Client no té lògica (només constructor i getter). Es prova de forma mínima per coherència
 * amb la resta d'entitats i perquè getId() no l'executa cap altre test.
 */
public class TestClient {

  @Test
  void constructor_guardaLIdentificador() {
    Client cl = new Client("cl1");
    assertEquals("cl1", cl.getId());
  }
}
