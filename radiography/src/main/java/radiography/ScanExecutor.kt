package radiography

import java.util.concurrent.Callable

/**
 * Ensures that scanning happens on the right thread (by e.g. posting work, throwing, or simply
 * not checking, depending on implementation)
 *
 * Some commons executors are:
 *  - [ScanExecutors.NeverThrowingExecutor]
 *  - [ScanExecutors.PassthroughExecutor]
 *  - [ScanExecutors.HandlerPostingExecutor]
 *  - [ScanExecutors.LooperEnforcingExecutor]
 */
public fun interface ScanExecutor {

  /** Returns the result of executing [callable] */
  public fun execute(callable: Callable<String>): String
}
