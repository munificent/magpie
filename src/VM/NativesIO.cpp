#include <sstream>

#include "Object.h"
#include "NativesIO.h"
#include "VM.h"

namespace magpie
{
  NATIVE(bindIO)
  {
    vm.bindIO();
    return vm.nothing();
  }

  NATIVE(fileClose)
  {
    gc<FileObject> fileObj = args[0]->asFile();
    fileObj->close(&fiber);

    result = NATIVE_RESULT_SUSPEND;
    return NULL;
  }
  
  NATIVE(fileIsOpen)
  {
    gc<FileObject> fileObj = args[0]->asFile();
    return vm.getBool(fileObj->isOpen());
  }
  
  NATIVE(fileOpen)
  {
    FileObject::open(&fiber, args[1]->asString());
    result = NATIVE_RESULT_SUSPEND;
    return NULL;
  }

  NATIVE(fileRead)
  {
    gc<FileObject> fileObj = args[0]->asFile();
    fileObj->read(&fiber);
    result = NATIVE_RESULT_SUSPEND;
    return NULL;
  }

  NATIVE(bufferNewSize)
  {
    return BufferObject::create(args[1]->asInt());
  }

  NATIVE(bufferCount)
  {
    gc<BufferObject> buffer = args[0]->asBuffer();
    return new IntObject(buffer->count());
  }

  NATIVE(bufferSubscriptInt)
  {
    // Note: bounds checking is handled by core before calling this.
    BufferObject* buffer = args[0]->asBuffer();
    return new IntObject(buffer->get(args[1]->asInt()));
  }
  
  NATIVE(bufferSubscriptSetInt)
  {
    // Note: bounds checking is handled by core before calling this.
    BufferObject* buffer = args[0]->asBuffer();
    // TODO(bob): Need to decide how to handle value outside of byte range.
    buffer->set(args[1]->asInt(),
                static_cast<unsigned char>(args[2]->asInt()));
    return args[2];
  }
}

