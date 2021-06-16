package org.acme;

public class Application {
  public static void main(String[] args) {
    System.out.println("Running application");
    new Application().greet(args[0]);
  }

  public String greet(String name) {
    return "Hello " + name;
  }
}
