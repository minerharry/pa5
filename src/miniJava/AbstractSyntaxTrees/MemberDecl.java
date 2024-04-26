package miniJava.AbstractSyntaxTrees;

import java.util.EnumSet;

public interface MemberDecl{

	public enum MemberType { //DECLARATION REASONS ONLY; e.g. you want to know what objects the keywords refer to
		FIELD,
		METHOD,
		CONSTRUCTOR,
		ENUM,
		CLASSLIKE; //can be further specified by a classtype

		public static EnumSet<MemberType> allOpts = EnumSet.allOf(MemberType.class);
	}

	public MemberType getMemberType();

	public Declaration asDeclaration();

	public ClassMemberDecl enclosingDecl();
	public void setEnclosingDecl(ClassMemberDecl cmd);

    public default ClassMemberDecl asClassLike(){
		throw new InvalidCastError("Cannot cast " + this + " to ClassLike");
	}

    public default ConstructorDecl asConstuctor(){
		throw new InvalidCastError("Cannot cast " + this + " to Constructor");
	}

    public default MethodDecl asMethod(){
		throw new InvalidCastError("Cannot cast " + this + " to Method");
	}

    public default FieldDecl asField(){
		throw new InvalidCastError("Cannot cast " + this + " to Field");
	}

	public default EnumElement asEnum(){
		throw new InvalidCastError("Cannot cast " + this + " to Enum");
	}

	public default GenericVar asGeneric(){
		throw new InvalidCastError("Cannot cast " + this + "to Generic");
	}

	public default int getStaticOffset(){
		ClassMemberDecl cmd = this.enclosingDecl();
		int totalOffset = this.asDeclaration().basePointerOffset;
		if (cmd == null){
			//outermost class! use static offset
			return totalOffset;
		}
		ClassMemberDecl currDecl = cmd;
		do { //not an outermost class! find outermost classes
			totalOffset += currDecl.basePointerOffset;
			currDecl = currDecl.enclosingDecl();
		} while (cmd.enclosingDecl() != null);
		return totalOffset;
	}
}
