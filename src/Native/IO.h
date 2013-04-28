#pragma once

#include "Common.h"
#include "Data/Queue.h"
#include "Memory/Memory.h"
#include "VM/Fiber.h"
#include "VM/Object.h"
#include "VM/Scheduler.h"

namespace magpie
{
  void defineIONatives(VM& vm);

  class BufferObject;
  class FileObject;
  class File;
  class StreamObject;

  // A task for a file system operation.
  class FSTask : public Task
  {
  public:
    FSTask(gc<Fiber> fiber);
    ~FSTask();

    uv_fs_t* request() { return &fs_; }

    virtual void kill();

    uv_fs_t fs_;
  };

  // A task for a file system operation.
  class FSReadTask : public FSTask
  {
  public:
    FSReadTask(gc<Fiber> fiber, int bufferSize);

    gc<BufferObject> buffer() { return buffer_; }

    virtual void reach();

  private:
    gc<BufferObject> buffer_;
  };

  // A task for reading from a stream.
  class StreamReadTask : public Task
  {
  public:
    StreamReadTask(gc<Fiber> fiber, gc<StreamObject> stream);

    virtual void reach();

  private:
    gc<StreamObject> stream_;
  };
  
  class BufferObject : public Object
  {
  public:
    static gc<BufferObject> create(int count, const char* data = NULL);

    virtual gc<ClassObject> getClass(VM& vm) const;

    virtual gc<String> toString() const;

    int count() const { return count_; }

    // Truncates the buffer's count to [count], which cannot be longer than the
    // current count. Does not free up memory, just logically shortens it.
    void truncate(int count);

    // Gets a raw pointer to the buffer data.
    void* data() { return bytes_; }

    unsigned char get(int index) { return bytes_[index]; }
    void set(int index, unsigned char value) { bytes_[index] = value; }

  private:
    BufferObject(int count)
    : Object(),
      count_(count)
    {}

    // Number of bytes in the buffer.
    int count_;
    unsigned char bytes_[FLEXIBLE_SIZE];
  };

  class FileObject : public Object
  {
  public:
    static void open(gc<Fiber> fiber, gc<String> path);

    FileObject(uv_file file)
    : Object(),
      file_(file),
      isOpen_(true)
    {}

    bool isOpen() const { return isOpen_; }

    // Gets the size of this file and sends it to [fiber].
    void getSize(gc<Fiber> fiber);

    // Reads [size] bytes from this file and sends the result as a buffer to
    // [fiber].
    void readBytes(gc<Fiber> fiber, int size);

    // Closes this file and resumes [fiber] when done.
    void close(gc<Fiber> fiber);

    virtual gc<ClassObject> getClass(VM& vm) const;
    virtual gc<String> toString() const;
    virtual void reach();

  private:
    // TODO(bob): Need some kind of finalization system so that files that get
    // GC'd get closed.
    uv_file file_;

    bool isOpen_;
  };

  class StreamObject : public Object
  {
  public:
    StreamObject()
    : Object(),
      read_(),
      pending_()
    {}

    virtual gc<ClassObject> getClass(VM& vm) const;
    virtual gc<String> toString() const;

    virtual void reach();

    void add(uv_buf_t data, size_t numRead);

    // Reads a buffer from the stream. If a buffer is already available, returns
    // it immediately. Otherwise, pauses [fiber] and creates a task that will
    // complete when a buffer is available.
    gc<BufferObject> read(gc<Fiber> fiber);

  private:
    // The buffers that have already been read and are ready to be consumed.
    Queue<gc<BufferObject> > read_;

    // The fibers that are waiting to read from this stream. read_ and pending_
    // will not both be non-empty at the same time.
    Queue<gc<Fiber> > pending_;
  };
}

