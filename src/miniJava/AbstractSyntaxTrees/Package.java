/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.CodeGeneration.x64.RByte;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class Package extends AST{

  public Package(FileHeader h, List<ClassMemberDecl> cdl, SourcePosition posn){
    super(posn);
    classes = cdl;
    header = h;
  }

  public Package(List<ClassMemberDecl> cdl, SourcePosition posn) {
    super(posn);
    classes = cdl;
    header = null;
  }
    
  public <A,R> R visit(Visitor<A,R> v, A o) {
      return v.visitPackage(this, o);
  }

  public List<ClassMemberDecl> classes;
  public FileHeader header;

  public String getFullName(){
    return header.getFullName();
  }

  //DECORATION: populated during type checking I think
  public MethodDecl mainMethod;

  @Override
  public String repr() {
    return "Package at position " + posn;
  }
}
