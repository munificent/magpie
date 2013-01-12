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
}

