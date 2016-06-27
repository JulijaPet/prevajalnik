package compiler.phase.synan;

import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import compiler.Task;
import compiler.common.logger.Transformer;
import compiler.common.report.CompilerError;
import compiler.common.report.Position;
import compiler.common.report.Report;
import compiler.data.ast.ArrType;
import compiler.data.ast.AtomExpr;
import compiler.data.ast.AtomExpr.AtomTypes;
import compiler.data.ast.AtomType;
import compiler.data.ast.BinExpr;
import compiler.data.ast.BinExpr.Oper;
import compiler.data.ast.CastExpr;
import compiler.data.ast.CompDecl;
import compiler.data.ast.CompName;
import compiler.data.ast.Decl;
import compiler.data.ast.Expr;
import compiler.data.ast.Exprs;
import compiler.data.ast.ForExpr;
import compiler.data.ast.FunCall;
import compiler.data.ast.FunDecl;
import compiler.data.ast.FunDef;
import compiler.data.ast.IfExpr;
import compiler.data.ast.ParDecl;
import compiler.data.ast.Program;
import compiler.data.ast.PtrType;
import compiler.data.ast.RecType;
import compiler.data.ast.Type;
import compiler.data.ast.TypeDecl;
import compiler.data.ast.UnExpr;
import compiler.data.ast.TypeName;
import compiler.data.ast.VarDecl;
import compiler.data.ast.VarName;
import compiler.data.ast.WhereExpr;
import compiler.data.ast.WhileExpr;
import compiler.phase.Phase;
import compiler.phase.lexan.LexAn;
import compiler.phase.lexan.Symbol;

/**
 * The syntax analyzer.
 * 
 * @author juliette
 */
public class SynAn extends Phase {

	/** The lexical analyzer. */
	private final LexAn lexAn;

	/**
	 * Constructs a new syntax analyzer.
	 * 
	 * @param lexAn
	 *            The lexical analyzer.
	 */
	public SynAn(Task task) {
		super(task, "synan");
		this.lexAn = new LexAn(task);
		if (this.logger != null) {
			this.logger.setTransformer(//
					new Transformer() {
						// This transformer produces the
						// left-most derivation.

						private String nodeName(Node node) {
							Element element = (Element) node;
							String nodeName = element.getTagName();
							if (nodeName.equals("nont")) {
								return element.getAttribute("name");
							}
							if (nodeName.equals("symbol")) {
								return element.getAttribute("name");
							}
							return null;
						}

						private void leftMostDer(Node node) {
							if (((Element) node).getTagName().equals("nont")) {
								String nodeName = nodeName(node);
								NodeList children = node.getChildNodes();
								StringBuffer production = new StringBuffer();
								production.append(nodeName + " -->");
								for (int childIdx = 0; childIdx < children.getLength(); childIdx++) {
									Node child = children.item(childIdx);
									String childName = nodeName(child);
									production.append(" " + childName);
								}
								Report.info(production.toString());
								for (int childIdx = 0; childIdx < children.getLength(); childIdx++) {
									Node child = children.item(childIdx);
									leftMostDer(child);
								}
							}
						}

						public Document transform(Document doc) {
							leftMostDer(doc.getDocumentElement().getFirstChild());
							return doc;
						}
					});
		}
	}

	/**
	 * Terminates syntax analysis. Lexical analyzer is not closed and, if
	 * logging has been requested, this method produces the report by closing
	 * the logger.
	 */
	@Override
	public void close() {
		lexAn.close();
		super.close();
	}

	/** The parser's lookahead buffer. */
	private Symbol laSymbol;

	/**
	 * Reads the next lexical symbol from the source file and stores it in the
	 * lookahead buffer (before that it logs the previous lexical symbol, if
	 * requested); returns the previous symbol.
	 * 
	 * @return The previous symbol (the one that has just been replaced by the
	 *         new symbol).
	 */
	private Symbol nextSymbol() {
		Symbol symbol = laSymbol;
		symbol.log(logger);
		laSymbol = lexAn.lexAn();
		return symbol;
	}

	/**
	 * Logs the error token inserted when a missing lexical symbol has been
	 * reported.
	 * 
	 * @return The error token (the symbol in the lookahead buffer is to be used
	 *         later).
	 */
	private Symbol nextSymbolIsError() {
		Symbol error = new Symbol(Symbol.Token.ERROR, "", new Position("", 0, 0));
		error.log(logger);
		return error;
	}

	/**
	 * Starts logging an internal node of the derivation tree.
	 * 
	 * @param nontName
	 *            The name of a nonterminal the internal node represents.
	 */
	private void begLog(String nontName) {
		if (logger == null)
			return;
		logger.begElement("nont");
		logger.addAttribute("name", nontName);
	}

	/**
	 * Ends logging an internal node of the derivation tree.
	 */
	private void endLog() {
		if (logger == null)
			return;
		logger.endElement();
	}

	/**
	 * The parser.
	 * 
	 * This method performs the syntax analysis of the source file.
	 * @return 
	 */
	public Program synAn() {
		lexAn.symols();
		laSymbol = lexAn.lexAn();
		Program p = parseProgram();
		if (laSymbol.token != Symbol.Token.EOF)
			Report.warning(laSymbol, "Unexpected symbol(s) at the end of file.");
		return p;
	}

	// All these methods are a part of a recursive descent implementation of an
	// LL(1) parser.
	private Program parseProgram() {
		begLog("Program");
		Program p = null;
		switch (laSymbol.token) {
		case ADD: case SUB: case NOT: case MEM: case OPENING_BRACKET: case IDENTIFIER: case CONST_INTEGER: case CONST_BOOLEAN: case CONST_CHAR:
		case CONST_STRING: case CONST_NULL: case CONST_NONE: case OPENING_PARENTHESIS: case IF: case FOR: case WHILE: case DO:
		{
			Position pos = laSymbol;
			Expr expr = parseExpression();
			p = new Program(new Position(pos, laSymbol), expr);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Program"));
		}
		endLog();
		return p;
	}
	
	private Expr parseExpression() {
		begLog("Expression");
		Expr expr = null;
		switch (laSymbol.token) {
		case ADD: case SUB: case NOT: case MEM: case OPENING_BRACKET: case IDENTIFIER: case CONST_INTEGER: case CONST_BOOLEAN: case CONST_CHAR:
		case CONST_STRING: case CONST_NULL: case CONST_NONE: case OPENING_PARENTHESIS: case IF: case FOR: case WHILE: case DO:
		{
			expr = parseExpression_(parseAssignmentExpression());
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Expression"));
		}
		endLog();
		return expr;
	}

	private Expr parseExpression_(Expr expr) {
		begLog("Expression'");
		switch (laSymbol.token) {
		case WHERE: {
			Symbol symbol = nextSymbol();
			Symbol symId;
			
			LinkedList<Decl> decls = parseDeclarations();
			
			if (laSymbol.token == Symbol.Token.END) {
				nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing end inserted.");
				throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token));
			}
			expr = parseExpression_(expr);
			expr = new WhereExpr(new Position(symbol, laSymbol), expr, decls);
			break;
		}
		case END: case COMMA: case CLOSING_BRACKET: case CLOSING_PARENTHESIS:			
		case THEN: case ELSE: case COLON: case TYP: case FUN: case VAR: case EOF: case WHILE:
		{
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Expression_"));
		}
		endLog();	
		return expr;
	}
	private LinkedList<Expr> parseExpressions()
	{
		begLog("Expressions");
		LinkedList<Expr> expr = new LinkedList<Expr>();
		switch (laSymbol.token) {
		case ADD: case SUB: case NOT: case MEM: case OPENING_BRACKET:
		case IDENTIFIER: case CONST_INTEGER: case CONST_BOOLEAN: case CONST_CHAR:
		case CONST_STRING: case CONST_NULL: case CONST_NONE: case OPENING_PARENTHESIS:
		case IF:  case FOR: case WHILE: case DO:
		{
			expr.add(parseExpression());
			parseExpressions_(expr);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Expressions"));
		}
		endLog();
		return expr;
	}
	private void parseExpressions_(LinkedList<Expr> expr)
	{
		begLog("Expressions'");
		switch (laSymbol.token) {
		case COMMA:
		{
			Symbol symbol = nextSymbol();
			expr.add(parseExpression());
			parseExpressions_(expr);
			break;
		}
		case CLOSING_PARENTHESIS:
		{
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Expressions_"));
		}
		endLog();
	}
	private Expr parseAssignmentExpression() {
		begLog("AssignmentExpression");
		Expr expr = null;
		switch (laSymbol.token) {
		case ADD: case SUB: case NOT: case MEM: case OPENING_BRACKET: case IDENTIFIER: case CONST_INTEGER: case CONST_BOOLEAN: case CONST_CHAR:
		case CONST_STRING: case CONST_NULL: case CONST_NONE: case OPENING_PARENTHESIS: case IF: case FOR: case WHILE: case DO:
		{
			expr = parseAssignmentExpression_(parseDisjunctiveExpression());
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Assignment expression"));
		}
		endLog();
		return expr;
	}
	private Expr parseAssignmentExpression_(Expr expr) {
		begLog("AssignmentExpression'");
		switch (laSymbol.token) {
		case WHERE: case END: case COMMA: case CLOSING_BRACKET: 
			case CLOSING_PARENTHESIS: case THEN: case ELSE:
			case COLON: case TYP: case FUN: case VAR: case EOF: case WHILE:
		{
			break;
		}
		case ASSIGN:
		{
			Symbol symbol = nextSymbol();
			Expr sndExpr = parseDisjunctiveExpression();
			expr = new BinExpr(new Position(expr, sndExpr), Oper.ASSIGN, expr, sndExpr);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Disjunctive expression"));
		}
		endLog();
		return expr;
	}
	private Expr parseDisjunctiveExpression() {
		begLog("DisjunctiveExpression");
		Expr expr = null;
		switch (laSymbol.token) {
		case ADD: case SUB: case NOT: case MEM: case OPENING_BRACKET: case IDENTIFIER: case CONST_INTEGER: case CONST_BOOLEAN: case CONST_CHAR:
			case CONST_STRING: case CONST_NULL: case CONST_NONE: case OPENING_PARENTHESIS: case IF: case FOR: case WHILE: case DO:
		{
			expr = parseDisjunctiveExpression_(parseConjunctiveExpression());
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol.token));
		}
		endLog();
		return expr;
	}
	private Expr parseDisjunctiveExpression_(Expr expr) {
		begLog("DisjunctiveExpression'");
		switch (laSymbol.token) {
		case WHERE: case END: case COMMA: case ASSIGN: case CLOSING_BRACKET: 
			case CLOSING_PARENTHESIS: case THEN: case ELSE:
			case COLON: case TYP: case FUN: case VAR: case EOF: case WHILE:
		{
			break;
		}
		case OR:
		{
			Symbol symbol = nextSymbol();
			Expr sndExpr = parseConjunctiveExpression();
			expr = new BinExpr(new Position(expr, sndExpr), Oper.OR, expr, sndExpr);
			expr = parseDisjunctiveExpression_(expr);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Disjunctive expression_"));
		}
		endLog();
		return expr;
	}
	private Expr parseConjunctiveExpression() {
		begLog("ConjunctiveExpression");
		Expr expr = null;
		switch (laSymbol.token) {
		case ADD: case SUB: case NOT: case MEM: case OPENING_BRACKET: case IDENTIFIER: case CONST_INTEGER: case CONST_BOOLEAN: case CONST_CHAR:
			case CONST_STRING: case CONST_NULL: case CONST_NONE: case OPENING_PARENTHESIS: case IF: case FOR: case WHILE: case DO:
		{
			expr = parseConjunctiveExpression_(parseRelationalExpression());
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Conjunctive expression"));
		}
		endLog();
		return expr;
	}
	private Expr parseConjunctiveExpression_(Expr expr) {
		begLog("DisjunctiveExpression'");
		switch (laSymbol.token) {
		case WHERE: case END: case COMMA: case ASSIGN: case OR: case CLOSING_BRACKET: 
			case CLOSING_PARENTHESIS: case THEN: case ELSE:
			case COLON: case TYP: case FUN: case VAR: case EOF: case WHILE:
		{
			break;
		}
		case AND:
		{
			Symbol symbol = nextSymbol();
			Expr sndExpr = parseRelationalExpression();
			expr = new BinExpr(new Position(expr, sndExpr), Oper.AND , expr, sndExpr);
			expr = parseConjunctiveExpression_(expr);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Conjunctive expression_"));
		}
		endLog();
		return expr;
	}
	private Expr parseRelationalExpression() {
		begLog("RelationalExpression");
		Expr expr = null;
		switch (laSymbol.token) {
		case ADD: case SUB: case NOT: case MEM: case OPENING_BRACKET: case IDENTIFIER: case CONST_INTEGER: case CONST_BOOLEAN: case CONST_CHAR:
			case CONST_STRING: case CONST_NULL: case CONST_NONE: case OPENING_PARENTHESIS: case IF: case FOR: case WHILE: case DO:
		{
			expr = parseRelationalExpression_(parseAdditiveExpression());
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Renational expression"));
		}
		endLog();
		return expr;
	}
	private Expr parseRelationalExpression_(Expr expr) {
		begLog("RelationalExpression'");
		switch (laSymbol.token) {
		case WHERE: case END: case COMMA: case ASSIGN: case OR: case AND: case CLOSING_BRACKET: 
			case CLOSING_PARENTHESIS: case THEN: case ELSE:
			case COLON: case TYP: case FUN: case VAR: case EOF: case WHILE:
		{
			break;
		}
		case EQU:
		{
			Symbol symbol = nextSymbol();
			Expr sndExpr = parseAdditiveExpression();
			expr = new BinExpr(new Position(expr, sndExpr), Oper.EQU , expr, sndExpr);
			break;
		}
		case NEQ:
		{
			Symbol symbol = nextSymbol();
			Expr sndExpr = parseAdditiveExpression();
			expr = new BinExpr(new Position(expr, sndExpr), Oper.NEQ , expr, sndExpr);
			break;
		}
		case LEQ:
		{
			Symbol symbol = nextSymbol();
			Expr sndExpr = parseAdditiveExpression();
			expr = new BinExpr(new Position(expr, sndExpr), Oper.LEQ , expr, sndExpr);
			break;
		}
		case LTH:
		{
			Symbol symbol = nextSymbol();
			Expr sndExpr = parseAdditiveExpression();
			expr = new BinExpr(new Position(expr, sndExpr), Oper.LTH , expr, sndExpr);
			break;
		}
		case GTH:
		{
			Symbol symbol = nextSymbol();
			Expr sndExpr = parseAdditiveExpression();
			expr = new BinExpr(new Position(expr, sndExpr), Oper.GTH , expr, sndExpr);
			break;
		}
		case GEQ:
		{
			Symbol symbol = nextSymbol();
			Expr sndExpr = parseAdditiveExpression();
			expr = new BinExpr(new Position(expr, sndExpr), Oper.GEQ , expr, sndExpr);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol + " " + laSymbol.token + " at parse Realtional expression_"));
		}
		endLog();
		return expr;
	}
	private Expr parseAdditiveExpression() {
		begLog("AdditiveExpression");
		Expr expr = null;
		switch (laSymbol.token) {
		case ADD: case SUB: case NOT: case MEM: case OPENING_BRACKET: case IDENTIFIER: case CONST_INTEGER: case CONST_BOOLEAN: case CONST_CHAR:
			case CONST_STRING: case CONST_NULL: case CONST_NONE: case OPENING_PARENTHESIS: case IF: case FOR: case WHILE: case DO:
		{
			expr = parseAdditiveExpression_(parseMultiplicativeExpression());
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol + " " + laSymbol.token + " at parse Additive expression"));
		}
		endLog();
		return expr;
	}
	private Expr parseAdditiveExpression_(Expr expr) {
		begLog("AdditiveExpression'");
		switch (laSymbol.token) {
		case WHERE: case END: case COMMA: case ASSIGN: case OR: case AND: 
			case EQU: case NEQ: case LTH: case GTH:  case LEQ:  case GEQ:
			case CLOSING_BRACKET: case CLOSING_PARENTHESIS: case THEN: case ELSE:
			case COLON: case TYP: case FUN: case VAR: case EOF: case WHILE:
		{
			break;
		}
		case ADD:
		{
			Symbol symbol = nextSymbol();
			Expr sndExpr = parseMultiplicativeExpression();
			expr = new BinExpr(new Position(expr, sndExpr), Oper.ADD , expr, sndExpr);
			expr = parseAdditiveExpression_(expr);
			break;
		}
		case SUB:
		{
			Symbol symbol = nextSymbol();
			Expr sndExpr = parseMultiplicativeExpression();
			expr = new BinExpr(new Position(expr, sndExpr), Oper.SUB , expr, sndExpr);
			expr = parseAdditiveExpression_(expr);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol + " " + laSymbol.token + " at parse Additive expression_"));
		}
		endLog();
		return expr;
	}

	private Expr parseMultiplicativeExpression() {
		begLog("MultiplicativeExpression");
		Expr expr = null;
		switch (laSymbol.token) {
		case ADD: case SUB: case NOT: case MEM: case OPENING_BRACKET: case IDENTIFIER: case CONST_INTEGER: case CONST_BOOLEAN: case CONST_CHAR:
			case CONST_STRING: case CONST_NULL: case CONST_NONE: case OPENING_PARENTHESIS: case IF: case FOR: case WHILE: case DO:
		{
			expr = parseMultiplicativeExpression_(parsePrefixExpression());
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Multiplicative expression"));
		}
		endLog();
		return expr;
	}
	private Expr parseMultiplicativeExpression_(Expr expr) {
		begLog("MultiplicativeExpression'");
		switch (laSymbol.token) {
		case WHERE: case END: case COMMA: case ASSIGN: case OR: case AND: 
			case EQU: case NEQ: case LTH: case GTH:  case LEQ:  case GEQ:
			case ADD: case SUB:
			case CLOSING_BRACKET: case CLOSING_PARENTHESIS: case THEN: case ELSE:
			case COLON: case TYP: case FUN: case VAR: case EOF: case WHILE:
		{
			break;
		}
		case MUL:
		{	
			Symbol symbol = nextSymbol();
			Expr sndExpr = parsePrefixExpression();
			Expr e = new BinExpr(new Position(expr, sndExpr), Oper.MUL , expr, sndExpr);
			expr = parseMultiplicativeExpression_(e);
			break;
		}
		case DIV:
		{	
			Symbol symbol = nextSymbol();
			Expr sndExpr = parsePrefixExpression();
			Expr e = new BinExpr(new Position(expr, sndExpr), Oper.DIV , expr, sndExpr);
			expr = parseMultiplicativeExpression_(e);
			break;
		}
		case MOD:
		{	
			Symbol symbol = nextSymbol();
			Expr sndExpr = parsePrefixExpression();
			Expr e = new BinExpr(new Position(expr, sndExpr), Oper.MOD , expr, sndExpr);
			expr = parseMultiplicativeExpression_(e);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Multiplicative expression_"));
		}
		endLog();
		return expr;
	}

	private Expr parsePrefixExpression() {
		begLog("PrefixExpression");
		Expr expr = null;
		switch (laSymbol.token) {
		case ADD:
		{
			Symbol symbol = nextSymbol();
			Expr subExpr = parsePrefixExpression();
			expr = new UnExpr(new Position(symbol, subExpr), UnExpr.Oper.ADD, subExpr);
			break;
		}
		case SUB:
		{
			Symbol symbol = nextSymbol();
			Expr subExpr = parsePrefixExpression();
			expr = new UnExpr(new Position(symbol, subExpr), UnExpr.Oper.SUB, subExpr);
			break;
		}
		case MEM: 
		{
			Symbol symbol = nextSymbol();
			Expr subExpr = parsePrefixExpression();
			expr = new UnExpr(new Position(symbol, subExpr), UnExpr.Oper.MEM, subExpr);
			break;
		}
		case NOT:
		{
			Symbol symbol = nextSymbol();
			Expr subExpr = parsePrefixExpression();
			expr = new UnExpr(new Position(symbol, subExpr), UnExpr.Oper.NOT, subExpr);
			break;
		}
		case OPENING_BRACKET:
		{
			Symbol symbol = nextSymbol();
			Symbol symId;
			
			Type type = parseType();
			
			if (laSymbol.token == Symbol.Token.CLOSING_BRACKET) {
				symId = nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol ']' inserted.");
				throw(new CompilerError("Unrecognisable symbol"));
			}
			expr = new CastExpr(new Position(symbol, laSymbol), type, parsePrefixExpression());
			break;
		}
		case IDENTIFIER: case CONST_INTEGER: case CONST_BOOLEAN: case CONST_CHAR:
			case CONST_STRING: case CONST_NULL: case CONST_NONE: case OPENING_PARENTHESIS: case IF: case FOR: case WHILE: case DO:
		{
			expr = parsePostfixExpression();
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Prefix expression"));
		}
		endLog();
		return expr;
	}
	private Expr parsePostfixExpression() {
		begLog("PostfixExpression");
		Expr expr = null;
		switch (laSymbol.token) {
		case IDENTIFIER: case CONST_INTEGER: case CONST_BOOLEAN: case CONST_CHAR:
			case CONST_STRING: case CONST_NULL: case CONST_NONE: case OPENING_PARENTHESIS: case IF: case FOR: case WHILE: case DO:
		{
			expr = parsePostfixExpression_(parseAtomicExpression());
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Postfix expression"));
		}
		endLog();
		return expr;
	}
	private Expr parsePostfixExpression_(Expr expr) {
		begLog("PostfixExpression'");
		switch (laSymbol.token) {
		case WHERE: case END: case COMMA: case ASSIGN: case OR: case AND: 
			case EQU: case NEQ: case LTH: case GTH:  case LEQ:  case GEQ:
			case ADD: case SUB: case MUL: case DIV: case MOD:
			case CLOSING_BRACKET: case CLOSING_PARENTHESIS: case THEN: case ELSE:
			case COLON: case TYP: case FUN: case VAR: case EOF: case WHILE:
		{
			break;
		}
		case OPENING_BRACKET:
		{
			nextSymbol();
			
			Expr e = parseExpression();
			
			if (laSymbol.token == Symbol.Token.CLOSING_BRACKET) {
				nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol ']' inserted.");
				throw(new CompilerError("Unrecognisable symbol"));
			}
			expr = new BinExpr(new Position(expr, laSymbol), Oper.ARR, expr, e);
			expr = parsePostfixExpression_(expr);
			break;
		}
		case DOT:
		{
			nextSymbol();
			Symbol symId;
			
			if (laSymbol.token == Symbol.Token.IDENTIFIER) {
				symId = nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing identifier inserted.");
				throw(new CompilerError("Unrecognisable symbol"));
			}
			CompName compName = new CompName(symId, symId.lexeme);
			expr = new BinExpr(new Position(expr, laSymbol), Oper.REC, expr, compName);
			expr = parsePostfixExpression_(expr);
			break;
		}
		case VAL:
		{
			Symbol symbol = nextSymbol();
			expr = new UnExpr(symbol, UnExpr.Oper.VAL, expr);
			expr = parsePostfixExpression_(expr);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Postfix Expression_"));
		}
		endLog();
		return expr;
	}
	private Expr parseAtomicExpression() {
		begLog("AtomicExpression");
		Expr expr = null;
		switch (laSymbol.token) {
		case IDENTIFIER:
		{
			Symbol symbol = nextSymbol();
			expr = parseArgumentsOpt(symbol);
			break;
		}
		case CONST_INTEGER:
		{
			Symbol symbol = nextSymbol();
			expr = new AtomExpr(symbol, AtomTypes.INTEGER, symbol.lexeme);
			break;
		}
		case CONST_BOOLEAN:
		{
			Symbol symbol = nextSymbol();
			expr = new AtomExpr(symbol, AtomTypes.BOOLEAN, symbol.lexeme);
			break;
		}
		case CONST_CHAR:
		{
			Symbol symbol = nextSymbol();
			expr = new AtomExpr(symbol, AtomTypes.CHAR, symbol.lexeme);
			break;
		}
		case CONST_STRING: 
		{
			Symbol symbol = nextSymbol();
			expr = new AtomExpr(symbol, AtomTypes.STRING, symbol.lexeme);
			break;
		}
		case CONST_NULL:
		{
			Symbol symbol = nextSymbol();
			expr = new AtomExpr(symbol, AtomTypes.PTR, symbol.lexeme);
			break;
		}
		case CONST_NONE:
		{
			Symbol symbol = nextSymbol();
			expr = new AtomExpr(symbol, AtomTypes.VOID, symbol.lexeme);
			break;
		}
		case OPENING_PARENTHESIS:
		{
			expr = new Exprs(nextSymbol(), parseExpressions());
			
			if (laSymbol.token == Symbol.Token.CLOSING_PARENTHESIS) {
				nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol ')' inserted.");
				throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token));
			}
			break;
		}
		case IF:
		{
			Symbol symbol = nextSymbol();
			Symbol symId;
			
			Expr cond = parseExpression();
			
			if (laSymbol.token == Symbol.Token.THEN) {
				symId = nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing THEN inserted.");
				throw(new CompilerError("Unrecognisable symbol"));
			}
			Expr thenExpr = parseExpression();
			
			if (laSymbol.token == Symbol.Token.ELSE) {
				symId = nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing ELSE inserted.");
				throw(new CompilerError("Unrecognisable symbol"));
			}
			Expr elseExpr = parseExpression();
			
			if (laSymbol.token == Symbol.Token.END) {
				symId = nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing END inserted.");
				throw(new CompilerError("Unrecognisable symbol"));
			}
			expr = new IfExpr(new Position(symbol, laSymbol), cond, thenExpr, elseExpr);
			break;
		}
		case FOR: 
		{
			Symbol name;
			Symbol symbol = nextSymbol();
			
			if (laSymbol.token == Symbol.Token.IDENTIFIER) {
				name = nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing IDENTIFIER inserted.");
				throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token));
			}
			VarName varName = new VarName(name, name.lexeme);
			
			if (laSymbol.token == Symbol.Token.ASSIGN) {
				nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol '=' inserted.");
				throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token));
			}
			Expr loBound = parseExpression();
			
			if (laSymbol.token == Symbol.Token.COMMA) {
				nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol '.' inserted.");
				throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token));
			}
			Expr hiBound = parseExpression();
			
			if (laSymbol.token == Symbol.Token.COLON) {
				nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol ':' inserted.");
				throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token));
			}
			Expr body = parseExpression();
			
			if (laSymbol.token == Symbol.Token.END) {
				nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing END inserted.");
				throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token));
			}
			expr = new ForExpr(new Position(symbol, laSymbol), varName, loBound, hiBound, body);
			break;
		}
		case DO:
		{
			Symbol symbol = nextSymbol();
			Symbol symId;
			
			Expr body = parseExpression();
			
			if (laSymbol.token == Symbol.Token.WHILE) {
				symId = nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol 'while' inserted.");
				throw(new CompilerError("Unrecognisable symbol"));
			}
			Expr cond =  parseExpression();
			
			if (laSymbol.token == Symbol.Token.END) {
				symId = nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing END inserted.");
				throw(new CompilerError("Unrecognisable symbol"));
			}
			expr = new WhileExpr(new Position(symbol, laSymbol), cond, body);
			break;
		}
		case WHILE:
		{
			Symbol symbol = nextSymbol();
			Symbol symId;
			
			Expr cond = parseExpression();
			
			if (laSymbol.token == Symbol.Token.COLON) {
				symId = nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol ':' inserted.");
				throw(new CompilerError("Unrecognisable symbol"));
			}
			Expr body =  parseExpression();
			
			if (laSymbol.token == Symbol.Token.END) {
				symId = nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing END inserted.");
				throw(new CompilerError("Unrecognisable symbol"));
			}
			expr = new WhileExpr(new Position(symbol, laSymbol), cond, body);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Atomic Expression"));
		}
		endLog();
		return expr;
	}
	private Expr parseArgumentsOpt(Symbol symbol2) {
		begLog("ArgumentsOpt");
		Expr expr;
		switch (laSymbol.token) {
		case WHERE: case END: case COMMA: case ASSIGN: case OR: case AND: 
		case EQU: case NEQ: case LTH: case GTH:  case LEQ:  case GEQ:
		case ADD: case SUB: case MUL: case DIV: case MOD: case OPENING_BRACKET:
		case CLOSING_BRACKET: case DOT: case VAL: case CLOSING_PARENTHESIS: case THEN: case ELSE:
		case COLON: case TYP: case FUN: case VAR: case EOF:
		{
			expr = new VarName(symbol2, symbol2.lexeme);
			break;
		}
		case OPENING_PARENTHESIS:
		{
			Symbol symbol = nextSymbol();
			expr = parseArgumentsOpt_(symbol2);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Arguments Opt"));
		}
		endLog();
		return expr;
	}
	
	private Expr parseArgumentsOpt_(Symbol symbol2) {
		begLog("ArgumentsOpt'");
		Expr expr;
		switch (laSymbol.token) {
		case ADD:
		case SUB:
		case NOT:
		case MEM:
		case OPENING_BRACKET:
		case IDENTIFIER:
		case CONST_INTEGER:
		case CONST_BOOLEAN:
		case CONST_CHAR:
		case CONST_STRING:
		case CONST_NULL:
		case CONST_NONE:
		case OPENING_PARENTHESIS:
		case IF:
		case FOR:
		case WHILE:
		case DO:
		{
			LinkedList<Expr> args = parseExpressions();
	
			if (laSymbol.token == Symbol.Token.CLOSING_PARENTHESIS) {
				nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol ')' inserted.");
				throw(new CompilerError("Unrecognisable symbol"));
			}
			expr = new FunCall(new Position(symbol2, laSymbol), symbol2.lexeme, args);
			break;
		}
		case CLOSING_PARENTHESIS:
		{
			LinkedList<Expr> args = new LinkedList<Expr>();
			if (laSymbol.token == Symbol.Token.CLOSING_PARENTHESIS) {
				nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol ')' inserted.");
				throw(new CompilerError("Unrecognisable symbol"));
			}
			expr = new FunCall(new Position(symbol2, laSymbol), symbol2.lexeme, args);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Arguments Opt"));
		}
		endLog();
		return expr;
	}

	private LinkedList<Decl> parseDeclarations() {
		begLog("Declarations");
		LinkedList<Decl> decl = new LinkedList<Decl>();
		switch (laSymbol.token) {
		case TYP: case FUN: case VAR: 
		{
			decl.add(parseDeclaration());
			parseDeclarations_(decl);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Declarations"));
		}
		endLog();
		return decl;
	}

	private LinkedList<Decl> parseDeclarations_(LinkedList<Decl> decl) {
		begLog("Declarations'");
		switch (laSymbol.token) {
		case END: {
			break;
		}
		case TYP: case FUN: case VAR: 
		{
			decl.add(parseDeclaration());
			parseDeclarations_(decl);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Declarations_"));
		}
		endLog();	
		return decl;
	}

	private Decl parseDeclaration() {
		begLog("Declaration");
		Decl decl = null;
		switch (laSymbol.token) {
		case TYP: 
		{
			decl = parseTypeDeclaration();
			break;
		}
		case FUN:
		{
			decl = parseFunctionDeclaration();
			break;
		}
		case VAR: 
		{
			decl = parseVariableDeclaration();
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Declaration"));
		}
		endLog();
		return decl;
	}
	
	private TypeDecl parseTypeDeclaration() {
		begLog("TypeDeclaration");
		TypeDecl typeDecl = null;
		switch (laSymbol.token) {
		case TYP: 
		{
			Symbol symbol = nextSymbol();
			Symbol name;
			if (laSymbol.token == Symbol.Token.IDENTIFIER) {
				name = nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing identifier inserted.");
				throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token));
			}
			if (laSymbol.token == Symbol.Token.COLON) {
				nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol ':' inserted.");
				throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token));
			}
			Type type = parseType();
			typeDecl= new TypeDecl(new Position(symbol, type), name.lexeme, type);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Type Declaration"));
		}
		endLog();
		return typeDecl;
	}
	
	private FunDecl parseFunctionDeclaration() {
		begLog("FunctionDeclaration");
		FunDecl funDecl = null;
		switch (laSymbol.token) {
		case FUN: 
		{
			Symbol symbol = nextSymbol();
			Symbol name;
			if (laSymbol.token == Symbol.Token.IDENTIFIER) {
				name = nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing identifier inserted.");
				throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token));
			}
			if (laSymbol.token == Symbol.Token.OPENING_PARENTHESIS) {
				nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol '(' inserted.");
				throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token));
			}
			LinkedList<ParDecl> pars = parseParametersOpt();
			if (laSymbol.token == Symbol.Token.CLOSING_PARENTHESIS) {
				nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol ')' inserted.");
				throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token));
			}
			if (laSymbol.token == Symbol.Token.COLON) {
				nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol ':' inserted.");
				throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token));
			}
			Type type = parseType();
			funDecl = parseFunctionBodyOpt(symbol, name, pars, type);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Function Declaration"));
		}
		endLog();
		return funDecl;
	}

	private LinkedList<ParDecl> parseParametersOpt() {
		begLog("ParametersOpt");
		LinkedList<ParDecl> parDecl = new LinkedList<ParDecl>();
		switch (laSymbol.token) {
		case IDENTIFIER: 
		{
			parDecl = parseParameters();
			break;
		}
		case CLOSING_PARENTHESIS:
		{
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Parameters Opt"));
		}
		endLog();
		return parDecl;
	}

	private LinkedList<ParDecl> parseParameters() {
		begLog("Parameters");
		LinkedList<ParDecl> parDecl = new LinkedList<ParDecl>();
		switch (laSymbol.token) {
		case IDENTIFIER: 
		{
			parDecl.add(parseParameter());
			parseParameters_(parDecl);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Parameters"));
		}
		endLog();
		return parDecl;
	}

	private void parseParameters_(LinkedList<ParDecl> parDecl) {
		begLog("Parameters'");
		switch (laSymbol.token) {
		case COMMA: 
		{
			Symbol symbol = nextSymbol();
			parDecl.add(parseParameter());
			parseParameters_(parDecl);
			break;
		}
		case CLOSING_PARENTHESIS:
		{
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Parameters_"));
		}
		endLog();
	}

	private ParDecl parseParameter() {
		begLog("Parameter");
		ParDecl parDecl = null;
		switch (laSymbol.token) {
		case IDENTIFIER: 
		{
			Symbol symbol = nextSymbol();
			Symbol symId;
			if (laSymbol.token == Symbol.Token.COLON) {
				nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol ':' inserted.");
				throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token));
			}
			Type type = parseType();
			parDecl = new ParDecl(new Position(symbol, type), symbol.lexeme, type);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Parameter"));
		}
		endLog();
		return parDecl;
	}
	
	private FunDecl parseFunctionBodyOpt(Symbol symb, Symbol name, LinkedList<ParDecl> pars, Type type) {
		begLog("FunctionBodyOpt");
		FunDecl funDecl = null;
		switch (laSymbol.token) {
		case END: case TYP: case FUN: case VAR:
		{
			funDecl = new FunDecl(new Position(symb, type), name.lexeme, pars, type);
			break;
		}
		case ASSIGN: 
		{
			Symbol symbol = nextSymbol();
			Expr body = parseExpression();
			funDecl = new FunDef(new Position(symb, type), name.lexeme, pars, type, body);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " at parse Function Body Opt"));
		}
		endLog();
		return funDecl;
	}

	private VarDecl parseVariableDeclaration() {
		begLog("VariableDeclaration");
		VarDecl varDecl;
		switch (laSymbol.token) {
		case VAR: {
			Symbol symVar = nextSymbol();
			Symbol symName;
			if (laSymbol.token == Symbol.Token.IDENTIFIER) {
				symName = nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing identifier inserted.");
				throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token));
			}
			if (laSymbol.token == Symbol.Token.COLON) {
				nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol ':' inserted.");
				throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token));
			}
			Type type = parseType();
			varDecl = new VarDecl(new Position(symVar, type), symName.lexeme, type);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse Variable Declaration"));
		}
		endLog();
		return varDecl;
	}

	private Type parseType() {
		begLog("Type");
		Type type = null;
		switch (laSymbol.token) {
		case IDENTIFIER: 
		{
			Symbol symbol = nextSymbol();
			type = new TypeName(symbol, symbol.lexeme);
			break;
		}
		case INTEGER: 
		{
			type = new AtomType(nextSymbol(), AtomType.AtomTypes.INTEGER);
			break;
		}
		case BOOLEAN: 
		{
			type = new AtomType(nextSymbol(), AtomType.AtomTypes.BOOLEAN);
			break;
		}
		case CHAR:
		{
			type = new AtomType(nextSymbol(), AtomType.AtomTypes.CHAR);
			break;
		}
		case STRING:
		{
			type = new AtomType(nextSymbol(), AtomType.AtomTypes.STRING);
			break;
		}
		case VOID:
		{
			type = new AtomType(nextSymbol(), AtomType.AtomTypes.VOID);
			break;
		}
		case ARR:
		{
			Symbol symbol = nextSymbol();
			Symbol symId;
			if (laSymbol.token == Symbol.Token.OPENING_BRACKET) {
				symId = nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol '[' inserted.");
				throw(new CompilerError("Unrecognisable symbol"));
			}
			
			Expr expr = parseExpression();
			
			if (laSymbol.token == Symbol.Token.CLOSING_BRACKET) {
				symId = nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol ']' inserted.");
				throw(new CompilerError("Unrecognisable symbol"));
			}
			type = new ArrType(new Position(symbol,laSymbol), expr, parseType());
			break;
		}
		case REC:
		{
			Symbol symbol = nextSymbol();
			Symbol symId;
			if (laSymbol.token == Symbol.Token.OPENING_BRACE) {
				nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol '{' inserted.");
				throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token));
			}
			
			LinkedList<CompDecl> compDecl = parseComponents();
			
			if (laSymbol.token == Symbol.Token.CLOSING_BRACE) {
				symId = nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol '}' inserted.");
				throw(new CompilerError("Unrecognisable symbol"));
			}
			type = new RecType(new Position(symbol, laSymbol), compDecl);
			break;
		}
		case PTR:
		{
			Symbol symbol = nextSymbol();
			Type t = parseType();
			type = new PtrType(new Position(symbol,t),t);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse type"));
		}
		endLog();
		return type;
	}

	private LinkedList<CompDecl> parseComponents() {
		begLog("Components");
		LinkedList<CompDecl> compDecl = new LinkedList<CompDecl>();
		switch (laSymbol.token) {
		case IDENTIFIER: 
		{
			compDecl.add(parseComponent());
			parseComponents_(compDecl);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse components"));
		}
		endLog();
		return compDecl;
	}

	private void parseComponents_(LinkedList<CompDecl> compDecl) {
		begLog("Components'");
		switch (laSymbol.token) {
		case COMMA: 
		{
			Symbol symbol = nextSymbol();
			compDecl.add(parseComponent());
			parseComponents_(compDecl);
			break;
		}
		case CLOSING_BRACE:
		{
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse components_"));
		}
		endLog();
	}

	private CompDecl parseComponent() {
		begLog("Component");
		CompDecl compDecl = null;
		switch (laSymbol.token) {
		case IDENTIFIER: 
		{
			Symbol symbol = nextSymbol();
			Symbol symId;
			if (laSymbol.token == Symbol.Token.COLON) {
				nextSymbol();
			} else {
				Report.warning(laSymbol, "Missing symbol ':' inserted.");
				throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token));
			}
			Type type = parseType();
			compDecl = new CompDecl(new Position(symbol,type), symbol.lexeme, type);
			break;
		}
		default:
			throw(new CompilerError("Unrecognisable symbol: "+ laSymbol + " " + laSymbol.token + " at parse component"));
		}
		endLog();
		return compDecl;
	}
}