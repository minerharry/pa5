/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class IdRef extends Expression {
	
	public IdRef(Identifier id, SourcePosition posn){
		super(posn);
		this.id = id;
	}

	public IdRef(Identifier id){ //if no position provided, copy the base identifier's
		super(id.posn);
		this.id = id;
	}
		
	public <A,R> R visit(Visitor<A,R> v, A o) {
		return v.visitIdRef(this, o);
	}

	public Identifier id;

	@Override
	public boolean isAssignable() {
		return true;
	}

	@Override
	public boolean isReferrable() {
		return true;
	}

	@Override
	public boolean isCallable() {
		return true;
	}

	@Override
	public CallExpr toMethodCall(ExprList el){
		return new CallExpr(id, el, posn);
	}

	@Override
	public String repr() {
		return "Identifier Expression/Reference " + id.spelling + " at position " + posn;
	}

	//DECORATION: Populated during type checking
	public ClassMemberDecl context;
}
