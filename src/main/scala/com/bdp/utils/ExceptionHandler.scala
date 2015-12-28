package com.bdp.utils

import scala.util.control.ControlThrowable

/**
 * Created by zhange on 15/10/27.
 *
 */

trait ExceptionHandler {
  def safely[T](handler: PartialFunction[Throwable, T]): PartialFunction[Throwable, T] = {
    case ex: ControlThrowable                     => throw ex

    // case ex: OutOfMemoryError (Assorted other nasty exceptions you don't want to catch)

    //If it's an exception they handle, pass it on
    case ex: Throwable if handler.isDefinedAt(ex) => handler(ex)

    // If they didn't handle it, rethrow. This line isn't necessary, just for clarity
    case ex: Throwable                            => throw ex
  }
}

// Usage:
/*
def doSomething: Unit = {
  try {
    somethingDangerous
  } catch safely {
    case ex: Throwable => println("AHHH")
  }
}
*/ 