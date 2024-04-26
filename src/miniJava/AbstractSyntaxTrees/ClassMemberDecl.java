package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.ContextualAnalysis.TypeResult;
import miniJava.ContextualAnalysis.ScopedIdentifier.IdentificationError;
import miniJava.ContextualAnalysis.TypeChecker.TypeError;
import miniJava.SyntacticAnalyzer.ModifierType;
import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class ClassMemberDecl extends Declaration implements MemberDecl {
    
	public enum ClassType { //SCOPE REASONS ONLY; e.g., you want to know what classtype a member is *within*
    	CLASS,
    	INTERFACE,
    	ENUM,
    	ANNOTATION;
    }

	public List<GenericVar> generics;
	public List<FieldDecl> fields;
	public List<MethodDecl> methods;
	public List<ConstructorDecl> constructors;
	public List<ClassMemberDecl> classmembers;
	public TypeDenoter superclass;
	public List<TypeDenoter> superinterfaces;

	public ClassMemberDecl(DeclKeywords keywords, 
			Identifier cn, 
			List<GenericVar> genericVars,
			List<FieldDecl> fdl, 
			List<MethodDecl> mdl, 
			List<ConstructorDecl> cdl,
			List<ClassMemberDecl> cmdl, TypeDenoter superclass,
			List<TypeDenoter> interfaces, 
			SourcePosition posn) {
		super(keywords, null, cn, posn);
		generics = genericVars;
		fields = fdl;
		methods = mdl;
		constructors = cdl;
		classmembers = cmdl;
		this.superclass = superclass;
		this.superinterfaces = interfaces;
		this.type = StaticType.fromClass(this);
	}

	public boolean isAbstract(){
		return keywords.modifiers.containsType(ModifierType.Abstract);
	}

    public abstract ClassType getClassType();

	@Override
	public Declaration asDeclaration() {
		return this;
	}

	@Override
	public MemberType getMemberType() {
		return MemberType.CLASSLIKE;
	}

	@Override
	public ClassMemberDecl asClassLike() {
		return this;
	}

	@Override
    public MemberDecl asMemberDecl(){
		return this;
	}


	public MemberDecl findMember(String name, boolean allow_type){

		if (allow_type){
			for (ClassMemberDecl cmdl : classmembers){
				if (cmdl.name.spelling.equals(name)){
					return cmdl;
				}
			}
		}

		for (FieldDecl fd : fields){
			if (fd.name.spelling.equals(name)){
				return fd;
			}
		}

		for (MethodDecl md : methods){
			if (md.name.spelling.equals(name)){
				return md;
			}
		}

		for (ConstructorDecl cd : constructors){
			if (cd.name.spelling.equals(name)){
				return cd;
			}
		}

		return null;
	}

	public MemberDecl findMemberThrow(Identifier name, boolean allow_type){
		MemberDecl res = findMember(name.spelling,allow_type);
		if (res == null){
			throw new TypeError("Could not find " + (allow_type ? "type " : "") + "member " + name.repr() + " in " + this.repr());
		}
		return res;
	}

	public boolean equals(ClassMemberDecl other){
		//TODO: this is dumb and incomplete
		return other.name.equals(this.name);
	}

    public MemberDecl findStaticMemberThrow(Identifier name, boolean allow_type) {
        MemberDecl res = findMemberThrow(name,allow_type);
		if (!res.asDeclaration().isStatic()){
			throw new TypeError("Cannot access non-static member " + name.repr() + " of " + this.repr() + " from static context");
		}
		return res;
    }

	@Override
	public String repr() {
		return this.getClassType().name() + " Declaration: " + this.name.spelling + " at position " + this.posn;
	}


	private ClassMemberDecl enclosing;
	@Override
	public ClassMemberDecl enclosingDecl() {
		return enclosing;
	}

	@Override
	public void setEnclosingDecl(ClassMemberDecl cmd) {
		enclosing = cmd;
	}

	public int staticSize;
	public int instanceSize;
}
