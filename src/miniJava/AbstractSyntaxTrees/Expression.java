/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.ContextualAnalysis.TypeResult;
import  miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Expression extends AST implements TypeOrExpression {

  //DECORATED AST: TYPE CHECKING
  public TypeDenoter returnType;
  
  public TypeDenoter setType(TypeDenoter type){
    this.returnType = type; 
    return type; //returns itself for oneliner purposes
  }

  public TypeResult setType(TypeResult type){
    this.returnType = type.getType(); 
    return type; //returns itself for oneliner purposes
  }

  public Expression(SourcePosition posn) {
    super (posn);
  }

  public TypeClassification getClassification(){
    return TypeClassification.EXPRESSION;
  }

  public abstract boolean isAssignable();
  public boolean isStateable(){
    return false;
  }
  public boolean isCallable() {
    return false;
  }
  public Expression asExpression(){
    return this;
  }
  public TypeDenoter asType()  throws InvalidCastError{
    throw new ASTError("Cannot convert Expression to TypeDenoter");
  }

  public CallExpr toMethodCall(ExprList el) {
    if (!isCallable()){
      throw new ASTError("Cannot convert type " + this.getClass() + " to method call");
    } else {
      throw new UnsupportedOperationException("Expression type " + this.getClass() + " is callable, but has not implemented toMethodCall method!");
    }
  }

  public boolean isReferrable() {
    return false;
  }

  
}
