/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

abstract public class Terminal extends AST {

  public Terminal (Token t) {
	super(t.getTokenPosition());
    spelling = t.getTokenText();
    kind = t.getTokenType();
  }

  public static Terminal fromToken(Token t){
    return fromToken(t,false);
  }

  public static Terminal fromToken(Token t,boolean allow_identifier){
    switch (t.getTokenType()) {
      case intLiteral:
        return new IntLiteral(t);
      case boolLiteral:
        return new BooleanLiteral(t);
      case stringLiteral:
        return new StringLiteral(t);
      case floatLiteral:
        return new FloatLiteral(t);
      case charLiteral:
        return new CharLiteral(t);
      case nullLiteral:
        return new NullLiteral(t);
      case id:
        if (allow_identifier){
          return new Identifier(t);
        }
      default:
        throw new ASTError("Token " + t + " is not a literal token");
    }
  }

  public TokenType kind;
  public String spelling;
  public Identifier asIdentifier() {
      // TODO Auto-generated method stub
      throw new InvalidCastError("Cannot cast terminal " + this + " to identifier");
  }
}
