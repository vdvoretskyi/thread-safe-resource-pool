package test;

import java.util.concurrent.TimeUnit;

public interface ResourcePool<R> {

  void open();
  boolean isOpen();
  void close() throws InterruptedException;
  void closeNow();

  R acquire() throws InterruptedException;
  R acquire(long timeout, TimeUnit timeUnit) throws InterruptedException;
  void release(R resource);

  boolean add(R resource);
  boolean remove(R resource) throws InterruptedException;
  boolean removeNow(R resource);
}
