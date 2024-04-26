/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */

package miniJava.AbstractSyntaxTrees;

import miniJava.Compiler;
import miniJava.SyntacticAnalyzer.SourcePosition;
/* Array Type */
public class ArrayType extends TypeDenoter {

	    public ArrayType(TypeDenoter eltType, SourcePosition posn){
	        super(TypeKind.ARRAY, posn);
			if (eltType instanceof ArrayType && Compiler.IS_MINI){
				throw new ASTError("Multi-dimensional array types are not allowed in miniJava. This is a parsing error.");
			}
	        this.eltType = eltType;
	    }
	        
	    public <A,R> R visit(Visitor<A,R> v, A o) {
	        return v.visitArrayType(this, o);
	    }

	    public TypeDenoter eltType;

		public boolean isEqual(ArrayType other){
			return eltType.equals(other.eltType);
		}

		@Override
		public boolean isEqual(TypeDenoter other) {
			if (other instanceof ArrayType){
				return ((ArrayType)other).isEqual(this); //why doesn't this work??
			} 
			return false;
		}

		@Override
		public String repr() {
			return "Array Type {" + eltType.repr() + "} []";
		}

		@Override
		protected MemberDecl findMember(Identifier name, boolean allow_type) {
			//TODO: How to actually implement .length in codegen?
			if (name.equals(Identifier.makeDummy("length")) && !allow_type){
				return new FieldDecl(new DeclKeywords(), new PrimitiveType(TypeKind.INT, posn), Identifier.makeDummy("length"), null, posn);
			}
			return null;
		}

		@Override
		public boolean isArrayType(){
			return true;
		}

		@Override
		public ArrayType asArrayType(){
			return this;
		}
	}

