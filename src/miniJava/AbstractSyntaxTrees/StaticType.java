package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.AbstractSyntaxTrees.MemberDecl.MemberType;
import miniJava.ContextualAnalysis.TypeChecker.TypeError;
//So, this is kind of un-java. In type checking, the differences between static and instance types have gotten really, 
// really annoying.
//To fix this, I just added this internal system that matches the consistency of python. 
// Static types cannot be directly referenced in code, nor can variables be a static type; instead, static types
//are generated during type checking when doing stuff like A.B.C.x or Class.staticVar. Specifically, in type parsing
// A.staticVar, the typedenoter of A is a StaticType(A), which is somewhat analagous to java's Class<A>. 
//
public class StaticType extends TypeDenoter {
    public TypeDenoter baseType;

    public StaticType(TypeDenoter base) {
        super(TypeKind.STATIC, base.posn);
        baseType = base;
        //TODO Auto-generated constructor stub
    }

    public static StaticType fromClass(ClassMemberDecl cmdl){
        IdentifierType type = IdentifierType.makeClassType(cmdl);
        return new StaticType(type);
    }
    

    @Override
    public boolean isEqual(TypeDenoter other) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isEqual'");
    }

    @Override
    public String repr() {
        return "Static Type of " + baseType.repr();
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public MemberDecl getMember(Identifier name) {
        return findMember(name, true);
    }

    @Override
    protected MemberDecl findMember(Identifier name, boolean allow_type) {
        //I think this is fine? I think this would only really be called in the case of like a static static type or 
        //a static type array Static<Class>[], neither of which should happen. Realistically this method should never be
        //called with allow_type false.
        
        if (allow_type == false){
            System.out.println(name.repr());
            throw new UnsupportedOperationException("I wonder what caused this?");
        }
        MemberDecl res = baseType.findMember(name, true);
        if (res instanceof OverloadedMethod){
            res = ((OverloadedMethod) res).staticMethods();
        }
        if (res != null && !res.asDeclaration().isStatic()){
            throw new TypeError("Cannot statically access non-static member " + name.repr() + " of class " + baseType.repr());
        }
        return res;
    }

    public StaticType getSuperStaticType(){
        if (typeDeclaration == null){
            throw new UnsupportedOperationException();
        }
        if (typeDeclaration.superclass == null){
            return null;
        } else {
            return StaticType.fromClass(typeDeclaration);
        }
    }
    
}
