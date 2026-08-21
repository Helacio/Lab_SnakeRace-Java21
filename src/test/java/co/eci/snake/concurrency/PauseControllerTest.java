package co.eci.snake.concurrency;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PauseControllerTest {

  @Test
  void awaitIfPausedBlocksUntilResumeIsCalled() throws InterruptedException {
    PauseController controller = new PauseController();
    controller.pause();

    CountDownLatch resumed = new CountDownLatch(1);
    AtomicBoolean interrupted = new AtomicBoolean(false);

    Thread waiter = new Thread(() -> {
      try {
        controller.awaitIfPaused();
        resumed.countDown();
      } catch (InterruptedException e) {
        interrupted.set(true);
      }
    });
    waiter.start();

    assertFalse(resumed.await(200, TimeUnit.MILLISECONDS), "Thread should remain blocked while paused");

    controller.resume();

    assertTrue(resumed.await(2, TimeUnit.SECONDS), "Thread should unblock right after resume()");
    waiter.join();
    assertFalse(interrupted.get());
  }

  @Test
  void awaitIfPausedReturnsImmediatelyWhenNotPaused() throws InterruptedException {
    PauseController controller = new PauseController();
    long start = System.nanoTime();
    controller.awaitIfPaused();
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;
    assertTrue(elapsedMs < 100);
  }
}
