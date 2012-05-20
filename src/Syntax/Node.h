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
  
  // A record pattern field.
  struct PatternField
  {
    PatternField()
    : name(),
      value()
    {}
    
    PatternField(gc<String> name, gc<Pattern> value)
    : name(name),
      value(value)
    {}
    
    gc<String> name;
    gc<Pattern> value;
  };
  
  // A single "catch" pattern in a CatchNode.
  class CatchClause
  {
  public:
    CatchClause()
    : pattern_(),
      body_()
    {}
    
    CatchClause(gc<Pattern> pattern, gc<Node> body)
    : pattern_(pattern),
      body_(body)
    {}
    
    gc<Pattern> pattern() const { return pattern_; }
    gc<Node> body() const { return body_; }
    
  private:
    gc<Pattern> pattern_;
    gc<Node> body_;
  };
  
#include "Node.generated.h"
}

