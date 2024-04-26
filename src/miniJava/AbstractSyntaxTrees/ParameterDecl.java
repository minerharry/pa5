/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ParameterDecl extends LocalDecl {
	
	public ParameterDecl(DeclKeywords keywords, TypeDenoter t, Identifier name, Expression initializer, SourcePosition posn){
		super(keywords, t, name, initializer, posn);
	}

	public boolean isDefault(){
		return initializer != null;
	}
	
	public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitParameterDecl(this, o);
    }

}

