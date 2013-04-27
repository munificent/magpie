#include <sstream>

#include "Native/IO.h"
#include "VM.h"

namespace magpie
{
  gc<BufferObject> asBuffer(gc<Object> obj)
  {
    return static_cast<BufferObject*>(&(*obj));
  }

  gc<FileObject> asFile(gc<Object> obj)
  {
    return static_cast<FileObject*>(&(*obj));
  }

  gc<StreamObject> asStream(gc<Object> obj)
  {
    return static_cast<StreamObject*>(&(*obj));
  }
  
  NATIVE(bindIO)
  {
    vm.bindClass("io", CLASS_BUFFER, "Buffer");
    vm.bindClass("io", CLASS_FILE, "File");
    vm.bindClass("io", CLASS_STREAM, "Stream");
    return vm.nothing();
  }

  NATIVE(fileClose)
  {
    gc<FileObject> fileObj = asFile(args[0]);
    fileObj->close(&fiber);

    result = NATIVE_RESULT_SUSPEND;
    return NULL;
  }
  
  NATIVE(fileIsOpen)
  {
    gc<FileObject> fileObj = asFile(args[0]);
    return vm.getBool(fileObj->isOpen());
  }
  
  NATIVE(fileOpen)
  {
    FileObject::open(&fiber, asString(args[1]));
    result = NATIVE_RESULT_SUSPEND;
    return NULL;
  }

  NATIVE(fileSize)
  {
    gc<FileObject> fileObj = asFile(args[0]);
    fileObj->getSize(&fiber);
    result = NATIVE_RESULT_SUSPEND;
    return NULL;
  }
  
  NATIVE(fileReadBytesInt)
  {
    gc<FileObject> fileObj = asFile(args[0]);
    fileObj->readBytes(&fiber, asInt(args[1]));
    result = NATIVE_RESULT_SUSPEND;
    return NULL;
  }

  NATIVE(fileStreamBytes)
  {
    gc<StreamObject> stream = new StreamObject();
    // TODO(bob): Hook up to file.
    return stream;
  }
  
  NATIVE(bufferNewSize)
  {
    return BufferObject::create(asInt(args[1]));
  }

  NATIVE(bufferCount)
  {
    gc<BufferObject> buffer = asBuffer(args[0]);
    return new IntObject(buffer->count());
  }

  NATIVE(bufferSubscriptInt)
  {
    // Note: bounds checking is handled by core before calling this.
    gc<BufferObject> buffer = asBuffer(args[0]);
    return new IntObject(buffer->get(asInt(args[1])));
  }
  
  NATIVE(bufferSubscriptSetInt)
  {
    // Note: bounds checking is handled by core before calling this.
    gc<BufferObject> buffer = asBuffer(args[0]);
    // TODO(bob): Need to decide how to handle value outside of byte range.
    buffer->set(asInt(args[1]),
                static_cast<unsigned char>(asInt(args[2])));
    return args[2];
  }

  NATIVE(bufferDecodeAscii)
  {
    gc<BufferObject> buffer = asBuffer(args[0]);
    return new StringObject(
        String::create(reinterpret_cast<char*>(buffer->data()),
                       buffer->count()));
  }

  void defineIONatives(VM& vm)
  {
    DEF_NATIVE(bindIO);
    DEF_NATIVE(fileClose);
    DEF_NATIVE(fileIsOpen);
    DEF_NATIVE(fileOpen);
    DEF_NATIVE(fileSize);
    DEF_NATIVE(fileReadBytesInt);
    DEF_NATIVE(fileStreamBytes);
    DEF_NATIVE(bufferNewSize);
    DEF_NATIVE(bufferCount);
    DEF_NATIVE(bufferSubscriptInt);
    DEF_NATIVE(bufferSubscriptSetInt);
    DEF_NATIVE(bufferDecodeAscii);
  }

  FSTask::FSTask(gc<Fiber> fiber)
  : Task(fiber)
  {
    fs_.data = this;
  }

  FSTask::~FSTask()
  {
    uv_fs_req_cleanup(&fs_);
  }

  void FSTask::kill()
  {
    uv_cancel(reinterpret_cast<uv_req_t*>(&fs_));
  }

  FSReadTask::FSReadTask(gc<Fiber> fiber, int bufferSize)
  : FSTask(fiber)
  {
    buffer_ = BufferObject::create(bufferSize);
  }

  void FSReadTask::reach()
  {
    FSTask::reach();
    buffer_.reach();
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
    return vm.getClass(CLASS_BUFFER);
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

  void BufferObject::truncate(int count)
  {
    ASSERT(count <= count_, "Cannot truncate to a larger size.");
    count_ = count;
  }

  static void openFileCallback(uv_fs_t* handle)
  {
    // TODO(bob): Handle errors!
    Task* task = static_cast<Task*>(handle->data);

    // Note that the file descriptor is returned in [result] and not [file].
    task->complete(new FileObject(handle->result));
  }

  void FileObject::open(gc<Fiber> fiber, gc<String> path)
  {
    FSTask* task = new FSTask(fiber);

    // TODO(bob): Make this configurable.
    int flags = O_RDONLY;
    // TODO(bob): Make this configurable when creating a file.
    int mode = 0;
    uv_fs_open(task->loop(), task->request(), path->cString(), flags, mode,
               openFileCallback);
  }

  static void getSizeCallback(uv_fs_t* handle)
  {
    // TODO(bob): Handle errors!
    Task* task = static_cast<Task*>(handle->data);
    // TODO(bob): Use handle.statbuf after upgrading to latest libuv where
    // that's public.
    uv_statbuf_t* statbuf = static_cast<uv_statbuf_t*>(handle->ptr);
    task->complete(new IntObject(statbuf->st_size));
  }

  void FileObject::getSize(gc<Fiber> fiber)
  {
    FSTask* task = new FSTask(fiber);
    uv_fs_fstat(task->loop(), task->request(), file_, getSizeCallback);
  }

  // TODO(bob): Stream-based code. Saving it for later.
  /*
   static uv_buf_t allocateCallback(uv_handle_t *handle, size_t suggested_size)
   {
   printf("Alloc %ld\n", suggested_size);
   // TODO(bob): Don't use malloc() here.
   return uv_buf_init((char*) malloc(suggested_size), suggested_size);
   }

   static void readCallback(uv_stream_t *stream, ssize_t nread, uv_buf_t buf)
   {
   // TODO(bob): Implement me!
   printf("Read %ld bytes\n", nread);
   }
   */

  static void readBytesCallback(uv_fs_t *request)
  {
    // TODO(bob): Handle errors!
    FSReadTask* task = reinterpret_cast<FSReadTask*>(request->data);

    gc<Object> result = task->buffer();
    if (request->result != 0)
    {
      // Trim the buffer to the actually read size.
      task->buffer()->truncate(request->result);
    }
    else
    {
      // If we read when at EOF, return done.
      result = task->fiber()->vm().getAtom(ATOM_DONE);
    }

    task->complete(result);
  }

  void FileObject::readBytes(gc<Fiber> fiber, int size)
  {
    FSReadTask* task = new FSReadTask(fiber, size);

    // TODO(bob): Check result.
    uv_fs_read(task->loop(), task->request(), file_,
               task->buffer()->data(), task->buffer()->count(), -1,
               readBytesCallback);

    // TODO(bob): Use this for the streaming methods:
    /*
     // TODO(bob): What if you call read on the same file multiple times?
     // Should the pipe be reused?
     // Get a pipe to the file.
     uv_pipe_t* pipe = tasks_.createPipe(fiber);
     uv_pipe_init(loop_, pipe, 0);
     uv_pipe_open(pipe, file->file());

     // TODO(bob): Check result.
     uv_read_start(reinterpret_cast<uv_stream_t*>(pipe), allocateCallback,
     readCallback);
     */
  }

  static void closeFileCallback(uv_fs_t* handle)
  {
    Task* task = static_cast<Task*>(handle->data);

    // Close returns nothing.
    task->complete(NULL);
  }

  void FileObject::close(gc<Fiber> fiber)
  {
    ASSERT(isOpen_, "IO library should not call close on a closed file.");
    // Mark the file closed immediately so other fibers can't try to use it.
    isOpen_ = false;

    FSTask* task = new FSTask(fiber);
    uv_fs_close(task->loop(), task->request(), file_,
                closeFileCallback);
  }

  gc<ClassObject> FileObject::getClass(VM& vm) const
  {
    return vm.getClass(CLASS_FILE);
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

  gc<ClassObject> StreamObject::getClass(VM& vm) const
  {
    return vm.getClass(CLASS_STREAM);
  }

  gc<String> StreamObject::toString() const
  {
    // TODO(bob): Include some kind of ID or something here.
    return String::create("[stream]");
  }
}

