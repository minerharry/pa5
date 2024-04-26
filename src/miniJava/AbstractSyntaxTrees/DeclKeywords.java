package miniJava.AbstractSyntaxTrees;


import java.util.ArrayList;
import java.util.List;
import miniJava.AbstractSyntaxTrees.ClassMemberDecl.ClassType;
import miniJava.AbstractSyntaxTrees.MemberDecl.MemberType;
import miniJava.SyntacticAnalyzer.ModifierType;
import miniJava.SyntacticAnalyzer.Parser.ClassContext;
import miniJava.SyntacticAnalyzer.Parser.MemberScope;
import miniJava.SyntacticAnalyzer.Parser.SyntaxError;
import miniJava.SyntacticAnalyzer.ProtectionType;
import miniJava.SyntacticAnalyzer.SourcePosition;


public class DeclKeywords extends AST {

    public Protection protection;
    public ModifierList modifiers;
    public List<Annotation> annotations;

    public boolean isPrivate(){
        return this.protection != null && this.protection.type == ProtectionType.Private;
    }
    public boolean isStatic(){
        return this.modifiers.containsType(ModifierType.Static);
    }
    public boolean isAbstract() {
        return this.modifiers.containsType(ModifierType.Abstract);
    }

    public DeclKeywords(){
        super(null);
        protection = null;
        modifiers = new ModifierList();
        annotations = new ArrayList<Annotation>();
    }

    public DeclKeywords(Protection p, ModifierList mods, List<Annotation> anns, SourcePosition posn) {
        super(posn);
        protection = p;
        modifiers = mods;
        annotations = anns;
    }

    public boolean isEmpty(){
        return (protection == null) && (modifiers.size() == 0);
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitDeclKeywords(this, o);
    }

    public <R extends Declaration> R validate(ClassContext cctx, R decl) {
        MemberScope scope = cctx.outerscope;
        ClassType scopeType = cctx.ct;
        MemberType mt = (decl instanceof MemberDecl ? (decl.asMemberDecl()).getMemberType() : null); //NOTE: GLOBAL SCOPE CAN BE MEMBERS BECAUSE OF THIS!!! 
        ClassType ct = (mt == MemberType.CLASSLIKE ? (decl.asMemberDecl().asClassLike()).getClassType() : null);
        boolean isLocal = scope == MemberScope.LOCAL || scope == MemberScope.PARAMETER;
                
        //illegal modifiers
        for(Modifier m : modifiers){
            if (mt == MemberType.ENUM){
                //enum elements cannot have modifiers! can have annotations though
                throw new SyntaxError("Enum elements cannot have modifiers!", m.posn);
            }
            if (scope == MemberScope.TYPEPARAMETER){
                //type parameters cannot have modifiers! can have annotations though
                throw new SyntaxError("Type parameters cannot have modifiers!",m.posn);
            }
            switch (m.type){
                case Abstract:
                    if (isLocal){
                        throw new SyntaxError("Keyword 'abstract' not allowed on local declarations",m.posn);
                    } if (mt == MemberType.FIELD) {
                        throw new SyntaxError("Cannot have abstract fields",m.posn);
                    } else if (mt == MemberType.CONSTRUCTOR) {
                        throw new SyntaxError("Cannot have abstract constructors",m.posn);
                    } else if (ct == ClassType.ENUM){
                        throw new SyntaxError("Cannot have abstract enums",m.posn);
                    }
                    break;
                case Final:
                    switch (scope) {
                        case PARAMETER:
                            throw new SyntaxError("Keyword 'final' not allowed in parameter declarations",m.posn);
                        case CLASSLIKE:
                            if (mt != MemberType.FIELD){
                                throw new SyntaxError("Keyword 'final' not allowed for non-field members",m.posn);
                            }
                        case LOCAL:
                            break;
                        default:
                            throw new SyntaxError("Keyword 'final' not allowed here",m.posn);
                    }
                    break;
                case Default:
                    if (scopeType != ClassType.INTERFACE || mt != MemberType.METHOD){
                        throw new SyntaxError("Default keyword only allowed for interface methods",m.posn);
                    }
                    break;
                case Static:
                    switch (scope) {
                        case GLOBAL:
                            throw new SyntaxError("Keyword 'static' not allowed in global scope",m.posn);
                        case LOCAL:
                        case PARAMETER:
                            throw new SyntaxError("Cannot have static local variables",m.posn);
                        case CLASSLIKE:
                            break;
                    }
                    break;
            }
        }

        //no illegal modifiers; any required modifiers? (interface methods specifically);
        if (scopeType == ClassType.INTERFACE) {
            if (mt == MemberType.FIELD) {
                if (!modifiers.containsType(ModifierType.Static) || !modifiers.containsType(ModifierType.Final)){
                    throw new SyntaxError("Interface fields must be static and final",decl.posn);
                }
            }
        }

        //invalid/incorrect protection (no required protections; a default is always provided)
        if (protection != null) {
            if (mt == MemberType.ENUM){
                throw new SyntaxError("Enum elements cannot have protections!",protection.posn);
            }
            if (scope == MemberScope.TYPEPARAMETER){
                throw new SyntaxError("Type parameters cannot have protections!",protection.posn);
            }
            switch (scope){
                case LOCAL:
                case PARAMETER:
                    throw new SyntaxError("Protection keywords not allowed on local variables",protection.posn);
                case GLOBAL:
                    break;
                case CLASSLIKE:
                    if (scopeType == ClassType.INTERFACE && protection.type != ProtectionType.Public){
                        throw new SyntaxError("All interface members must be public",protection.posn);
                    }
            }
        }

        //Method body full/empty: Has to happen here b/c keywords
        if (mt == MemberType.METHOD){
            MethodDecl md = decl.asMemberDecl().asMethod();
            if (md.isEmpty()){ //
                if (scopeType != ClassType.INTERFACE) {
                    //does method annotation match method body?
                    if (!md.isAbstract()){ 
                        throw new SyntaxError("Concrete method " + md.repr() + " missing method body",md.posn);
                    }
                    //does method type match scope?
                    if (!cctx.isAbstract){
                        throw new SyntaxError("Abstract method " + md.repr() + " in non-abstract class body",md.posn);
                    }
                    //METHOD IS ABSTRACT: cannot be prviate!
                    if (md.isPrivate()){
                        throw new SyntaxError("Abstract method " + md.repr() + " cannot be private; must be protected or public",md.posn);
                    }


                } else {
                    if (md.keywords.modifiers.containsType(ModifierType.Default)){
                        throw new SyntaxError("Concrete (default) method " + md.repr() + " missing method body",md.posn);
                    }
                }
            } else { //method has body, cannot be abstract (regular class) or non-default (interface)
                if (scopeType != ClassType.INTERFACE){
                    if (md.isAbstract()) {
                        throw new SyntaxError("Abstract method " + md.repr() + " cannot have body, ';' expected",md.posn);
                    }

                    //don't need to check for class context; in non-interface classes, concrete methods are always allowed
                } else {
                    
                    //concrete method needs a default keyword
                    if (!md.keywords.modifiers.containsType(ModifierType.Default)){
                        throw new SyntaxError("Interface method " + md.repr() + " cannot have method body, ';' expected, or use default keyword to mark as concrete",md.posn);
                    }
                }
            }
        }

        //TODO: annotation validation? still have no idea how annotations work. For now just accept.

        return decl;

    }
    @Override
    public String repr() {
        return "Declaration keywords at position " + posn; //really don't need to list out the details
    }
    

    
}
