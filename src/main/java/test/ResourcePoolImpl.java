package test;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class ResourcePoolImpl<R> implements ResourcePool<R> {

  private volatile boolean isOpened = false;
  private volatile boolean isClosed = false;

  private final BlockingQueue<R> availableResources = new LinkedBlockingQueue<>();
  private final Map<R, CountDownLatch> acquiredResources = new ConcurrentHashMap<>();

  @Override
  public void open() {
    if (isOpened) {
      throw new IllegalStateException("Pool is already opened!");
    }
    if (isClosed) {
      throw new IllegalStateException("Pool is already closed!");
    }

    //preliminarily create some resources if needed

    isOpened = true;
  }

  @Override
  public boolean isOpen() {
    return isOpened;
  }

  @Override
  public void close() throws InterruptedException {
    if (!isOpened) {
      throw new IllegalStateException("Pool is already closed!");
    }

    isClosed = true;
    isOpened = false;

    for (CountDownLatch countDownLatch : acquiredResources.values()) {
      countDownLatch.await();
    }
  }

  @Override
  public void closeNow() {
    if (!isOpened) {
      throw new IllegalStateException("Pool is already closed!");
    }

    isClosed = true;
    isOpened = false;
  }

  @Override
  public R acquire() throws InterruptedException {
    if (!isOpened) {
      throw new IllegalStateException("The pool is closed");
    }
    final R resource = availableResources.take();
    acquiredResources.put(resource, new CountDownLatch(1));
    return resource;
  }

  @Override
  public R acquire(final long timeout, final TimeUnit timeUnit) throws InterruptedException {
    if (!isOpened) {
      throw new IllegalStateException("The pool is closed");
    }
    final R resource = availableResources.poll(timeout, timeUnit);
    if (resource != null) {
      acquiredResources.put(resource, new CountDownLatch(1));
    }
    return resource;
  }

  @Override
  public void release(final R resource) {
    final CountDownLatch countDownLatch = acquiredResources.get(resource);
    if (countDownLatch == null) {
      throw new IllegalArgumentException("Resource was not acquired");
    }
    availableResources.add(resource);
    countDownLatch.countDown();
  }

  @Override
  public boolean add(final R resource) {
    return availableResources.add(resource);
  }

  @Override
  public boolean remove(final R resource) throws InterruptedException {
    final CountDownLatch countDownLatch = acquiredResources.get(resource);
    if (countDownLatch != null) {
      countDownLatch.await();
    }
    return availableResources.remove(resource);
  }

  @Override
  public boolean removeNow(final R resource) {
    return availableResources.remove(resource);
  }
}
