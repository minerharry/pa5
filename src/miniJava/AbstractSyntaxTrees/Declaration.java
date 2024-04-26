/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.AbstractSyntaxTrees.MemberDecl.MemberType;
import miniJava.SyntacticAnalyzer.ModifierType;
import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Declaration extends AST {

	public Declaration(DeclKeywords keywords, TypeDenoter t, Identifier name, SourcePosition posn){
		super(posn);
		this.keywords = keywords;
		this.type = t;
		this.name = name;
	}

	public MemberDecl asMemberDecl(){
		throw new InvalidCastError("Cannot cast " + this + " to MemberDecl");
	}

	public boolean isMember(){
		try{
			this.asMemberDecl();
			return true;
		} catch (InvalidCastError e){
			return false;
		}
	}

	public boolean isTypeMember(){
		return this.isMember() && this.asMemberDecl().getMemberType() == MemberType.CLASSLIKE;
	}

	public boolean isVarMember() {
        return this instanceof VarDecl; //I think this just works??
    }

	public boolean isAbstract(){
		return keywords.isAbstract();
	}

    public boolean isStatic() {
        return keywords.isStatic();
    }

	public boolean isPrivate() {
		//todo: replace with isvisible
		return keywords.isPrivate();
	}

	public DeclKeywords keywords;
	public TypeDenoter type;
	public Identifier name;
	
	//CODE GENERATION DECORATION
	/**
	 * offset from relevant "base pointer" to this location in memory. If this is a stack variable, the var is stored at [RBP-basePointerOffset].
	 * If this is a heap declaration (class static variables), the **pointer to this class* class is stored at [heap pointer - basePointerOffset].
	 * if this is a member of an object on the heap, the var is stored at [Object pointer - basePointerOffset].
	 */
	public int basePointerOffset;
	
}
