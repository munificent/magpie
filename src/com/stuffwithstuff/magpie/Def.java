package com.stuffwithstuff.magpie;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Def {
  String value();
}
