/* 		OBJECT-ORIENTED RECOGNIZER FOR SIMPLE EXPRESSIONS
 program ->  decls stmts end
      decls   ->  int idlist ';'
      idlist  ->  id [',' idlist ]
      stmts   ->  stmt [ stmts ]
      stmt    ->  assign ';'| cmpd | cond | loop
      assign  ->  id '=' expr
      cmpd    ->  '{' stmts '}'
      cond    ->  if '(' rexp ')' stmt [ else stmt ]
      loop    ->  for '(' [assign] ';' [rexp] ';' [assign] ')' stmt
	  rexp -> expr('<'|'>'|'=='|'!=')expr 
	  expr -> term [ ('+' | '-') expr ]
	  term -> factor [ ('*' | '/') term ]
	  factor -> int_lit | id | '(' expr ')'
*/

import java.lang.System;
import java.util.*;

public class Parser {
	public static void main(String[] args) {
		System.out.println("Enter an expression, end with \"end\"!\n");
		Lexer.lex();
		new Program();
		Code.output();
	}
}

class Program{
	Decls d;
	Stmts s;
	public Program() {
		d = new Decls();
     	s=  new Stmts();
	}
}
//d
class Decls{
	IdList i;
	public Decls(){
		if(Lexer.nextToken == Token.KEY_INT)
			Lexer.lex();
			i = new IdList();
			}
}

class IdList {
	IdList id;
	char v;
	public IdList() {
		if(Lexer.nextToken==Token.ID){
			v= Lexer.ident;
			Code.gen(Code.id(v,Lexer.nextToken));
			Lexer.lex();
			if(Lexer.nextToken == Token.SEMICOLON) {
				Lexer.lex();   //Comment this
				return;
			}
			if(Lexer.nextToken == Token.COMMA) {
				Lexer.lex();
				id = new IdList();
			}
		}
	}
}

class Stmts{
	Stmnt s;
	Stmts ss;
	public Stmts(){
		s = new Stmnt();
		if(Lexer.lex() == Token.KEY_END) { 
			Code.gen(Code.end());
			return;
		}
		else
			ss = new Stmts();
	}
}

class Stmnt{
	Assign a;
	public Stmnt(){
		//Lexer.lex();   //Uncomment this
		switch(Lexer.nextToken) {
		case Token.ID:
			a= new Assign();
			break;
		default:
			break;
		}
	}
}

class Assign{ //assign  ->  id '=' expr ';'
	char id;
	Expr e;
	Factor f;
	char c;
	public Assign(){
		if(Lexer.nextToken==Token.ID){
			c = Lexer.ident;
			Lexer.lex();
			if(Lexer.nextToken == Token.ASSIGN_OP){
				Lexer.lex();
				e = new Expr();
			}
		}
		Code.gen(Code.id(c,Token.ASSIGN_OP));
	}
}

class Rexpr{ //rexp -> expr('<'|'>'|'=='|'!=')expr
	Expr e1;
	Expr e2;
	char op;
	
	public Rexpr(){
		e1 = new Expr();
		int token = Lexer.nextToken;
		if ( token == Token.GREATER_OP || token == Token.LESSER_OP || token == Token.EQ_OP || token == Token.NOT_EQ){
			op=Lexer.nextChar;
			Lexer.lex();
			e2 = new Expr();
			Code.gen(Code.opcode(op));
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
			v= Lexer.ident;
			Code.gen(Code.id(v,Lexer.nextToken));
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
	static int spacePtr=0;;
	static Map<Character, Integer> hm = new HashMap<Character, Integer>();

	public static void gen(String s) {
		if(!s.isEmpty()) {
			code[codeptr] = s;
		codeptr++;
		}
	}

	public static String intcode(int i) {
		int a=spacePtr++;
		if (i > 127) {
			spacePtr++;
			spacePtr++;
			return a+": sipush " + i;
		}
		if (i > 5) {
			spacePtr++;
			return a+": bipush " + i;
		}
		return a+": iconst_" + i;
	}
	
	
	public static String id(Character v, Integer i) {
		int c;
		if(hm.containsKey(v)){
			int space = spacePtr++;
			c=hm.get(v);
			if(i==Token.ASSIGN_OP){
				if (c+1 > 3) { 
					spacePtr++;
					return space+": istore " + Integer.toString(c+1);
				}
				return space+": istore_" + Integer.toString(c+1);
			}
			else{
				if (c+1 > 3) {
					spacePtr++;
					return space+": iload " + Integer.toString(c+1);
				}	
				return space+": iload_" + Integer.toString(c+1);
			}
		}
		else {
			hm.put(v, counter);
			c=counter;
			counter++;
			return "";
		}
		
	}

	public static String opcode(char op) {
		int a = spacePtr++;
		switch(op) {
		case '+' : { return a+": iadd"; }
		case '-':  { return a+": isub"; }
		case '*':  { return a+": imul"; }
		case '/':  { return a+": idiv"; }
		default: return "";
		}
	}

	public static void output() {
		for (int i=0; i<codeptr; i++)
			System.out.println(code[i]);
	}
	public static String end() {
		return Code.spacePtr+": return";
	}
}


