package org.acme;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class MyAnnotationDrivenAspect {
  @Before("execution(static * *(..)) && within(Application)")
  public void myAdvice(JoinPoint joinPoint) {
    System.out.println(joinPoint);
  }
}
