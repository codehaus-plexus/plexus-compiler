package org.acme;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class TestAspect {
  @Before("call(* *(..)) && !within(TestAspect)")
  public void beforeCall(JoinPoint joinPoint) {
    System.out.println(joinPoint);
  }
}
