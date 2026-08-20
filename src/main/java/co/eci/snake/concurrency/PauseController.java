package co.eci.snake.concurrency;

public final class PauseController {
  private final Object lock = new Object();
  private volatile boolean paused = false;

  public void pause() {
    synchronized (lock) {
      paused = true;
    }
  }

  public void resume() {
    synchronized (lock) {
      paused = false;
      lock.notifyAll();
    }
  }

  public void awaitIfPaused() throws InterruptedException {
    synchronized (lock) {
      while (paused) lock.wait();
    }
  }
}
