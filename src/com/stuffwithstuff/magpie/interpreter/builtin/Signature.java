package com.stuffwithstuff.magpie.interpreter.builtin;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Signature {
  String value();
}
