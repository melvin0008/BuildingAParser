/* 		OBJECT-ORIENTED RECOGNIZER FOR SIMPLE EXPRESSIONS
stmnt ->
assign  ->  id '=' expr ';'
expr    -> term   (+ | -) expr | term
term    -> factor (* | /) term | factor
factor  -> int_lit | id |'(' expr ')'
*/

import java.lang.System;
import java.util.*;

public class Parser {
	public static void main(String[] args) {
		System.out.println("Enter an expression, end with \"end\"!\n");
//		while(Lexer.lex()!=21)
//		{
//			Lexer.lex();
//			new Decls();
//			Lexer.lex();
//			new Assign();
//			Lexer.lex();
//			new Assign();
//			Lexer.lex();
//			new Assign();
//		}
		new Program();
		Code.gen(Code.end());
		Code.output();
		
	}
}

class Program{
	Decls d;
//	Stmts s;

	public Program() {
		Lexer.lex();
		d = new Decls();
		while(Lexer.lex()!=21)
			new Stmnt();
	}
}

class Decls{

	public Decls(){
		if(Lexer.nextToken==Token.KEY_INT){
			Lexer.lex();
			if(Lexer.nextToken==Token.ID){
				new Factor();
				while(Lexer.nextToken==Token.COMMA){
					Lexer.lex();
					new Factor();
				}
			}
		}
		
	}
}

class Stmnt{
	Assign a;
	public Stmnt(){
		a= new Assign();
	}
}

class Assign{ //assign  ->  id '=' expr ';'
	char id;
	Expr e;
	Factor f;
	public Assign(){
		if(Lexer.nextToken==Token.ID){
			f= new Factor();
			if(Lexer.nextToken == Token.ASSIGN_OP){
				Lexer.lex();
				e = new Expr();
			}
		}
	}
}

class Rexpr{ //rexpr -> expr ('<'|'>'|'=='|'!=') expr
	
	
}

class Expr   { // expr -> term (+ | -) expr | term
	Term t;
	Expr e;
	char op;

	public Expr() {
		t = new Term();
		if (Lexer.nextToken == Token.ADD_OP || Lexer.nextToken == Token.SUB_OP) {
			op = Lexer.nextChar;
			Lexer.lex();
			e = new Expr();
			Code.gen(Code.opcode(op));
			
		}
	}
}

class Term    { // term -> factor (* | /) term | factor
	Factor f;
	Term t;
	char op;

	public Term() {
		f = new Factor();
		if (Lexer.nextToken == Token.MULT_OP || Lexer.nextToken == Token.DIV_OP) {
			op = Lexer.nextChar;
			Lexer.lex();
			t = new Term();
			Code.gen(Code.opcode(op));
			}
	}
}

class Factor { // factor -> number | '(' expr ')'
	Expr e;
	int i;
	char v;
	
	public Factor() {
		switch (Lexer.nextToken) {
		case Token.INT_LIT: // number
			i = Lexer.intValue;
			Code.gen(Code.intcode(i));
			Lexer.lex();
			break;
		case Token.ID: // id 
			v= Lexer.ident;
			Lexer.lex();
			Code.gen(Code.id(v,Lexer.nextToken));
			break;
		case Token.LEFT_PAREN: // '('
			Lexer.lex();
			e = new Expr();
			Lexer.lex(); // skip over ')'
			break;
		default:
			break;
		}
	}
}


class Code {
	static String[] code = new String[100];
	static int codeptr = 0;
	static int counter= 0;
	static Map<Character, Integer> hm = new HashMap<Character, Integer>();

	public static void gen(String s) {
		code[codeptr] = s;
		codeptr++;
	}

	public static String intcode(int i) {
		if (i > 127) return "sipush " + i;
		if (i > 5) return "bipush " + i;
		return "iconst_" + i;
	}
	
	public static String end(){
		return "return";
	}
	
	public static String id(Character v, Integer i) {
		int c;
		if(hm.containsKey(v)){
			c=hm.get(v);
			if(i==Token.ASSIGN_OP){
				if (c > 3) return "istore " + c;
				return "istore_" + c;
			}
			else{
				if (c > 3) return "iload " + c;
				return "iload_" + c;
			}
		}
		
		hm.put(v, counter);
		c=counter;
		counter++;
		return "";
		
	}

	public static String opcode(char op) {
		switch(op) {
		case '+' : return "iadd";
		case '-':  return "isub";
		case '*':  return "imul";
		case '/':  return "idiv";
		default: return "";
		}
	}

	public static void output() {
		for (int i=0; i<codeptr; i++)
			System.out.println(code[i]);
	}
}


