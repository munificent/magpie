#include <sstream>

#include "Array.h"
#include "ObjectIO.h"
#include "VM.h"

namespace magpie
{
  using std::ostream;

  gc<BufferObject> asBuffer(gc<Object> obj)
  {
    return static_cast<BufferObject*>(&(*obj));
  }

  gc<FileObject> asFile(gc<Object> obj)
  {
    return static_cast<FileObject*>(&(*obj));
  }

  gc<BufferObject> BufferObject::create(int count)
  {
    // Allocate enough memory for the buffer and its data.
    void* mem = Memory::allocate(sizeof(BufferObject) +
                                 sizeof(unsigned char) * (count - 1));

    // Construct it by calling global placement new.
    gc<BufferObject> buffer = ::new(mem) BufferObject(count);

    // Fill with zero.
    for (int i = 0; i < count; i++)
    {
      buffer->bytes_[i] = 0;
    }
    
    return buffer;
  }
  
  gc<ClassObject> BufferObject::getClass(VM& vm) const
  {
    return vm.bufferClass();
  }

  gc<String> BufferObject::toString() const
  {
    if (count_ == 0) return String::create("[buffer]");

    gc<String> result = String::create("[buffer");

    if (count_ <= 8)
    {
      // Small buffer, so show the whole contents.
      for (int i = 0; i < count_; i++)
      {
        result = String::format("%s %02x", result->cString(), bytes_[i]);
      }
    }
    else
    {
      // Long buffer, so just shows the first and last few octets.
      for (int i = 0; i < 4; i++)
      {
        result = String::format("%s %02x", result->cString(), bytes_[i]);
      }

      result = String::format("%s ...", result->cString());
      
      for (int i = count_ - 4; i < count_; i++)
      {
        result = String::format("%s %02x", result->cString(), bytes_[i]);
      }
    }

    return String::format("%s]", result->cString());
  }

  void FileObject::open(gc<Fiber> fiber, gc<String> path)
  {
    fiber->scheduler().openFile(fiber, path);
  }

  void FileObject::read(gc<Fiber> fiber)
  {
    fiber->scheduler().read(fiber, this);
  }

  void FileObject::close(gc<Fiber> fiber)
  {
    ASSERT(isOpen_, "IO library should not call close on a closed file.");
    // Mark the file closed immediately so other fibers can't try to use it.
    isOpen_ = false;

    fiber->scheduler().closeFile(fiber, this);
  }

  gc<ClassObject> FileObject::getClass(VM& vm) const
  {
    return vm.fileClass();
  }

  gc<String> FileObject::toString() const
  {
    // TODO(bob): Include some kind of ID or something here.
    return String::create("[file]");
  }

  void FileObject::reach()
  {
    // TODO(bob): How should we handle file_ here?
  }
}
