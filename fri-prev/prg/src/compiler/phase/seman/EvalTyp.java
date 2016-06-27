package compiler.phase.seman;

import java.util.LinkedList;
import java.util.Stack;

import compiler.common.report.*;
import compiler.data.ast.*;
import compiler.data.ast.attr.*;
import compiler.data.ast.code.*;
import compiler.data.typ.*;

/**
 * Type checker.
 * 
 * <p>
 * Type checker checks type of all sentential forms of the program and resolves
 * the component names as this cannot be done earlier, i.e., in
 * {@link compiler.phase.seman.EvalDecl}.
 * </p>
 * 
 * @author juliette
 */
public class EvalTyp extends FullVisitor {

	private final Attributes attrs;
	
	private int prelet;
	
	private Stack<String> namespace;
	
	public EvalTyp(Attributes attrs) {
		this.attrs = attrs;
	}
	
	/** The symbol table. */
	private SymbolTable symbolTable = new SymbolTable();
	
	public void visit(ArrType arrType) {
		arrType.size.accept(this);
		arrType.elemType.accept(this);
		
		Long n = attrs.valueAttr.get(arrType.size);
		Typ type = attrs.typAttr.get(arrType.elemType);
		
		if(n == null)
			throw(new CompilerError("Array size not an integer at " + arrType.size));
		if(n <= 0)
			throw(new CompilerError("Illegal array size at " + arrType.size));
		if(type != null)
			attrs.typAttr.set(arrType, new ArrTyp(n, type));
		else
			throw(new CompilerError("Illegal array type at " + arrType));
	}
	
	public void visit(AtomExpr atomExpr) {
		switch (atomExpr.type) {
		case INTEGER:
		{
			attrs.typAttr.set(atomExpr, new IntegerTyp());
			break;
		}
		case BOOLEAN:
		{
			attrs.typAttr.set(atomExpr, new BooleanTyp());
			break;
		}
		case CHAR:
		{
			attrs.typAttr.set(atomExpr, new CharTyp());
			break;
		}
		case STRING:
		{
			attrs.typAttr.set(atomExpr, new StringTyp());
			break;
		}
		case PTR:
		{
			attrs.typAttr.set(atomExpr, new PtrTyp(new VoidTyp()));
			break;
		}
		case VOID:
		{
			attrs.typAttr.set(atomExpr, new VoidTyp());
			break;
		}
		default:
			throw(new CompilerError("Wrong type " + atomExpr.type + " at " + atomExpr));
		}
	}

	public void visit(AtomType atomType) {
		switch (atomType.type) {
		case INTEGER:
		{
			attrs.typAttr.set(atomType, new IntegerTyp());
			break;
		}
		case BOOLEAN:
		{
			attrs.typAttr.set(atomType, new BooleanTyp());
			break;
		}
		case CHAR:
		{
			attrs.typAttr.set(atomType, new CharTyp());
			break;
		}
		case STRING:
		{
			attrs.typAttr.set(atomType, new StringTyp());
			break;
		}
		case VOID:
		{
			attrs.typAttr.set(atomType, new VoidTyp());
			break;
		}
		default:
			throw(new CompilerError("Wrong type " + atomType.type + " at " + atomType));
		}
	}

	public void visit(BinExpr binExpr) {
		binExpr.fstExpr.accept(this);
		
		if(binExpr.oper == binExpr.oper.REC)
		{
			RecTyp rec = (RecTyp)(attrs.typAttr.get(binExpr.fstExpr)).actualTyp();
			String name = rec.nameSpace;
			namespace.push(name);
			symbolTable.enterNamespace(name);
		}
		Typ fstExpr = attrs.typAttr.get(binExpr.fstExpr);
		
		binExpr.sndExpr.accept(this);
		
		Typ sndExpr = attrs.typAttr.get(binExpr.sndExpr);
		
		switch (binExpr.oper) {
		case ADD:
		case SUB:
		case MUL:
		case DIV:
		case MOD:
		{
			if(fstExpr.actualTyp() instanceof IntegerTyp && sndExpr.actualTyp() instanceof IntegerTyp)
				attrs.typAttr.set(binExpr, new IntegerTyp());
			else
				throw(new CompilerError("Semantic error at " + binExpr + " " + binExpr.oper));
			break;
		}
		case AND:
		case OR:
		{
			if(fstExpr instanceof BooleanTyp && sndExpr instanceof BooleanTyp)
				attrs.typAttr.set(binExpr, new BooleanTyp());
			else
				throw(new CompilerError("Semantic error at " + binExpr + " " + binExpr.oper));
			break;
		}
		case EQU:
		case NEQ:
		case LTH:
		case GTH:
		case LEQ:
		case GEQ:
		{
			if(fstExpr instanceof IntegerTyp && sndExpr instanceof IntegerTyp)
				attrs.typAttr.set(binExpr, new BooleanTyp());
			else if(fstExpr instanceof BooleanTyp && sndExpr instanceof BooleanTyp)
				attrs.typAttr.set(binExpr, new BooleanTyp());
			else if(fstExpr instanceof CharTyp && sndExpr instanceof CharTyp)
				attrs.typAttr.set(binExpr, new BooleanTyp());
			else if(fstExpr instanceof PtrTyp && sndExpr instanceof PtrTyp)
				attrs.typAttr.set(binExpr, new BooleanTyp());
			else
				throw(new CompilerError("Semantic error at " + binExpr + " " + binExpr.oper + " fstExpr " + fstExpr + " sndExpr " + sndExpr));
			break;
		}	
		case ASSIGN:
		{
			if(fstExpr instanceof IntegerTyp && sndExpr instanceof IntegerTyp)
				attrs.typAttr.set(binExpr, new VoidTyp());
			else if(fstExpr instanceof BooleanTyp && sndExpr instanceof BooleanTyp)
				attrs.typAttr.set(binExpr, new VoidTyp());
			else if(fstExpr instanceof CharTyp && sndExpr instanceof CharTyp)
				attrs.typAttr.set(binExpr, new VoidTyp());
			else if(fstExpr instanceof StringTyp && sndExpr instanceof StringTyp)
				attrs.typAttr.set(binExpr, new VoidTyp());
			else if(fstExpr instanceof PtrTyp && sndExpr instanceof PtrTyp)
				attrs.typAttr.set(binExpr, new VoidTyp());
			else
				throw(new CompilerError("Semantic error at " + binExpr + " " + binExpr.oper));
			break;
		}
		case ARR:
		{
			ArrTyp arr = (ArrTyp)fstExpr;
				
			if(!(sndExpr instanceof IntegerTyp))
				throw(new CompilerError("Error at " + binExpr.sndExpr + ". Array index not an integer."));
			
			if(arr != null)
				attrs.typAttr.set(binExpr, arr.elemTyp);
			else
				throw(new CompilerError("Error at " + binExpr.fstExpr + " Wrong array's type."));
			break;
		}
		case REC:
		{
			if(fstExpr.actualTyp() instanceof RecTyp)
			{
				attrs.typAttr.set(binExpr, sndExpr);
				
				symbolTable.leaveNamespace();
				namespace.pop();
			}
			else
				throw(new CompilerError("Error at " + binExpr + " " + fstExpr.actualTyp() + " is not RecTyp."));
			break;
		}
		default:
			break;
		}
	}

	public void visit(CastExpr castExpr) {
		castExpr.type.accept(this);
		castExpr.expr.accept(this);
	
		Typ type = attrs.typAttr.get(castExpr.type);
		Typ expr = attrs.typAttr.get(castExpr.expr);
		
		if (type == null)
			throw new CompilerError("Error with cast type "+castExpr.type);
		if (expr == null)
			throw new CompilerError("Error with cast expression "+castExpr.expr);
		
		if (type instanceof PtrTyp && expr.isStructEquivTo(new PtrTyp(new VoidTyp())))
			attrs.typAttr.set(castExpr, type);
		else 
			throw new CompilerError("Cast error " + castExpr);

	}

	public void visit(CompDecl compDecl) {
		String name = symbolTable.newNamespace(compDecl.name);
		namespace.push(name);
		symbolTable.enterNamespace(name);

		compDecl.type.accept(this);
		if(attrs.typAttr.get(compDecl.type) != null)
			attrs.typAttr.set(compDecl, attrs.typAttr.get(compDecl.type));
		else
			throw new CompilerError("Component error " + compDecl);
		
		symbolTable.leaveNamespace();
		namespace.pop();
	}

	public void visit(CompName compName) {
		try {
			Decl decl = symbolTable.fndDecl(namespace.peek(), compName.name());
			if(decl != null)
				attrs.declAttr.set(compName, decl);
			else
				throw new CompilerError("Component name not undeclared " + compName);
			
			attrs.typAttr.set(compName, attrs.typAttr.get(attrs.declAttr.get(compName)));
		} catch (CannotFndNameDecl e) {
			throw new CompilerError("Component name not found " + compName);
		}
	}
	

	public void visit(Exprs exprs) {
		for (int e = 0; e < exprs.numExprs(); e++)
		{
			exprs.expr(e).accept(this);
			
			if(attrs.typAttr.get(exprs.expr(e)) == null)
				throw(new CompilerError("Expression type can not be null " + exprs.expr(e)));
		}
		attrs.typAttr.set(exprs, attrs.typAttr.get(exprs.expr(exprs.numExprs()-1)));
	}

	public void visit(ForExpr forExpr) {
		forExpr.var.accept(this);
		forExpr.loBound.accept(this);
		forExpr.hiBound.accept(this);
		forExpr.body.accept(this);
		
		Typ varName = attrs.typAttr.get(forExpr.var);
		
		if(!(varName instanceof IntegerTyp))
			throw(new CompilerError("Variable is not integer " + forExpr.var));
		
		Typ loExpr = attrs.typAttr.get(forExpr.loBound);
		
		if(!(loExpr instanceof IntegerTyp))
			throw(new CompilerError("Low boundary is not integer " + forExpr.loBound));
		
		Typ hiExpr = attrs.typAttr.get(forExpr.hiBound);
		
		if(!(hiExpr instanceof IntegerTyp))
			throw(new CompilerError("High boundary is not integer " + forExpr.hiBound));
		
		Typ body = attrs.typAttr.get(forExpr.body);
		
		if(body == null && !(body instanceof VoidTyp))
			throw(new CompilerError("Body can not be null and must be void type " + forExpr.body));
		
		attrs.typAttr.set(forExpr, new VoidTyp());
	}

	public void visit(FunCall funCall) {
		for (int a = 0; a < funCall.numArgs(); a++)
			funCall.arg(a).accept(this);

		FunTyp type = (FunTyp)attrs.typAttr.get(attrs.declAttr.get(funCall));
		
		attrs.typAttr.set(funCall, type.resultTyp);
	}

	public void visit(FunDecl funDecl) {
		if(prelet == 1)
			funDecl.type.accept(this);
		if(prelet == 2)
		{
			LinkedList<Typ> parTyps = new LinkedList<Typ>();
			
			IntegerTyp integer = new IntegerTyp();
			BooleanTyp bool = new BooleanTyp();
			CharTyp ch = new CharTyp();
			StringTyp string = new StringTyp();
			VoidTyp Void = new VoidTyp();
			
			PtrTyp pointInt = new PtrTyp(new IntegerTyp());
			PtrTyp pointBool = new PtrTyp(new BooleanTyp());
			PtrTyp pointChar = new PtrTyp(new CharTyp());
			PtrTyp pointString = new PtrTyp(new StringTyp());
			PtrTyp pointVoid = new PtrTyp(new VoidTyp());
			
			
			for (int p = 0; p < funDecl.numPars(); p++)
			{
				funDecl.par(p).accept(this);
				
				Typ type = attrs.typAttr.get(funDecl.par(p));
				
				if(integer.isStructEquivTo(type))
					parTyps.add(type);
				else if(bool.isStructEquivTo(type))
					parTyps.add(type);
				else if(ch.isStructEquivTo(type))
					parTyps.add(type);
				else if(string.isStructEquivTo(type))
					parTyps.add(type);
				else if(pointInt.isStructEquivTo(type))
					parTyps.add(type);
				else if(pointBool.isStructEquivTo(type))
					parTyps.add(type);
				else if(pointChar.isStructEquivTo(type))
					parTyps.add(type);
				else if(pointString.isStructEquivTo(type))
					parTyps.add(type);
				else if(pointVoid.isStructEquivTo(type))
					parTyps.add(type);
				else
					throw(new CompilerError("Unrecognisable type of parameter " + funDecl.par(p)));
			}
			
			Typ type = attrs.typAttr.get(funDecl.type);
	
			if(integer.isStructEquivTo(type))
				attrs.typAttr.set(funDecl, new FunTyp(parTyps, type));
			else if(bool.isStructEquivTo(type))
				attrs.typAttr.set(funDecl, new FunTyp(parTyps, type));
			else if(ch.isStructEquivTo(type))
				attrs.typAttr.set(funDecl, new FunTyp(parTyps, type));
			else if(string.isStructEquivTo(type))
				attrs.typAttr.set(funDecl, new FunTyp(parTyps, type));
			else if(Void.isStructEquivTo(type))
				attrs.typAttr.set(funDecl, new FunTyp(parTyps, type));
			else if(pointInt.isStructEquivTo(type))
				attrs.typAttr.set(funDecl, new FunTyp(parTyps, type));
			else if(pointBool.isStructEquivTo(type))
				attrs.typAttr.set(funDecl, new FunTyp(parTyps, type));
			else if(pointChar.isStructEquivTo(type))
				attrs.typAttr.set(funDecl, new FunTyp(parTyps, type));
			else if(pointString.isStructEquivTo(type))
				attrs.typAttr.set(funDecl, new FunTyp(parTyps, type));
			else if(pointVoid.isStructEquivTo(type))
				attrs.typAttr.set(funDecl, new FunTyp(parTyps, type));
			else
				throw(new CompilerError("Function type undeclared " + funDecl));
		}	
	}

	public void visit(FunDef funDef) {
		if(prelet == 1)
		{
			funDef.type.accept(this);
		
			LinkedList<Typ> parTyps = new LinkedList<Typ>();
			
			IntegerTyp integer = new IntegerTyp();
			BooleanTyp bool = new BooleanTyp();
			CharTyp ch = new CharTyp();
			StringTyp string = new StringTyp();
			VoidTyp Void = new VoidTyp();
			
			PtrTyp pointInt = new PtrTyp(new IntegerTyp());
			PtrTyp pointBool = new PtrTyp(new BooleanTyp());
			PtrTyp pointChar = new PtrTyp(new CharTyp());
			PtrTyp pointString = new PtrTyp(new StringTyp());
			PtrTyp pointVoid = new PtrTyp(new VoidTyp());
			
			for (int p = 0; p < funDef.numPars(); p++)
			{
				funDef.par(p).accept(this);
				
				Typ type = attrs.typAttr.get(funDef.par(p));
				
				if(integer.isStructEquivTo(type))
					parTyps.add(type);
				else if(bool.isStructEquivTo(type))
					parTyps.add(type);
				else if(ch.isStructEquivTo(type))
					parTyps.add(type);
				else if(string.isStructEquivTo(type))
					parTyps.add(type);
				else if(pointInt.isStructEquivTo(type))
					parTyps.add(type);
				else if(pointBool.isStructEquivTo(type))
					parTyps.add(type);
				else if(pointChar.isStructEquivTo(type))
					parTyps.add(type);
				else if(pointString.isStructEquivTo(type))
					parTyps.add(type);
				else if(pointVoid.isStructEquivTo(type))
					parTyps.add(type);
				else
					throw(new CompilerError("Unrecognisable symbol at funDef par " + funDef.par(p)));
			}
			
			
			Typ type = attrs.typAttr.get(funDef.type);
			
			if(integer.isStructEquivTo(type))
				attrs.typAttr.set(funDef, new FunTyp(parTyps, type));
			else if(bool.isStructEquivTo(type))
				attrs.typAttr.set(funDef, new FunTyp(parTyps, type));
			else if(ch.isStructEquivTo(type))
				attrs.typAttr.set(funDef, new FunTyp(parTyps, type));
			else if(string.isStructEquivTo(type))
				attrs.typAttr.set(funDef, new FunTyp(parTyps, type));
			else if(Void.isStructEquivTo(type))
				attrs.typAttr.set(funDef, new FunTyp(parTyps, type));
			else if(pointInt.isStructEquivTo(type))
				attrs.typAttr.set(funDef, new FunTyp(parTyps, type));
			else if(pointBool.isStructEquivTo(type))
				attrs.typAttr.set(funDef, new FunTyp(parTyps, type));
			else if(pointChar.isStructEquivTo(type))
				attrs.typAttr.set(funDef, new FunTyp(parTyps, type));
			else if(pointString.isStructEquivTo(type))
				attrs.typAttr.set(funDef, new FunTyp(parTyps, type));
			else if(pointVoid.isStructEquivTo(type))
				attrs.typAttr.set(funDef, new FunTyp(parTyps, type));
			else
				throw(new CompilerError("Unrecognisable symbol at funDef type " + funDef));
		}
		if(prelet == 2)
		{
			funDef.body.accept(this);
		
			Typ body = attrs.typAttr.get(funDef.body);
			
			if (body== null) {
				throw new CompilerError("Function body can not be null " + funDef.body + " " + funDef.name + " at " + funDef);
			}
		}
	}

	public void visit(IfExpr ifExpr) {
		ifExpr.cond.accept(this);
		ifExpr.thenExpr.accept(this);
		ifExpr.elseExpr.accept(this);
		
		Typ cond = attrs.typAttr.get(ifExpr.cond);
		
		if(!(cond instanceof BooleanTyp))
			throw(new CompilerError("Conditional expression must be of boolean type " + ifExpr.cond));
		
		Typ thenBody = attrs.typAttr.get(ifExpr.thenExpr);
		
		if(thenBody == null)
			throw(new CompilerError("Then expression can not be null " + ifExpr.thenExpr));
		
		Typ elseExpr = attrs.typAttr.get(ifExpr.elseExpr);
		
		if(elseExpr == null)
			throw(new CompilerError("Else expression can not be null " + ifExpr.elseExpr));
	
		attrs.typAttr.set(ifExpr, new VoidTyp());
	}

	public void visit(ParDecl parDecl) {
		String name = symbolTable.newNamespace(parDecl.name);
		namespace.push(name);
		symbolTable.enterNamespace(name);

		parDecl.type.accept(this);
		
		attrs.typAttr.set(parDecl, attrs.typAttr.get(parDecl.type));
		
		namespace.pop();
		symbolTable.leaveNamespace();
	}

	public void visit(Program program) {
		namespace = new Stack<>();
		namespace.push("#");
		program.expr.accept(this);
		attrs.typAttr.set(program, attrs.typAttr.get(program.expr));
	}

	public void visit(PtrType ptrType) {
		if(prelet >= 1)
		{
			ptrType.baseType.accept(this);
			Typ type = attrs.typAttr.get(ptrType.baseType);
			attrs.typAttr.set(ptrType, new PtrTyp(type));
		}
	}

	public void visit(RecType recType) {
		LinkedList<Typ> compTyps = new LinkedList<Typ>();
		for (int c = 0; c < recType.numComps(); c++)
		{
			recType.comp(c).accept(this);
			try {
				
				symbolTable.insDecl(namespace.peek(), recType.comp(c).name, recType.comp(c));
			} catch (CannotInsNameDecl e) {
				//throw new CompilerError("Component cannot be declared "+ recType.comp(c));
			}
			compTyps.add(attrs.typAttr.get(recType.comp(c)));
		}
		attrs.typAttr.set(recType, new RecTyp(namespace.peek(), compTyps));
	}

	public void visit(TypeDecl typDecl) {
		String name = symbolTable.newNamespace(typDecl.name);
		namespace.push(name);
		symbolTable.enterNamespace(name);
		
		if(prelet == 0)
			attrs.typAttr.set(typDecl, new TypName(typDecl.name));

		if (prelet == 1)
		{
			typDecl.type.accept(this);
			TypName typName = (TypName)attrs.typAttr.get(typDecl);
			typName.setType(attrs.typAttr.get(typDecl.type));
		}
		namespace.pop();
		symbolTable.leaveNamespace();
	}

	public void visit(TypeName typeName) {
		attrs.typAttr.set(typeName, attrs.typAttr.get(attrs.declAttr.get(typeName)));
	}

	public void visit(UnExpr unExpr) {
		unExpr.subExpr.accept(this);
		switch (unExpr.oper) {
		case ADD:
		case SUB:
		{
			if(attrs.typAttr.get(unExpr.subExpr) != null && attrs.typAttr.get(unExpr.subExpr) instanceof IntegerTyp)
				attrs.typAttr.set(unExpr, new IntegerTyp());
			else
				throw(new CompilerError("Wrong operator for integer expression " + unExpr));
			break;
		}
		case NOT:
		{
			if(attrs.typAttr.get(unExpr.subExpr) != null && attrs.typAttr.get(unExpr.subExpr) instanceof BooleanTyp)
				attrs.typAttr.set(unExpr, new BooleanTyp());
			else
				throw(new CompilerError("Wrong operator for boolean expression " + unExpr));
			break;
		}
		case VAL:
		{
			Typ type = attrs.typAttr.get(unExpr.subExpr);
			PtrTyp ptr = (PtrTyp) type.actualTyp();
			
			if(ptr != null && ptr instanceof PtrTyp)
				attrs.typAttr.set(unExpr, ptr.baseTyp);
			else
				throw(new CompilerError("Unrecognisable symbol at UnExpr VAL " + unExpr.subExpr));
			break; 
		}
		case MEM:
		{
			Typ type = attrs.typAttr.get(unExpr.subExpr);
			
			if(type != null && !(type instanceof VoidTyp))// && attrs.memAttr.get(unExpr.subExpr))
				attrs.typAttr.set(unExpr, new PtrTyp(type));
			else
				throw(new CompilerError("Mem type error " + unExpr.subExpr));
			break;
		}
		default:
			break;
		}
	}

	public void visit(VarDecl varDecl) {
		if(prelet == 1)
		{
			String name = symbolTable.newNamespace(varDecl.name);
			namespace.push(name);
			symbolTable.enterNamespace(name);
			
			
			varDecl.type.accept(this);
			attrs.typAttr.set(varDecl, attrs.typAttr.get(varDecl.type));
			
			namespace.pop();
			symbolTable.leaveNamespace();
				
		}
	}

	public void visit(VarName varName) {
		attrs.typAttr.set(varName, attrs.typAttr.get(attrs.declAttr.get(varName)));
	}

	public void visit(WhereExpr whereExpr) {
		prelet = 0;
		for (int d = whereExpr.numDecls()-1; d >= 0; d--)
			whereExpr.decl(d).accept(this);
		prelet = 1;
		for (int d = whereExpr.numDecls()-1; d >= 0; d--)
			whereExpr.decl(d).accept(this);

		prelet = 2;
		for (int d = whereExpr.numDecls()-1; d >= 0; d--)
		{
			whereExpr.decl(d).accept(this);
			if(attrs.typAttr.get(whereExpr.decl(d)) == null)
				throw(new CompilerError("A declaration inside where statement cannot be without a type " + whereExpr.decl(d)));
		}
		whereExpr.expr.accept(this);
		
		attrs.typAttr.set(whereExpr, attrs.typAttr.get(whereExpr.expr));
	}

	public void visit(WhileExpr whileExpr) {
		whileExpr.cond.accept(this);
		whileExpr.body.accept(this);
		
		Typ type = attrs.typAttr.get(whileExpr.cond);

		if(!(type instanceof BooleanTyp))
			throw(new CompilerError("Conditional expression is not boolean " + whileExpr.cond));
		
		Typ body = attrs.typAttr.get(whileExpr.body);
		
		if(body == null)
			throw(new CompilerError("Loop body do not have a type " + whileExpr.body));
		
		attrs.typAttr.set(whileExpr, new VoidTyp());
	}
}
