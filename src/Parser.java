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
		while(Lexer.lex()!=21)
		{
			new Assign();
		}
		Code.output();
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
	
	public Assign(){
		id = Lexer.nextChar;
		Lexer.lex();
		if(Lexer.nextToken == Token.ASSIGN_OP){
			Lexer.lex();
			e = new Expr();
			Code.gen(Code.add_var(id));
		}
	}
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
			v= Lexer.nextChar;
			Code.gen(Code.id(v));
			Lexer.lex();
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
	
	public static String add_var(char v){
		hm.put(v, counter);
		if (counter > 3) return "istore " + counter++;
		return "istore_" + counter++;
	}
	
	
	public static String id(Character v) {
		int k=hm.get(v);
		if (k > 3) return "iload " + k;
		return "iload_" + k;
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


