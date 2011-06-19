package com.stuffwithstuff.magpie.interpreter;

import java.util.concurrent.LinkedBlockingQueue;

public class Channel {
  public Channel() {
    mQueue = new LinkedBlockingQueue<Obj>();
  }
  
  public Channel(int capacity) {
    mQueue = new LinkedBlockingQueue<Obj>(capacity);
  }
  
  public void send(Obj obj) throws InterruptedException {
    mQueue.put(obj);
  }
  
  public Obj receive() throws InterruptedException {
    return mQueue.take();
  }
  
  private final LinkedBlockingQueue<Obj> mQueue;
}
