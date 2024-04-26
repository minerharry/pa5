/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.SyntacticAnalyzer.ModifierType;
import  miniJava.SyntacticAnalyzer.SourcePosition;

public class ClassDecl extends ClassMemberDecl {

  public ClassDecl(DeclKeywords keywords, 
        Identifier cn, 
        List<GenericVar> genericVars,
        List<FieldDecl> fdl, 
        List<MethodDecl> mdl, 
        List<ConstructorDecl> cdl,
        List<ClassMemberDecl> cmdl, TypeDenoter superclass,
        List<TypeDenoter> interfaces, 
        SourcePosition posn) {
    super(keywords, cn, genericVars, fdl, mdl, cdl, cmdl, superclass, interfaces, posn);
  }

  public <A,R> R visit(Visitor<A, R> v, A o) {
      return v.visitClassDecl(this, o);
  }

  @Override
  public ClassType getClassType() {
    return ClassType.CLASS;
  }

}
