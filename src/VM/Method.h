#pragma once

#include "Array.h"
#include "Ast.h"
#include "Bytecode.h"
#include "Macros.h"
#include "Managed.h"
#include "Memory.h"

namespace magpie
{
  class DefExpr;
  class Module;
  class Object;
  class Pattern;
  class VM;
  
  // A compiled chunk of bytecode that can be executed by a Fiber.
  class Chunk : public Managed
  {
  public:
    Chunk()
    : code_(),
      constants_(),
      numSlots_(0)
    {}
    
    void setCode(const Array<instruction>& code,
                 int maxSlots);
    
    inline const Array<instruction>& code() const { return code_; }
    
    int addConstant(gc<Object> constant);
    gc<Object> getConstant(int index) const;
    
    int numSlots() const { return numSlots_; }

    void debugTrace() const;
    void debugTrace(instruction ins) const;

    virtual void reach();
    
  private:
    Array<instruction> code_;
    Array<gc<Object> > constants_;
    
    int numSlots_;
    
    NO_COPY(Chunk);
  };
  
  // Intermediate representation of a single method in a multimethod. This is
  // produced during module compilation and "halfway" compiles the method to
  // bytecode. The final compilation to bytecode occurs once all methods for a
  // multimethod are known. The Expr for the body here should already be
  // resolved.
  class Method : public Managed
  {
  public:
    Method(Module* module, gc<DefExpr> def)
    : module_(module),
      def_(def)
    {}
    
    Module* module() { return module_; }
    gc<DefExpr> def() { return def_; }

    virtual void reach();
    
  private:
    Module* module_;
    gc<DefExpr> def_;
  };

  // The relative ordering of two methods. When a method comes "before" another,
  // that means it is more specialized and would be preferred over the other
  // when given an argument that matches both.
  enum MethodOrder
  {
    // The first method is more specialized than the second.
    ORDER_BEFORE,

    // The second method is more specialized than the first.
    ORDER_AFTER,

    // The two methods are equivalent. This means the patterns have collided.
    ORDER_EQUAL,

    // The two methods don't have an ordering relative to each other.
    ORDER_NONE
  };
  
  class Multimethod : public Managed
  {
  public:
    Multimethod(gc<String> signature);
    
    gc<String> signature() { return signature_; }
    gc<Chunk> getChunk(VM& vm);
    
    Array<gc<Method> >& methods() { return methods_; }
    
    void addMethod(gc<Method> method);

    virtual void reach();

  private:
    void sort(VM& vm);
    MethodOrder compare(gc<Method> a, gc<Method> b);

    // Given an array of orders, determines the overall ordering. This is used
    // to determine how a pair of records are ordered given the ordering of all
    // of their elements.
    MethodOrder unifyOrders(const Array<MethodOrder>& orders);

    gc<String> signature_;
    gc<Chunk> chunk_;
    Array<gc<Method> > methods_;
  };

  class PatternComparer : public PatternVisitor
  {
  public:
    static MethodOrder compare(gc<Pattern> a, gc<Pattern> b);
    
    PatternComparer(Pattern& other, MethodOrder* result)
    : other_(other),
      result_(result)
    {}
    
    virtual void visit(RecordPattern& node, int dummy);
    virtual void visit(TypePattern& node, int dummy);
    virtual void visit(ValuePattern& node, int dummy);
    virtual void visit(VariablePattern& node, int dummy);
    virtual void visit(WildcardPattern& node, int dummy);

  private:
    Pattern& other_;
    MethodOrder* result_;
  };
}