package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.io.InputStream;

import miniJava.Compiler;
import miniJava.CompilerError;
import miniJava.ErrorReporter;

public class Scanner {
	private InputStream _in;
	private ErrorReporter _errors;
	private String _filename;
	private StringBuilder _currentText;
	private char _currentChar;
	private int _startLine,_startCol;
	private int _currentLine = 1;
	private int _currentCol = 0;

	public Scanner( InputStream in, ErrorReporter errors, String filename ) {
		this._in = in;
		this._errors = errors;
		this._filename = filename;
		clearText();
		
		nextChar();
	}

	public char[] escapeChars = {'t','b','n','r','f','\'','\"','\\'};
	
	public Token scan() {
		// TODO: This function should check the current char to determine what the token could be.
		
		// TODO: Consider what happens if the current char is whitespace or comment
		Token maybe = whitespaceOrOp();
		if (maybe != null){
			return maybe;
		}

		if (Token.opstarts.contains(_currentChar)){
			char c = _currentChar;
			takeIt(); //we know this will be a token, accept
			//do operator parsing
			if (c == '='){
				//two options: assignment and ==
				if (_currentChar == '='){
					takeIt();
					return makeToken(TokenType.compOp);
				} else {
					return makeToken(TokenType.assignment);
				}
			}

			//+=, -=, etc
			if (Token.assignOps.contains(c) && _currentChar == '='){
				takeIt();
				return makeToken(TokenType.assignOp);
			}

			//plus and minus 
			if (Token.dualOps.contains(c)){
				if (_currentChar == c){  //++, --
					takeIt();
					return makeToken(TokenType.incOp); //special op because can go before or after
				} else {
					return makeToken(TokenType.genOp);
				}
			}
			
			switch (c) {
				case '!':
					if (_currentChar == '='){
						takeIt();
						return makeToken(TokenType.compOp);
					} else {
						return makeToken(TokenType.unOp);
					}
				case '>':
				case '<':
					if (_currentChar == '='){
						takeIt();
						return makeToken(TokenType.compOp);
					} else if (_currentChar == c){
						takeIt();
						if (_currentChar == c && _currentChar == '>') { //triple shift
							takeIt();
						}
						if (_currentChar == '='){
							takeIt();
							return makeToken(TokenType.assignOp);
						}
						return makeToken(TokenType.binOp);
					}
					if (c == '>'){
						return makeToken(TokenType.rchevron);
					} else {
						return makeToken(TokenType.lchevron);
					}
					
				case '|':
				case '&':
					if (_currentChar == c){
						takeIt();
					}
					return makeToken(TokenType.binOp);
				default: // *,/
					return makeToken(TokenType.binOp);
			}
		}
		
		// identifiers and keywords
		
		if (Character.isJavaIdentifierStart(_currentChar)){ //how convenient, there's already a function for it
			if (!Compiler.IS_MINI || (Character.isLetter(_currentChar) || Character.getType(_currentChar) == Character.LETTER_NUMBER)){
				//start reading until no longer alphanumeric
				takeIt();
				while (Character.isJavaIdentifierPart(_currentChar)){ takeIt(); }
				String text = _currentText.toString();
				if (Token.keywords.containsKey(text)){
					return makeToken(Token.keywords.get(text));
				} else if (Token.primitives.containsKey(text)) {
					return makeToken(Token.primitives.get(text));
				} else if (ProtectionType.isProtection(text)) {
					return makeToken(TokenType.protection);
				} else if (ModifierType.isModifier(text)) {
					return makeToken(TokenType.modifier);
				} else {
					return makeToken(TokenType.id);
				}
			}
		}

		//literal parsing

		//numeric literal
		if (Character.isDigit(_currentChar)){
			while (Character.isDigit(_currentChar)){
				takeIt();
			}
			if (!Character.isAlphabetic(_currentChar) && !(_currentChar == '_')){
				return makeToken(TokenType.intLiteral);
			}
			else if (_currentChar != '.'){
				this._errors.reportError(new CompilerError("Invalid character while parsing number: " + _currentChar + " at pos " + getScannerPosition()));
			}
			while (Character.isDigit(_currentChar)){
				takeIt();
			}
			//even if there's no characters it's still a float
			return makeToken(TokenType.floatLiteral);
		}

		//string literal (simple escape)
		if (_currentChar == '"'){
			takeIt(); //take startquote
			while (_currentChar != '"') {
				if (_currentChar == '\\'){ //beginning of escape sequence
					takeIt();
					boolean taken = false;
					for (char i : escapeChars){
						if (_currentChar == i){
							takeIt();
							taken = true;
							break;
						}
					}
					if (!taken){
						this._errors.reportError(new CompilerError("Invalid String literal escape sequence: \\" + _currentChar));
						takeIt();
					}
				} else {
					takeIt();
				}
			}
			takeIt(); //take endquote
			return makeToken(TokenType.stringLiteral);
		}

		//char literal (no escape sequences)
		if (_currentChar == '\''){
			takeIt(); //starting '
			if (_currentChar == '\''){
				this._errors.reportError(new CompilerError("Invalid Character literal: cannot be empty"));
				takeIt();
				return makeToken(TokenType.charLiteral);
			}
			if (_currentChar == '\\'){ //inner character escape sequence
				takeIt();
				boolean taken = false;
				for (char i : escapeChars){
					if (_currentChar == i){
						takeIt();
						taken = true;
						break;
					}
				}
				if (!taken){
					this._errors.reportError(new CompilerError("Invalid Character literal escape sequence: \\" + _currentChar));
					takeIt();
				}
			} else {
				takeIt(); //inner char
			}
			if (_currentChar != '\''){
				while (_currentChar != '\''){
					if (_currentChar == '\n'){
						this._errors.reportError(new CompilerError("Unterminated Character literal"));
						return makeToken(TokenType.charLiteral);
					} else {
						takeIt();
					}
				}
				takeIt(); //final '
				this._errors.reportError(new CompilerError("Invalid Character literal: too long must be no longer than one character"));
				return makeToken(TokenType.charLiteral);
			} else {
				takeIt(); //final '
				return makeToken(TokenType.charLiteral);
			}
		}

		//punctuation
		if (_currentChar == '.' && !Compiler.IS_MINI){ //check for ellipsis for variadic type args
			takeIt();
			if (_currentChar != '.'){
				return makeToken(TokenType.dot);
			}
			takeIt();
			if (_currentChar != '.'){
				this._errors.reportError(new CompilerError("Invalid punctuation: .."));
				return makeToken(TokenType.EOT); //mom I'm scared
			}
			takeIt();
			return makeToken(TokenType.ellipsis);

		} else if (Token.punctuation.containsKey(_currentChar)){
			TokenType token = Token.punctuation.get(_currentChar);
			takeIt();
			return makeToken(token);
		}

		// TODO: Determine what the token is. For example, if it is a number
		//  keep calling takeIt() until _currentChar is not a number. Then
		//  create the token via makeToken(TokenType.IntegerLiteral) and return it.
		
		
		// TODO: What happens if there are no more tokens?
		if (_currentChar == 0){
			return makeToken(TokenType.EOT);
		}

		//REACHED HERE: unknown token!
		_errors.reportError(new CompilerError("Invalid character " + _currentChar + " encountered at pos " + getScannerPosition() + " while scanning"));
		skipIt();
		return scan();
	}

	private Token whitespaceOrOp(){
		while (true) {
			skipWhitespace();
			if (_currentChar == '/') { //three options: start of comment, /= assignment operator, or / binary operator
				takeIt(); //we have to take in order to advance because no lookahead
				switch (_currentChar) {
					case '=': //assignment op /=
						takeIt();
						return makeToken(TokenType.assignOp);
					case '/': //single-line comment, accept until newline
						skipIt();
						while (_currentChar != '\n' && _currentChar != 0){
							skipIt();
						}
						skipIt();
						clearText(); //remove the '/' in the current char
						continue;
					case '*': //multi-line comment, accept until */
						skipIt(); // /*/ is not a complete multiline
						char last = 0;
						while (!(last == '*' && _currentChar == '/')){
							if (_currentChar == 0){
								_errors.reportError(new CompilerError("Unterminated multiline comment"));
								return makeToken(TokenType.EOT);
							}
							last = _currentChar;
							skipIt();
						}
						skipIt();
						clearText(); //remove the '/' in the current char
						continue;
					default:
						// valid binary operator
						return makeToken(TokenType.binOp);
				}
			} else { //no more reason to loop, next character *must* return a token of some sort
				break;
			}
		}
		return null;
	}

	private void skipWhitespace(){
		while (Character.isWhitespace(_currentChar)){
			skipIt();
		}
	}
	
	private void takeIt() {
		_currentText.append(_currentChar);
		nextChar();
	}
	
	private void skipIt() {
		nextChar();
		if (_currentText.length() == 0){ //if we haven't started grabbing tokens, move the start
			_startCol = _currentCol;
			_startLine = _currentLine;
		}
	}
	
	private void nextChar() {
		try {
			int c = _in.read();
			_currentCol += 1;
			if (c == '\n'){
				_currentLine += 1;
				_currentCol = 0;
			}
			
			// TODO: What happens if c == -1?
			if (c == -1) { //EOF
				c = 0;
			}
			_currentChar = Character.toChars(c)[0]; //hacky but it works to avoid cast

			
			// TODO: What happens if c is not a regular ASCII character?
			
		} catch( IOException e ) {
			// TODO: Report an error here
		}
	}

	private void clearText(){
		_currentText = new StringBuilder();
		_startCol = _currentCol;
		_startLine = _currentLine;
	}
	
	public SourcePosition getScannerPosition() {
		return new SourcePosition(_filename,_startLine,_startCol);
	}

	private Token makeToken( TokenType toktype ) {
		// TODO: return a new Token with the appropriate type and text
		//  contained in 
		Token t = new Token(toktype,_currentText.toString(),getScannerPosition());
		// System.err.println("making new token " + t);
		clearText();
		return t;
	}
}
