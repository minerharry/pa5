/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
/*Represents a classname*/
public class IdentifierType extends TypeDenoter
{
    public IdentifierType(Identifier cn){
        super(TypeKind.CLASS, cn.posn);
        className = cn;
    }

    public static IdentifierType makeClassType(ClassMemberDecl cmdl){
        IdentifierType type = new IdentifierType(cmdl.name);
        type.typeDeclaration = cmdl;
        return type;
    }
            
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitIdentifierType(this, o);
    }

    public Identifier className;

    @Override
    public Identifier asIdentifier() {
        return this.className;
    }

    @Override
    public boolean isIdentifier() {
        return true;
    }

    public boolean isEqual(IdentifierType other){
        return other.className.equals(this.className);
    }

    @Override
    public boolean isEqual(TypeDenoter o) {
        if (o instanceof QualType){
            return o.isEqual(this);
        } else if (o instanceof IdentifierType){
            return ((IdentifierType)o).isEqual(this); //idk why you need the cast here??
        } else {
            return false;
        }
    }

    @Override
    public String repr() {
        return "Identifier Type " + this.className.repr();
    }

    @Override
    protected MemberDecl findMember(Identifier name, boolean allow_type){
        if (this.typeDeclaration == null){
            //this is an error in the implementation of typechecking, not in the code itself
            throw new UnsupportedOperationException("Cannot get declaration of type " + repr());
        } else {
            return this.typeDeclaration.findMember(name.spelling, allow_type);
        }
    }

}
