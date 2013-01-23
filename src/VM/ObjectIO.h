#pragma once

#include <iostream>
#include "uv.h"

#include "Macros.h"
#include "Managed.h"
#include "MagpieString.h"
#include "Object.h"

namespace magpie
{
  class BufferObject;
  class FileObject;
  class File;

  // Unsafe downcasting functions. These must *only* be called after the object
  // has been verified as being the right type.
  gc<BufferObject> asBuffer(gc<Object> obj);
  gc<FileObject> asFile(gc<Object> obj);

  class BufferObject : public Object
  {
  public:
    static gc<BufferObject> create(int count);

    virtual gc<ClassObject> getClass(VM& vm) const;

    virtual gc<String> toString() const;

    int count() const { return count_; }
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

    uv_file file() { return file_; }
    bool isOpen() const { return isOpen_; }

    void read(gc<Fiber> fiber);
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
}
