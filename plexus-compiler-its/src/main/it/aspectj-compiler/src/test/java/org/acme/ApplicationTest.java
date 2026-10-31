package org.acme;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApplicationTest {
  @Test
  public void testGreet() {
    assertEquals("Hello Jane", new Application().greet("Jane"));
  }

  @Test
  public void testMain() {
    Application.main(new String[] { "Joe" });
    assertTrue(true);
  }
}
