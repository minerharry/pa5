package miniJava.SyntacticAnalyzer;

import miniJava.Compiler;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class Token {

	public static boolean fullString = true;

	public static final HashSet<Character> opstarts = new HashSet<>(Arrays.asList('+','/','*','-','%','|','&','~','!', '=', '>','<')); //both unary and binary operators
	public static final HashSet<Character> dualOps = new HashSet<>(Arrays.asList('+','-')); //both unary and binary operators
	public static final HashSet<Character> assignOps = new HashSet<>(Arrays.asList('+','/','*','-','%','|','&')); //both unary and binary operators



	private static class LoadMap<K,V> extends HashMap<K,V> {
		public LoadMap(LoadEntry<K,V>... entries){
			for (LoadEntry<K,V> entry : entries){
				if (entry.isValid()){
					this.put(entry.getKey(),entry.getValue());
				}
			}
		}
	}

	private static class LoadEntry<K,V> implements Entry<K,V> {
		K key;
		V value;
		boolean valid;

		public LoadEntry(K key, V value){
			this.key = key;
			this.value = value;
			this.valid = true;
		}

		public LoadEntry(K key, V value, boolean valid){
			this.key = key;
			this.value = value;
			this.valid = valid;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			return this.value = value;
		}

		public boolean isValid(){
			return valid;
		}

	}

	@SuppressWarnings("unchecked")
	public static final HashMap<Character,TokenType> punctuation = new LoadMap<Character,TokenType>(
		new LoadEntry('.',TokenType.dot),
		new LoadEntry(',',TokenType.comma),
		new LoadEntry(';',TokenType.semicolon),
		new LoadEntry('{',TokenType.lcurly),
		new LoadEntry('}',TokenType.rcurly),
		new LoadEntry('(',TokenType.lparen),
		new LoadEntry(')',TokenType.rparen),
		new LoadEntry('[',TokenType.lsquare),
		new LoadEntry(']',TokenType.rsquare),
		new LoadEntry('?',TokenType.question,!Compiler.IS_MINI),
		new LoadEntry('@',TokenType.atsign,!Compiler.IS_MINI),
		new LoadEntry(':',TokenType.colon,!Compiler.IS_MINI));

	@SuppressWarnings("unchecked")
	public static final HashMap<String,TokenType> keywords = new LoadMap<String,TokenType>(
		new LoadEntry("class", TokenType.classKeyword),
		new LoadEntry("if",TokenType.ifKeyword),
		new LoadEntry("else",TokenType.elseKeyword),
		new LoadEntry("for", TokenType.forKeyword),
		new LoadEntry("this", TokenType.thisKeyword),
		new LoadEntry("while", TokenType.whileKeyword),
		new LoadEntry("void",TokenType.voidKeyword),
		new LoadEntry("new",TokenType.newKeyword),
		new LoadEntry("return",TokenType.returnKeyword),
		new LoadEntry("true",TokenType.boolLiteral),
		new LoadEntry("false",TokenType.boolLiteral),
		new LoadEntry<>("null",TokenType.nullLiteral),
			new LoadEntry("package",TokenType.packageKeyword,!Compiler.IS_MINI),
			new LoadEntry("import",TokenType.importKeyword,!Compiler.IS_MINI),
			new LoadEntry("throws",TokenType.throwsKeyword,!Compiler.IS_MINI),
			new LoadEntry("throw",TokenType.throwKeyword,!Compiler.IS_MINI),
			new LoadEntry("implements",TokenType.implementsKeyword,!Compiler.IS_MINI),
			new LoadEntry("extends",TokenType.extendsKeyword,!Compiler.IS_MINI),
			new LoadEntry("try",TokenType.tryKeyword,!Compiler.IS_MINI),
			new LoadEntry("catch",TokenType.catchKeyword,!Compiler.IS_MINI),
			new LoadEntry("finally",TokenType.finallyKeyword,!Compiler.IS_MINI),
			new LoadEntry("break",TokenType.breakKeyword,!Compiler.IS_MINI),
			new LoadEntry("continue",TokenType.continueKeyword,!Compiler.IS_MINI),
			new LoadEntry("do",TokenType.doKeyword,!Compiler.IS_MINI),
			new LoadEntry("switch",TokenType.switchKeyword,!Compiler.IS_MINI),
			new LoadEntry("case",TokenType.caseKeyword,!Compiler.IS_MINI),
			new LoadEntry("instanceof",TokenType.instanceOfKeyword,!Compiler.IS_MINI),
			new LoadEntry("interface",TokenType.interfaceKeyword,!Compiler.IS_MINI),
			new LoadEntry("enum",TokenType.enumKeyword,!Compiler.IS_MINI));

	@SuppressWarnings("unchecked")
	public static final HashMap<String,TokenType> primitives = new LoadMap<String,TokenType>(
		new LoadEntry("int",TokenType.intPrimitive),
		new LoadEntry("boolean",TokenType.boolPrimitive),
			new LoadEntry("float",TokenType.floatPrimitive,!Compiler.IS_MINI),
			new LoadEntry("double",TokenType.doublePrimitive,!Compiler.IS_MINI),
			new LoadEntry("char",TokenType.charPrimitive,!Compiler.IS_MINI)
		);

	public static final HashSet<TokenType> literals = new HashSet<TokenType>(Arrays.asList(
		TokenType.boolLiteral,
		TokenType.intLiteral,
		TokenType.nullLiteral,
			TokenType.charLiteral,
			TokenType.floatLiteral,
			TokenType.stringLiteral));


	private TokenType _type;
	private String _text;
	private SourcePosition _pos;
	
	public Token(TokenType type, String text, SourcePosition position) {
		_type = type;
		_text = text;
		_pos = position;
	}
	
	public TokenType getTokenType() {
		return _type;
	}
	
	public String getTokenText() {
		return _text;
	}

	public SourcePosition getTokenPosition(){
		// if (hasPos)
		return _pos;
		// return null;
	}

	public String toString(){
		if (fullString){
			return "Token of type {" + _type + "}: " + _text;// + " at " + _pos.shortString();
		}
		return getTokenText();
	}
}
