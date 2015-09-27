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
				Lexer.lex();   
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
		if(Lexer.nextToken == Token.KEY_END || Lexer.lex() == Token.KEY_END) { 
			Code.gen(Code.end());
			return;
		}
		else if(Lexer.nextToken == Token.RIGHT_BRACE) {
			//Lexer.lex();
			return;
		}
		else
			ss = new Stmts();
	}
}

class Stmnt{
	Assign a;
	Cond c;
	Cmpd cmpd;
	Loop l;
	public Stmnt(){
		//Lexer.lex();   //Uncomment this
		switch(Lexer.nextToken) {
		case Token.ID:
			a = new Assign();
			break;
		case Token.KEY_IF:
			c= new Cond();
			break;
		case Token.KEY_FOR:
			l=new Loop();
			break;
		case Token.LEFT_BRACE:
			Lexer.lex();
			cmpd = new Cmpd();
		default:
			break;
		}
	}
}

class Cmpd {
	Stmts st;
	public Cmpd() {
		st = new Stmts();
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

class Cond{
	Rexpr r;
	Stmnt s1,s2;
	int ptr,ptr2;
	public Cond(){
		Lexer.lex();//Change this
		Lexer.lex();
		ptr=Code.getcodeptr();
		r=new Rexpr();
		Lexer.lex();
		s1=new Stmnt();
		if(Lexer.lex()!=Token.KEY_ELSE){
			Code.gen(Code.condition(ptr,true));
			return;
		}
		ptr2=Code.getcodeptr();
		Code.gen(Code.condition(ptr,false));
		Lexer.lex();
		s2=new Stmnt();
		Code.gotofunc(ptr2);
	}	
}

class Loop{
	Assign a1,a2;
	Stmnt s;
	Rexpr r;
	int flag,start1,end1,end2,cmpPtr,spacePtr;
	int resetCode, resetSpace;
	public Loop(){
		Lexer.lex();
		Lexer.lex();
		if(Lexer.nextToken != Token.SEMICOLON)
			a1= new Assign();
		spacePtr = Code.getSpacePtr();
		Lexer.lex();
		cmpPtr=Code.getcodeptr();
		r= new Rexpr();
		resetSpace = Code.getSpacePtr();
		resetCode = Code.getcodeptr();
		start1=Code.getcodeptr();
		Lexer.lex();
		if(Lexer.nextToken != Token.RIGHT_PAREN)
			a2=new Assign();
		String [] assiArray = Code.assArrayWithReset(resetSpace, resetCode);
		Lexer.lex();
		s = new Stmnt();
		if(assiArray.length != 0)
			Code.generateAssign(assiArray);
		Code.gen(Code.genGoTo(spacePtr));
		Code.gen(Code.condition(cmpPtr,true));
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
			Lexer.lex();
			e2 = new Expr();
			Code.gen(Code.opcoderexpr(token));
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
	
	public static int getcodeptr(){
		return codeptr;
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
	
	public static String condition(int i,Boolean b){
		if(b){
			code[i+2]=code[i+2]+" "+spacePtr;
		}
		else{
			gen(spacePtr+": "+"goto");
			spacePtr+=3;
			code[i+2]=code[i+2]+" "+spacePtr;
		}
		return "";
	}
	public static void loop(int s1,int e1,int e2){
		String [] assin=  new String[e1-s1];
		int ptr = 0,str1 = s1, count = assin.length;
		for(int i=s1;i<e1;i++)
			assin[ptr++] = code[i];
		while(e2-e1 > 0) {
			String a[] = code[str1].split(":");
			String b[] = code[e1].split(":");
			code[str1] = a[0]+":"+b[b.length-1];
			e1++;
			str1++;
		}
		while(count-- > 0) {
			 e2--;
			String a[] = assin[count].split(":");
			String b[] = code[e2].split(":");
			code[e2] = b[0]+":"+a[a.length-1];
		   
		}
	}

	public static void gotofunc(int i){
		code[i]=code[i]+" "+spacePtr;	
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
	
	public static String opcoderexpr(int op) {
		int a = spacePtr;
		spacePtr+=3;
		String s = "if_icmp";
		switch(op) {
		case Token.GREATER_OP : { return a+": "+s+"le"; }
		case Token.LESSER_OP:  { return a+": "+s+"ge"; }
		case Token.EQ_OP:  { return a+": "+s+"ne"; }
		case Token.NOT_EQ:  { return a+": "+s+"eq"; }
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
	public static String genGoTo(int loadStart) {
		int a=spacePtr;
		spacePtr+=3;
		return a+": goto "+loadStart; 
	}
	public static int getSpacePtr() {
		return spacePtr;
	}
	public static void setSpacePtr(int spacePoint) {
		spacePtr = spacePoint;
	}
	public static void setCodePtr(int ptr) {
		codeptr = ptr;
	}
	public static String[] assArrayWithReset(int space, int codePt) {
		String assignment [] = new String[codeptr-codePt];
		int temp = 0, count = codePt;
		if(count != codeptr) {
		while(count != codeptr)
			assignment[temp++] = code[count++];
		codeptr = codePt;
		spacePtr = space;
		}
		return assignment;
	}
	public static void generateAssign(String [] array) {
		String []a={""};
		String []b={""};
		for(int i=0;i<array.length-1;i++) {
			a =array[i].split(":");
			b = array[i+1].split(":");
			int diff = Integer.parseInt(b[0]) - Integer.parseInt(a[0]);
			code[codeptr++] =  spacePtr+":"+a[a.length-1];
			spacePtr+=diff;
		}
			code[codeptr++] = spacePtr+":"+b[b.length-1];
			spacePtr++;
	}
}
