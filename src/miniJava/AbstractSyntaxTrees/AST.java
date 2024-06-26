/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class AST {

  public AST (SourcePosition posn) {
    this.posn = posn;
  }
  
  public String toString() {
      String fullClassName = this.getClass().getName();
      String cn = fullClassName.substring(1 + fullClassName.lastIndexOf('.'));
      if (ASTDisplay.showPosition && posn != null)
    	  cn = cn + " " + posn.toString();
      return cn;
  }
  
  public abstract String repr(); //tired of not having good string. Should print out this node's info only, **inclusing source position**

  public abstract <A,R> R visit(Visitor<A,R> v, A arg);

  public SourcePosition posn;
}
