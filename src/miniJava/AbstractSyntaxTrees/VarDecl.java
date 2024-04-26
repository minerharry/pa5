/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class VarDecl extends Declaration {
	
	public VarDecl(DeclKeywords keywords, TypeDenoter t, Identifier name, Expression il, SourcePosition posn) {
		super(keywords, t, name, posn);
		initializer = il;
	}

	public Expression initializer;
}
