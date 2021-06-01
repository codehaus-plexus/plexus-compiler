package org.acme;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
