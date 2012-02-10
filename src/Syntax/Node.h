#pragma once

#include "Array.h"
#include "Macros.h"
#include "Managed.h"
#include "NodeVisitor.h"
#include "Token.h"

#define DECLARE_NODE(type)                                  \
  virtual void accept(NodeVisitor& visitor, int arg) const  \
  {                                                         \
    visitor.visit(*this, arg);                              \
  }                                                         \
  virtual const type* as##type() const { return this; }

#define DECLARE_PATTERN(type)                                   \
  virtual void accept(PatternVisitor& visitor, int arg) const   \
  {                                                             \
    visitor.visit(*this, arg);                                  \
  }                                                             \
  virtual const type* as##type() const { return this; }

namespace magpie
{
  using std::ostream;
  
  class MethodAst;
  class Pattern;
  
  class ModuleAst : public Managed
  {
  public:
    temp<ModuleAst> static create(Array<gc<MethodAst> >& methods);
    
    const Array<gc<MethodAst> > methods() const { return methods_; }
    
    virtual void reach();

  private:
    ModuleAst(Array<gc<MethodAst> >& methods);
    
    Array<gc<MethodAst> > methods_;
  };
  
  // A method definition.
  class MethodAst : public Managed
  {
  public:
    static temp<MethodAst> create(gc<String> name, gc<Node> body);
    
    gc<String> name() const { return name_; }
    Node& body() const { return *body_; }
    
    virtual void reach();
    virtual void trace(std::ostream& out) const;
    
  private:
    MethodAst(gc<String> name, gc<Node> body);
    
    gc<String> name_;
    gc<Node> body_;
  };
  
  // Base class for all AST node classes.
  class Node : public Managed
  {
  public:
    virtual ~Node() {}

    // The visitor pattern.
    virtual void accept(NodeVisitor& visitor, int arg) const = 0;
    
    // Dynamic casts.
    virtual const BoolNode*     asBoolNode()     const { return NULL; }
    virtual const BinaryOpNode* asBinaryOpNode() const { return NULL; }
    virtual const IfNode*       asIfNode()       const { return NULL; }
    virtual const NameNode*     asNameNode()     const { return NULL; }
    virtual const NumberNode*   asNumberNode()   const { return NULL; }
    virtual const SequenceNode* asSequenceNode() const { return NULL; }
    virtual const VariableNode* asVariableNode() const { return NULL; }
  };
  
  // A binary operator.
  class BinaryOpNode : public Node
  {
  public:
    static temp<BinaryOpNode> create(gc<Node> left, TokenType type,
                                     gc<Node> right);
    
    DECLARE_NODE(BinaryOpNode);
    
    Node& left() const { return *left_; }
    TokenType type() const { return type_; }
    Node& right() const { return *right_; }
    
    virtual void reach();
    virtual void trace(std::ostream& out) const;

  private:
    BinaryOpNode(gc<Node> left, TokenType type, gc<Node> right);
    
    gc<Node>  left_;
    TokenType type_;
    gc<Node>  right_;
  };
  
  // A boolean literal.
  class BoolNode : public Node
  {
  public:
    static temp<BoolNode> create(bool value);
    
    DECLARE_NODE(BoolNode);
    
    bool value() const { return value_; }
    
    virtual void trace(std::ostream& out) const;
    
  private:
    BoolNode(bool value);
    
    bool value_;
  };
   
  // An if-then-else expression.
  class IfNode : public Node
  {
  public:
    static temp<IfNode> create(gc<Node> condition, gc<Node> thenArm,
                               gc<Node> elseArm);
    
    DECLARE_NODE(IfNode);
    
    Node& condition() const { return *condition_; }
    Node& thenArm() const { return *thenArm_; }
    Node& elseArm() const { return *elseArm_; }
    
    virtual void reach();
    virtual void trace(std::ostream& out) const;
    
  private:
    IfNode(gc<Node> condition, gc<Node> thenArm, gc<Node> elseArm);
    
    gc<Node> condition_;
    gc<Node> thenArm_;
    gc<Node> elseArm_;
  };
  
  // A named variable reference.
  class NameNode : public Node
  {
  public:
    static temp<NameNode> create(gc<String> name);
    
    DECLARE_NODE(NameNode);
    
    gc<String> name() const { return name_; }
    
    virtual void reach();
    virtual void trace(std::ostream& out) const;
    
  private:
    NameNode(gc<String> name);
    
    gc<String> name_;
  };
  
  // A number literal.
  class NumberNode : public Node
  {
  public:
    static temp<NumberNode> create(double value);
  
    DECLARE_NODE(NumberNode);
    
    double value() const { return value_; }
    
    virtual void trace(std::ostream& out) const;

  private:
    NumberNode(double value);
    
    double value_;
  };
  
  // A sequence of line- or semicolon-separated expressions.
  class SequenceNode : public Node
  {
  public:
    static temp<SequenceNode> create(const Array<gc<Node> >& expressions);
    
    DECLARE_NODE(SequenceNode);
    
    const Array<gc<Node> >& expressions() const { return expressions_; }
    
    virtual void trace(std::ostream& out) const;
    
  private:
    SequenceNode(const Array<gc<Node> >& expressions);
    
    Array<gc<Node> > expressions_;
  };
  
  // A 'var' or 'val' variable declaration.
  class VariableNode : public Node
  {
  public:
    static temp<VariableNode> create(bool isMutable, gc<Pattern> pattern,
        gc<Node> value);
    
    DECLARE_NODE(VariableNode);
    
    bool isMutable() const { return isMutable_; }
    gc<Pattern> pattern() const { return pattern_; }
    gc<Node> value() const { return value_; }
    
    virtual void trace(std::ostream& out) const;
    
  private:
    VariableNode(bool isMutable, gc<Pattern> pattern, gc<Node> value);
    
    bool isMutable_;
    gc<Pattern> pattern_;
    gc<Node> value_;
  };
  
  // Base class for all AST pattern node classes.
  class Pattern : public Managed
  {
  public:
    virtual ~Pattern() {}
    
    // The visitor pattern.
    virtual void accept(PatternVisitor& visitor, int arg) const = 0;
    
    // Dynamic casts.
    virtual const VariablePattern* asVariable() const { return NULL; }
    
    // Get the number of variables a pattern declares.
    virtual int countVariables() const = 0;
  };
  
  // A variable pattern.
  class VariablePattern : public Pattern
  {
  public:
    static temp<VariablePattern> create(gc<String> name);
    
    DECLARE_PATTERN(VariablePattern);
    
    virtual int countVariables() const { return 1; }
    
    gc<String> name() const { return name_; }
    
    virtual void trace(std::ostream& out) const;
    
  private:
    VariablePattern(gc<String> name);
    
    gc<String> name_;
  };
}

