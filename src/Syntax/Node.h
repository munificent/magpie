#pragma once

#include "Array.h"
#include "Macros.h"
#include "Managed.h"
#include "Token.h"

namespace magpie
{
  using std::ostream;

  class Node;
  class Pattern;
  
  // A record field.
  struct Field
  {
    Field()
    : name(),
      value()
    {}
    
    Field(gc<String> name, gc<Node> value)
    : name(name),
      value(value)
    {}

    gc<String> name;
    gc<Node> value;
  };
  
#include "Node.generated.h"
}

