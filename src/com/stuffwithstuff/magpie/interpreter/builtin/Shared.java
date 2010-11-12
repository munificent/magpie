package com.stuffwithstuff.magpie.interpreter.builtin;

import java.lang.annotation.*;

/**
 * Annotates a built-in method as shared. Will be added to the metaclass
 * instead of the main class when processed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Shared { }
