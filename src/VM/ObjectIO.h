#pragma once

#include <iostream>
#include "uv.h"

#include "Macros.h"
#include "Managed.h"
#include "MagpieString.h"
#include "Object.h"
#include "Scheduler.h"

namespace magpie
{
  class BufferObject;
  class FileObject;
  class File;
  class StreamObject;

  // Unsafe downcasting functions. These must *only* be called after the object
  // has been verified as being the right type.
  gc<BufferObject> asBuffer(gc<Object> obj);
  gc<FileObject> asFile(gc<Object> obj);
  gc<StreamObject> asStream(gc<Object> obj);
  
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

  // A task using a uv_handle_t.
  class HandleTask : public Task
  {
  public:
    HandleTask(gc<Fiber> fiber, uv_handle_t* handle);
    ~HandleTask();
    virtual void kill();

  private:
    uv_handle_t* handle_;
  };
  
  class BufferObject : public Object
  {
  public:
    static gc<BufferObject> create(int count);

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
    : Object()
    {}

    virtual gc<ClassObject> getClass(VM& vm) const;
    virtual gc<String> toString() const;

  private:
  };
}
