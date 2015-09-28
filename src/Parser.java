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
		Lexer.lex();             //Start Parser
		new Program();          
		Code.output();
	}
}

class Program{
	Decls d;
	Stmts s;
	public Program() {
		d = new Decls();         //Declaration
     	s=  new Stmts();         //Statements
	}
}

class Decls{
	IdList i;
	public Decls(){
		if(Lexer.nextToken == Token.KEY_INT)     //If Key_INT next cursor and call IDlist
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
			Code.gen(Code.id(v,Lexer.nextToken));          //Map the variable
			Lexer.lex();								
			if(Lexer.nextToken == Token.SEMICOLON) {	  //If semicolon Lex and return
				Lexer.lex();   
				return;
			}
			if(Lexer.nextToken == Token.COMMA) {          //If comma, new IDList again
				Lexer.lex();
				id = new IdList();
			}
		}
	}
}

class Stmts{
	Stmt s;
	Stmts ss;
	public Stmts(){
		s = new Stmt();
		if(Lexer.nextToken == Token.KEY_END || Lexer.lex() == Token.KEY_END) { 
			Code.gen(Code.end());						//Generate return code
			return;
		}
		else if( Lexer.nextToken == Token.RIGHT_BRACE ) {     //Right brace when compound statement
			return;
		}
		else
			ss = new Stmts();								//Recursively call Statements until end
	}
}

class Stmt{
	Assign a;
	Cond c;
	Cmpd cmpd;
	Loop l;
	public Stmt(){
		switch(Lexer.nextToken) {              //Statement will be either
		case Token.ID:
			a = new Assign();                  // Assign or
			break;
		case Token.KEY_IF:                     // Cond or
			c= new Cond(); 
			break;
		case Token.KEY_FOR:                    //For or
			l=new Loop();
			break;
		case Token.LEFT_BRACE:                //Cmpnd
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
		st = new Stmts(); 					//Call Statements again to create Bracket indentation
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
				e = new Expr();					//Expression will be called in right side of "=" operator
			}
		}
		Code.gen(Code.id(c,Token.ASSIGN_OP));			//Generate "store" Bytecode
	}
}

class Cond{
	Rexpr r;
	Stmt s1,s2;
	int ptr1,ptr2;
	public Cond(){
		Lexer.lex();			
		Lexer.lex();		//Move to next Token after LeftBrace
		r=new Rexpr();
		ptr1=Code.getcodeptr();		//Save CodePtr to use later for adding imcmpgne "Bytecode number"
		Lexer.lex();
		s1=new Stmt();				
		if(Lexer.nextToken != Token.KEY_END && Lexer.lex()!=Token.KEY_ELSE){	//	if else is called then goto "Bytecode number" is generated
			Code.gen(Code.condition(ptr1,true));
			return;
		}
		ptr2=Code.getcodeptr();				//Called to get the end statement for putting back in If compare Bytecode
		Code.gen(Code.condition(ptr1,false)); //number
		Lexer.lex();						
		s2=new Stmt();						
		Code.gotofunc(ptr2);				//Goto statement will be called after stmts in If are generated
	}	
}

class Loop{
	Assign a1,a2;
	Stmt s;
	Rexpr r;
	int cmpPtr,spacePtr, tempAssign;
	int resetCode, resetSpace;
	public Loop(){
		Lexer.lex();
		Lexer.lex();                      //Bring Cursor to initialization of for 
		if(Lexer.nextToken != Token.SEMICOLON)
			a1= new Assign();			//Assignment statement called if only semicolon as no code will be generated for ;
		spacePtr = Code.getSpacePtr();	
		Lexer.lex();
		r= new Rexpr();
		cmpPtr=Code.getcodeptr();
		resetSpace = Code.getSpacePtr();		//Get the place where to reset the spacePtr, ie After Imcpgne(Rexpr)
		resetCode  = Code.getcodeptr();			//	Get the arrayplace to reset codeptr
		Lexer.lex();
		if(Lexer.nextToken != Token.RIGHT_PAREN)
			a2=new Assign();
		tempAssign = Code.getSpacePtr();
		String [] assiArray = Code.assArrayWithReset(resetSpace, resetCode);	//Store assignment stmt in array and reset
		Lexer.lex();															// array and space ptrs
		s = new Stmt();
		if(assiArray.length != 0)
			Code.generateAssign(assiArray);				//Restore the generated array after the statements after Stmt has 
		Code.gen(Code.genGoTo(spacePtr));				//executed and bring space and arrayPtrs up to speed
		if(spacePtr - tempAssign != 0)
			Code.gen(Code.condition(cmpPtr,true));			//Previous statement creates a goto for the for statement
	}													//Previous adds the proper line no to Ifcmpgne "Byte Space No"
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
		int t;
		t=i-1;
		if(b){				//If true then code for ifcmpg is being generated
			code[t]=code[t]+" "+spacePtr;
		}
		else{				//Else code generation for "else" is being created
			gen(spacePtr+": "+"goto");
			spacePtr+=3;
			code[t]=code[t]+" "+spacePtr;
		}
		return "";
	}
	public static void gotofunc(int i){
		code[i]=code[i]+" "+spacePtr;	
	}
	
	public static String id(Character v, Integer i) {
		int c;
		if(hm.containsKey(v)){		//If key is contained which is false for case of declaration
			int space = spacePtr++;
			c=hm.get(v);
			if(i==Token.ASSIGN_OP){
				if (c+1 > 3) { 
					spacePtr++;
					return space+": istore " + Integer.toString(c+1);	//istore for case of assignment
				}
				return space+": istore_" + Integer.toString(c+1);
			}
			else{
				if (c+1 > 3) {
					spacePtr++;
					return space+": iload " + Integer.toString(c+1);		//Case of loading the variables
				}	
				return space+": iload_" + Integer.toString(c+1);
			}
		}
		else {				//Variables are being mapped to their values
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
		cleanReturn();
		for (int i=0; i<codeptr; i++)
			System.out.println(code[i]);
	}
	public static void cleanReturn() {		//Removes extra return statement created due to Stmt first condition
		String [] cleaner = new String[100];	//Makes no other changes to code array
		int i;
		int codeP = codeptr;
		int count=0;
		for(i=0;i<codeP-1;i++) {
			if(!code[i].contains("return"))
				cleaner[count++] = code[i];
			else
				codeptr--;
		}
		cleaner[count++] = code[i]; 
		code = cleaner;
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
		String assignment [] = new String[codeptr-codePt]; //Creates array for assignments statements to be returned.
		int temp = 0, count = codePt;	
		if(count != codeptr) {			//If count is equal ie No assignment statements then return empty array
		while(count != codeptr)			//Puts values from codePt to Last code pointer
			assignment[temp++] = code[count++];
		codeptr = codePt;				//Resets the space and code pointers
		spacePtr = space;
		}
		return assignment;
	}
	public static void generateAssign(String [] array) {	//Puts the assignment statements in the right order along
		String []a={""};									//with correct space pointers
		String []b={""};
		for(int i=0;i<array.length-1;i++) {
			a =array[i].split(":");			//"Split a around : and similar with the next array
			b = array[i+1].split(":");
			int diff = Integer.parseInt(b[0]) - Integer.parseInt(a[0]);	//Calculate next space ptr difference between two
			code[codeptr++] =  spacePtr+":"+a[a.length-1];				//successive bytecodes and then increment the space
			spacePtr+=diff;												//Ptr by this amount
		}																//In previous statement 419, correct Bytenumber is
			code[codeptr++] = spacePtr+":"+b[b.length-1];				//being generated
			spacePtr++;
	}
}
