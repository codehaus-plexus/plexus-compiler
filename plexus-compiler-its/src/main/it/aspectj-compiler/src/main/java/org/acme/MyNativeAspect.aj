package org.acme;

public aspect MyNativeAspect {
  before() : execution(!static * *(..)) && within(Application) {
    System.out.println(thisJoinPoint);
  }
}
