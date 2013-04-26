#include <sstream>

#include "ObjectIO.h"
#include "NativesIO.h"
#include "VM.h"

namespace magpie
{
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
}

