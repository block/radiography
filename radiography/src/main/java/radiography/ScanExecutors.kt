package radiography

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

public object ScanExecutors {

  internal val mainHandler by lazy {
    Handler(Looper.getMainLooper())
  }

  /**
   * Runs the scanning tasks synchronously without any thread check, posting or exception catching.
   */
  @JvmField
  public val PassthroughExecutor: ScanExecutor = ScanExecutor { callable ->
    callable.call()
  }

  /**
   * Executes the scanning tasks on the provided [delegate] ScanExecutor but catches any
   * exception and returns an error string instead.
   */
  @JvmStatic
  public fun NeverThrowingExecutor(delegate: ScanExecutor): ScanExecutor = ScanExecutor { callable ->
    try {
      delegate.execute(callable)
    } catch (throwable: Throwable) {
      "Exception when scanning: ${throwable.message}"
    }
  }

  /**
   * Runs the scanning tasks and blocks the current thread until completion. If the current thread
   * is the same as the [handler] thread, then the runnable runs immediately without being enqueued.
   * Otherwise, posts the runnable to [handler] and waits for it to complete before returning,
   * throwing if timeout occurs or if the work could not be scheduled, and rethrowing any main
   * thread exception to the calling thread.
   */
  @JvmStatic
  public fun HandlerPostingExecutor(
    handler: Handler,
    timeout: Long,
    timeoutUnit: TimeUnit
  ): ScanExecutor = ScanExecutor { callable ->
    if (handler.looper === Looper.myLooper()) {
      callable.call()
    } else {
      var result: Result<String>? = null
      val latch = CountDownLatch(1)
      val posted = handler.post {
        result = try {
          Result.success(callable.call())
        } catch (throwable: Throwable) {
          Result.failure(throwable)
        }
        latch.countDown()
      }
      check(posted) {
        "Callback not posted, probably because the looper processing the message queue is exiting."
      }
      check(latch.await(timeout, timeoutUnit)) {
        "Could not scan hierarchy from main thread, timed out"
      }
      result!!.getOrThrow()
    }
  }

  /**
   * Runs the scanning tasks synchronously, throwing if the current thread is not the same as the
   * thread of the provided [Looper]. Does not catch any exception.
   */
  @JvmStatic
  public fun LooperEnforcingExecutor(looper: Looper): ScanExecutor = ScanExecutor { callable ->
    check(looper === Looper.myLooper()) {
      "Should be called from ${looper.thread.name}, not ${Thread.currentThread().name}"
    }
    callable.call()
  }
}
