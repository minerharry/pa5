package miniJava.AbstractSyntaxTrees;

public interface TypeOrExpression {
    public enum TypeClassification{
        TYPE,
	    EXPRESSION,
	    AMBIGUOUS;
    }
    public TypeClassification getClassification();
    public TypeDenoter asType() throws InvalidCastError;
    public Expression asExpression() throws InvalidCastError;
}
