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
  
  class ModuleAst
  {
  public:
    ModuleAst(Array<MethodAst*>& methods);
    ~ModuleAst();
    
    const Array<MethodAst*> methods() const { return methods_; }

  private:
    Array<MethodAst*> methods_;
  };
  
  // A method definition.
  class MethodAst
  {
  public:
    MethodAst(gc<String> name, Pattern* parameter, Node* body);
    ~MethodAst();
    
    gc<String> name() const { return name_; }
    const Pattern* parameter() const { return parameter_; }
    Node& body() const { return *body_; }
    
  private:
    gc<String> name_;
    Pattern* parameter_;
    Node* body_;
  };
  
  // Base class for all AST node classes.
  class Node
  {
  public:
    Node(const SourcePos& pos)
    : pos_(pos)
    {}
    
    virtual ~Node() {}

    // The visitor pattern.
    virtual void accept(NodeVisitor& visitor, int arg) const = 0;
    
    // Dynamic casts.
    virtual const BoolNode*     asBoolNode()     const { return NULL; }
    virtual const BinaryOpNode* asBinaryOpNode() const { return NULL; }
    virtual const CallNode*     asCallNode()     const { return NULL; }
    virtual const IfNode*       asIfNode()       const { return NULL; }
    virtual const NameNode*     asNameNode()     const { return NULL; }
    virtual const NumberNode*   asNumberNode()   const { return NULL; }
    virtual const SequenceNode* asSequenceNode() const { return NULL; }
    virtual const StringNode*   asStringNode()   const { return NULL; }
    virtual const VariableNode* asVariableNode() const { return NULL; }
    
    const SourcePos& pos() const { return pos_; }
  
  private:
    SourcePos pos_;
  };
  
  // A binary operator.
  class BinaryOpNode : public Node
  {
  public:
    BinaryOpNode(const SourcePos& pos,
                 Node* left, TokenType type, Node* right);
    ~BinaryOpNode();
    
    DECLARE_NODE(BinaryOpNode);
    
    Node& left() const { return *left_; }
    TokenType type() const { return type_; }
    Node& right() const { return *right_; }
    
    virtual void trace(std::ostream& out) const;

  private:
    Node*  left_;
    TokenType type_;
    Node*  right_;
  };
  
  // A boolean literal.
  class BoolNode : public Node
  {
  public:
    BoolNode(const SourcePos& pos, bool value);
    
    DECLARE_NODE(BoolNode);
    
    bool value() const { return value_; }
    
    virtual void trace(std::ostream& out) const;
    
  private:
    bool value_;
  };
  
  // A method call.
  class CallNode : public Node
  {
  public:
    CallNode(const SourcePos& pos,
             Node* leftArg, gc<String> name, Node* rightArg);
    ~CallNode();
    
    DECLARE_NODE(CallNode);
    
    gc<String> name()     const { return name_; }
    Node*   rightArg() const { return rightArg_; }
    Node*   leftArg()  const { return leftArg_; }
    
    virtual void trace(std::ostream& out) const;
    
  private:
    Node* leftArg_;
    gc<String> name_;
    Node* rightArg_;
  };
  
  // An if-then-else expression.
  class IfNode : public Node
  {
  public:
    IfNode(const SourcePos& pos, Node* condition,
           Node* thenArm, Node* elseArm);
    ~IfNode();
    
    DECLARE_NODE(IfNode);
    
    Node& condition() const { return *condition_; }
    Node& thenArm() const { return *thenArm_; }
    Node& elseArm() const { return *elseArm_; }
    
    virtual void trace(std::ostream& out) const;
    
  private:    
    Node* condition_;
    Node* thenArm_;
    Node* elseArm_;
  };
  
  // A named variable reference.
  class NameNode : public Node
  {
  public:
    NameNode(const SourcePos& pos, gc<String> name);
    ~NameNode();
    
    DECLARE_NODE(NameNode);
    
    gc<String> name() const { return name_; }
    
    virtual void trace(std::ostream& out) const;
    
  private:    
    gc<String> name_;
  };
  
  // A number literal.
  class NumberNode : public Node
  {
  public:
    NumberNode(const SourcePos& pos, double value);
    
    DECLARE_NODE(NumberNode);
    
    double value() const { return value_; }
    
    virtual void trace(std::ostream& out) const;

  private:
    double value_;
  };
  
  // A sequence of line- or semicolon-separated expressions.
  class SequenceNode : public Node
  {
  public:
    SequenceNode(const SourcePos& pos, const Array<Node*>& expressions);
    ~SequenceNode();
    
    DECLARE_NODE(SequenceNode);
    
    const Array<Node*>& expressions() const { return expressions_; }
    
    virtual void trace(std::ostream& out) const;
    
  private:    
    Array<Node*> expressions_;
  };
  
  // A string literal.
  class StringNode : public Node
  {
  public:
    StringNode(const SourcePos& pos, gc<String> value);
    
    DECLARE_NODE(StringNode);
    
    gc<String> value() const { return value_; }
    
    virtual void trace(std::ostream& out) const;
    
  private:    
    gc<String> value_;
  };
  
  // A 'var' or 'val' variable declaration.
  class VariableNode : public Node
  {
  public:
    VariableNode(const SourcePos& pos, bool isMutable,
                 Pattern* pattern, Node* value);
    ~VariableNode();
    
    DECLARE_NODE(VariableNode);
    
    bool isMutable() const { return isMutable_; }
    const Pattern* pattern() const { return pattern_; }
    Node* value() const { return value_; }
    
    virtual void trace(std::ostream& out) const;
    
  private:    
    bool isMutable_;
    Pattern* pattern_;
    Node* value_;
  };
  
  // Base class for all AST pattern node classes.
  class Pattern
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
    VariablePattern(gc<String> name);
    
    DECLARE_PATTERN(VariablePattern);
    
    virtual int countVariables() const { return 1; }
    
    gc<String> name() const { return name_; }
    
    virtual void trace(std::ostream& out) const;
    
  private:    
    gc<String> name_;
  };
}

