package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResourcePoolImplTest {

  private ResourcePool<String> resourcePool;

  @BeforeEach
  void setUp() {
    resourcePool = new ResourcePoolImpl<>();
  }

  @Test
  void closeNotOpenedPool() {
    assertThrows(IllegalStateException.class,
        () -> resourcePool.close());
  }

  @Test
  void closeNowNotOpenedPool() {
    assertThrows(IllegalStateException.class,
        () -> resourcePool.closeNow());
  }

  @Test
  void reopen() throws InterruptedException {
    resourcePool.open();
    resourcePool.close();
    assertThrows(IllegalStateException.class,
        () -> resourcePool.open());
  }

  @Test
  void acquireResourceBeforeOpeningPool() {
    assertThrows(IllegalStateException.class,
        () -> resourcePool.acquire());
  }

  @Test
  void acquireResourceAfterClose() throws InterruptedException {
    resourcePool.open();
    resourcePool.close();
    assertThrows(IllegalStateException.class,
        () -> resourcePool.acquire());
  }

  @Test
  void acquireResource() throws InterruptedException {
    resourcePool.open();
    resourcePool.add("r1");
    assertEquals(resourcePool.acquire(), "r1");
  }

  @Test
  void acquireResourceWithTimeout() throws InterruptedException {
    resourcePool.open();
    resourcePool.add("r1");
    assertEquals(resourcePool.acquire(1, TimeUnit.MILLISECONDS), "r1");
  }

  @Test
  void releaseResource() throws InterruptedException {
    resourcePool.open();
    resourcePool.add("r1");
    resourcePool.acquire();
    resourcePool.release("r1");
    assertEquals(resourcePool.acquire(), "r1");
  }

  @Test
  void releaseNotAcuiredResource() {
    resourcePool.open();
    assertThrows(IllegalArgumentException.class,
        () -> resourcePool.release("r1"));
  }

  @Test
  void removeResource() throws InterruptedException {
    resourcePool.open();
    resourcePool.add("r1");
    assertTrue(resourcePool.remove("r1"));
    assertFalse(resourcePool.remove("r1"));
  }

  @Test
  void blockOnAcquireTillPoolIsEmpty() throws InterruptedException {
    resourcePool.open();

    new Thread(() -> {
      try {
        Thread.sleep( 1000 );
        resourcePool.add("r1");
      }
      catch (final InterruptedException e) {
        e.printStackTrace();
      }
    }).start();

    final long timeStart = System.nanoTime();
    resourcePool.acquire();
    assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timeStart) > 1000);
  }

  @Test
  void blockOnRemoveTillResourceNotReleased() throws InterruptedException {
    resourcePool.open();

    resourcePool.add("r1");
    resourcePool.acquire();

    final Thread releasingThread = new Thread(() -> {
      try {
        Thread.sleep( 1000 );
        resourcePool.release("r1");
      }
      catch (final InterruptedException e) {
        e.printStackTrace();
      }
    });
    releasingThread.start();

    final long timeStart = System.nanoTime();
    resourcePool.remove("r1");
    assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timeStart) > 1000);
  }
}