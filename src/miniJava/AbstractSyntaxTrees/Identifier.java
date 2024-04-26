/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

public class Identifier extends Terminal {

  public Declaration refDecl;

  public Identifier (Token t) {
    super (t);
  }

  public <A,R> R visit(Visitor<A,R> v, A o) {
      return v.visitIdentifier(this, o);
  }

  public String getSpelling(){
    return spelling;
  }
  @Override
  public Identifier asIdentifier() {
    return this;
  }

  public boolean equals(Identifier other){
    return other.spelling.equals(this.spelling);
  }

  public String repr() {
      return "Identifier \"" + spelling + "\" at position " + this.posn;
  }

public static Identifier makeDummy(String name) {
    return new Identifier(new Token(TokenType.id, name, null));
}

}
