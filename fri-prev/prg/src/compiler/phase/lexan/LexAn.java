package compiler.phase.lexan;

import java.io.*;
import java.util.Arrays;
import java.util.Stack;

import compiler.*;
import compiler.common.report.*;
import compiler.phase.*;
import compiler.phase.lexan.Symbol.Token;

/**
 * The lexical analyzer.
 * 
 * @author juliette
 */
public class LexAn extends Phase {

	/** The source file. */
	private FileReader srcFile;

	private String fileName;
	
	private Stack<Symbol> st;
	/**
	 * Constructs a new lexical analyzer.
	 * 
	 * Opens the source file and prepares the buffer. If logging is requested,
	 * sets up the logger.
	 * 
	 * @param task.srcFName
	 *            The name of the source file name.
	 */
	public LexAn(Task task) {
		super(task, "lexan");
		
		fileName = this.task.srcFName;
		st = new Stack<Symbol>();
		// Open the source file.
		try {
			srcFile = new FileReader(this.task.srcFName);
		} catch (FileNotFoundException ex) {
			throw new CompilerError("Source file '" + this.task.srcFName + "' not found.");
		}
	}

	/**
	 * Terminates lexical analysis. Closes the source file and, if logging has
	 * been requested, this method produces the report by closing the logger.
	 */
	@Override
	public void close() {
		// Close the source file.
		
		if (srcFile != null) {
			try {
				srcFile.close();
			} catch (IOException ex) {
				Report.warning("Source file '" + task.srcFName + "' cannot be closed.");
			}
		}
		super.close();
	}

	/**
	 * Returns the next lexical symbol from the source file.
	 * 
	 * @return The next lexical symbol.
	 */
	public Symbol lexAn() {
		return st.pop();
	}
	public void symols() {
		Stack<Symbol> stack = new Stack<Symbol>();
		int c;
		int index = 0;
		char[] buffer = new char[1024];
		try {
			while ((c = srcFile.read()) != -1) {
			    char ch = (char) c;
			    buffer[index++] = ch;
			    if(index == buffer.length)
			    	buffer = Arrays.copyOf(buffer, buffer.length*2);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		Symbol s = null;
		Position position = null;
		int line = 1;
		int column = 0;
		for(int i = 0; i < index; i++)
		{
			column++;
		    /** new line **/
			if(buffer[i] == 10 || buffer[i] == 13)
		    {
		    	column = 0;
		    	line++;
		    }
			else if(buffer[i] == ' ' || buffer[i] == '\t')
		    	//do nothing
				continue;
			else if(buffer[i] == '\t')
				column += 8;
			else if(buffer[i] == '+')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.ADD,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == '&')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.AND,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == '=' && i+1 < index && buffer[i+1] == '=')
		    {
		    	position = new Position(fileName, line, column, fileName, line, column+=1);
		    	s = new Symbol(Token.EQU,position);
		    	log(s);
		    	i++;
		    	stack.push(s);
		    }
		    else if(buffer[i] == '=')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.ASSIGN,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == ':')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.COLON,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == ',')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.COMMA,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == '}')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.CLOSING_BRACE,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == ']')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.CLOSING_BRACKET,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == ')')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.CLOSING_PARENTHESIS,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == '.')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.DOT,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == '/')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.DIV,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == '>' && i+1 < index && buffer[i+1] == '=')
		    {
		    	position = new Position(fileName, line, column, fileName, line, column+=1);
		    	s = new Symbol(Token.GEQ,position);
		    	log(s);
		    	i++;
		    	stack.push(s);
		    }
		    else if(buffer[i] == '>')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.GTH,position);
		    	log(s);
		    	stack.push(s);
		    }	
		    else if(buffer[i] == '<' && i+1 < index && buffer[i+1] == '=')
		    {
		    	position = new Position(fileName, line, column, fileName, line, column+=1);
		    	s = new Symbol(Token.LTH,position);
		    	log(s);
		    	i++;
		    	stack.push(s);
		    }
		    else if(buffer[i] == '<')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.LEQ,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == '@')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.MEM,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == '%')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.MOD,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == '*')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.MUL,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == '!' && i+1 < index && buffer[i+1] == '=')
		    {
		    	position = new Position(fileName, line, column, fileName, line, column+=1);
		    	s = new Symbol(Token.NEQ,position);
		    	log(s);
		    	i++;
		    	stack.push(s);
		    }
		    else if(buffer[i] == '!' )
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.NOT,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == '{')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.OPENING_BRACE,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == '[')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.OPENING_BRACKET,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == '(')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.OPENING_PARENTHESIS,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == '|')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.OR,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == '-')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.SUB,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == '^')
		    {
		    	position = new Position(fileName, line, column);
		    	s = new Symbol(Token.VAL,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] >= '0' && buffer[i] <= '9')
		    {
				/** Integer constant. */
		    	int begColumn = column;
		    	int begI = i;
		   		while((buffer[i] >= '0' && buffer[i] <= '9'))
				{
					column++;
					i++;
				}
				column--;
				i--;
				position = new Position(fileName, line, begColumn, fileName, line, column);
				String lexeme = new String(buffer, begI, i-(begI-1));
		    	s = new Symbol(Token.CONST_INTEGER,lexeme,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == 39)
		    {
		    	/** Character constant. */
		    	/** empty char '' **/
		    	if(i+1 < index && buffer[i+1] == 39)
			    	throw(new CompilerError("Invalid character constant at line " + line + ", column "+column));
		    	if(buffer[i+1] == 92)
			    {
					if(i+3 < index && buffer[i+3] == 39)
					{
				    	if(buffer[i+2] == 92 || buffer[i+2] == 39 || buffer[i+2] == 34 || buffer[i+2] == 't' || buffer[i+2] == 'n')
				    	{
					    	position = new Position(fileName, line, column, fileName, line, column+=3);
					    	String lexeme = new String(buffer, i, (i+3)-(i-1));
					    	s = new Symbol(Token.CONST_CHAR,lexeme,position);
					    	log(s);
					    	i+=3;
					    	stack.push(s);
				    	}
				    	else
				    		throw(new CompilerError("Invalid escape sequence at line " + line + ", column "+column));
				    }
				    else
				    	throw(new CompilerError("Character constant is not properly closed by a single-quote  at line " + line + ", column "+column));
			    }
			    else if(i+1 == index || buffer[i+1] == '\n' || buffer[i+1] == '\t')
			    	throw(new CompilerError("Character constant is not properly closed by a single-quote  at line " + line + ", column "+column));
			    else if(i+2 == index || buffer[i+2] != 39)
			    		throw(new CompilerError("Character constant is not properly closed by a single-quote  at line " + line + ", column "+column));
			    else if(buffer[i+1] >= 32 && buffer[i+1] <= 126)
			    {
			    	String lexeme = new String(buffer, i, (i+2)-(i-1));
			    	position = new Position(fileName, line, column, fileName, line, column+=2);
			    	s = new Symbol(Token.CONST_CHAR,lexeme,position);
			    	log(s);
			    	i+=2;
			    	stack.push(s);
			    }
			    else
			    	throw(new CompilerError("Invalid character constant at line " + line + ", column "+column));
			   
		    }
		    else if(buffer[i] == '"')
		    {
		    	/** String constant. */
		    	int begColumn = column;
		    	int begI = i;
		    	i++;
		    	column++;
				while(buffer[i] != '"' )
				{
					/** error cause ' **/
					if(buffer[i] == 39)
				    	throw(new CompilerError("Invalid string constant at line " + line + ", column "+column));
					else if(buffer[i] == 92)
					{
						i++;
						column++;
						/** != \ && != ' && != " && != t && != n **/
						if(buffer[i] != 92 && buffer[i] != 39  && buffer[i] != 34  && buffer[i] != 't'  && buffer[i] != 'n')
					    	throw(new CompilerError("Invalid escape sequence at line " + line + ", column "+column));
					}
					else if(buffer[i] == '\t')
					{
				    	throw(new CompilerError("string with illegal character TAB at line " + line + ", column "+column));
					}
					else if(buffer[i] == '\n')
				    	throw(new CompilerError("String literal is not properly closed by a double-quote  at line " + line + ", column "+column));
					else if(i == index)
				    	throw(new CompilerError("Invalid string constant at line " + line + ", column "+column));
					else if(!(buffer[i] >= 0 && buffer[i] <= 127))
				    	throw(new CompilerError("Unexpected character at line " + line + ", column "+column));	
					column++;
					i++;
				}
				int endColumn = column;
				position = new Position(fileName, line, begColumn, fileName, line, endColumn);
				String lexeme = new String(buffer, begI, i-(begI-1));
		    	s = new Symbol(Token.CONST_STRING,lexeme,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if((buffer[i] >= 'a' && buffer[i] <= 'z') || (buffer[i] >= 'A' && buffer[i] <= 'Z') || buffer[i] == '_')
		    {
		    	/** Identifier. */
		    	int begColumn = column;
		    	int begI = i;
				while((buffer[i] >= 'a' && buffer[i] <= 'z') || (buffer[i] >= 'A' && buffer[i] <= 'Z') || (buffer[i] >= '0' && buffer[i] <= '9') || buffer[i] == '_')
				{
					i++;
					column++;
				}
				i--;
				column--;
				int endColumn = column;
				position = new Position(fileName, line, begColumn, fileName, line, endColumn);
				String lexeme = new String(buffer, begI, i-(begI-1));
				if(lexeme.equals("false"))
					s = new Symbol(Token.CONST_BOOLEAN,lexeme,position);
				else if(lexeme.equals("true"))
					s = new Symbol(Token.CONST_BOOLEAN,lexeme,position);
				else if(lexeme.equals("null"))
					s = new Symbol(Token.CONST_NULL,lexeme,position);
				else if(lexeme.equals("none"))
					s = new Symbol(Token.CONST_NONE,lexeme,position);
				else if(lexeme.equals("integer"))
					s = new Symbol(Token.INTEGER,lexeme,position);
				else if(lexeme.equals("boolean"))
					s = new Symbol(Token.BOOLEAN,lexeme,position);
				else if(lexeme.equals("char"))
					s = new Symbol(Token.CHAR,lexeme,position);
				else if(lexeme.equals("string"))
					s = new Symbol(Token.STRING,lexeme,position);
				else if(lexeme.equals("void"))
					s = new Symbol(Token.VOID,lexeme,position);	
				else if(lexeme.equals("arr"))
					s = new Symbol(Token.ARR,lexeme,position);
				else if(lexeme.equals("else"))
					s = new Symbol(Token.ELSE,lexeme,position);
				else if(lexeme.equals("end"))
					s = new Symbol(Token.END,lexeme,position);
				else if(lexeme.equals("for"))
					s = new Symbol(Token.FOR,lexeme,position);
				else if(lexeme.equals("fun"))
					s = new Symbol(Token.FUN,lexeme,position);
				else if(lexeme.equals("if"))
					s = new Symbol(Token.IF,lexeme,position);
				else if(lexeme.equals("then"))
					s = new Symbol(Token.THEN,lexeme,position);
				else if(lexeme.equals("ptr"))
					s = new Symbol(Token.PTR,lexeme,position);
				else if(lexeme.equals("rec"))
					s = new Symbol(Token.REC,lexeme,position);
				else if(lexeme.equals("typ"))
					s = new Symbol(Token.TYP,lexeme,position);
				else if(lexeme.equals("var"))
					s = new Symbol(Token.VAR,lexeme,position);
				else if(lexeme.equals("where"))
					s = new Symbol(Token.WHERE,lexeme,position);
				else if(lexeme.equals("while"))
					s = new Symbol(Token.WHILE,lexeme,position);
				else if(lexeme.equals("do"))
					s = new Symbol(Token.DO,lexeme,position);
				else 
					s = new Symbol(Token.IDENTIFIER,lexeme,position);
		    	log(s);
		    	stack.push(s);
		    }
		    else if(buffer[i] == '#')
		    {
		    	/** comment. */
				while(buffer[i] != '\n')
				{
					if(!(buffer[i+1] >= 0 && buffer[i+1] <= 127))
				    	throw(new CompilerError("Comment contains only ASCII characters at line " + line + ", column " + column));
					i++;
					if(i == index)
				    	throw(new CompilerError("Comment must end with LF not EOF at line " + line + ", column " + column));
				}
				line++;
				column=0;
		    }
		    else 
		    	throw(new CompilerError("Unrecognisable symbol at line " + line + ", column " + column));
		}
		column++;
		position = new Position(fileName, line, column);
		s = new Symbol(Token.EOF,position);
		log(s);
    	stack.push(s);
    	while(!stack.empty())
    	{
    		st.push(stack.pop());
    	}
	}

	/**
	 * Prints out the symbol and returns it.
	 * 
	 * This method should be called by the lexical analyzer before it returns a
	 * symbol so that the symbol can be logged (even if logging of lexical
	 * analysis has not been requested).
	 * 
	 * @param symbol
	 *            The symbol to be printed out.
	 * @return The symbol received as an argument.
	 */
	private Symbol log(Symbol symbol) {
		symbol.log(logger);
		return symbol;
	}

}