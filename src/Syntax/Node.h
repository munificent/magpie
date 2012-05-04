#pragma once

#include "Array.h"
#include "Macros.h"
#include "Managed.h"
#include "Token.h"

#define DECLARE_PATTERN(type)                                   \
  virtual void accept(PatternVisitor& visitor, int arg) const   \
  {                                                             \
    visitor.visit(*this, arg);                                  \
  }                                                             \
  virtual const type* as##type() const { return this; }

namespace magpie
{
  using std::ostream;

  class Node;
  class NodeVisitor;
  class Pattern;
  class VariablePattern;
  
  // Visitor pattern for dispatching on AST pattern nodes. Implemented by the
  // compiler.
  class PatternVisitor
  {
  public:
    virtual ~PatternVisitor() {}
    
    virtual void visit(const VariablePattern& pattern, int dest) = 0;
    
  protected:
    PatternVisitor() {}
    
  private:
    NO_COPY(PatternVisitor);
  };
  
  class ModuleAst : public Managed
  {
  public:
    ModuleAst(Array<gc<Node> >& methods);

    const Array<gc<Node> > methods() const { return methods_; }

    virtual void reach();
  private:
    Array<gc<Node> > methods_;
  };

#include "Node.generated.h"

  // Base class for all AST pattern node classes.
  class Pattern : public Managed
  {
  public:
    virtual ~Pattern() {}

    // The visitor pattern.
    virtual void accept(PatternVisitor& visitor, int arg) const = 0;

    // Dynamic casts.
    virtual const VariablePattern* asVariablePattern() const { return NULL; }
  };

  // A variable pattern.
  class VariablePattern : public Pattern
  {
  public:
    VariablePattern(gc<String> name);

    DECLARE_PATTERN(VariablePattern);

    gc<String> name() const { return name_; }

    virtual void reach();
    virtual void trace(std::ostream& out) const;
  private:
    gc<String> name_;
  };
}

