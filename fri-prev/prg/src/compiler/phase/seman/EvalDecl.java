package compiler.phase.seman;

import compiler.common.report.*;
import compiler.data.ast.*;
import compiler.data.ast.attr.*;
import compiler.data.ast.code.*;

/**
 * Declaration resolver.
 * 
 * <p>
 * Declaration resolver maps each AST node denoting a
 * {@link compiler.data.ast.Declarable} name to the declaration where
 * this name is declared. In other words, it links each use of each name to a
 * declaration of that name.
 * </p>
 * 
 * @author juliette
 */
public class EvalDecl extends FullVisitor {

	private final Attributes attrs;
	
	public EvalDecl(Attributes attrs) {
		this.attrs = attrs;
	}

	/** The symbol table. */
	private SymbolTable symbolTable = new SymbolTable();
	
	public void visit(FunCall funCall) {
		try {
			Decl name = symbolTable.fndDecl(funCall.name());
			if(name instanceof FunDecl)
				attrs.declAttr.set(funCall, name);
			else
				throw(new CompilerError("Error at " + funCall + " " + funCall.name() + " is not a function name."));
		} catch (CannotFndNameDecl e) {
			throw(new CompilerError("Unknown function at " + funCall + " " + funCall.name()));
		}
		for (int a = 0; a < funCall.numArgs(); a++)
			funCall.arg(a).accept(this);
	}

	public void visit(FunDecl funDecl) {
		symbolTable.enterScope();
		for (int p = 0; p < funDecl.numPars(); p++)
		{
			try{
				symbolTable.insDecl(funDecl.par(p).name, funDecl.par(p));
			} catch(CannotInsNameDecl e){
				throw(new CompilerError("Cannot redeclare name at " + funDecl.par(p) +  " " + funDecl.par(p).name));
			}	
		}
		for (int p = 0; p < funDecl.numPars(); p++)
			funDecl.par(p).accept(this);
		funDecl.type.accept(this);	
		symbolTable.leaveScope();
	}

	public void visit(FunDef funDef) {
		symbolTable.enterScope();
		for (int p = 0; p < funDef.numPars(); p++)
		{
			try {
				symbolTable.insDecl(funDef.par(p).name, funDef.par(p));
			} catch (CannotInsNameDecl e) {
				throw(new CompilerError("Cannot redeclare name at " + funDef.par(p) +  " " + funDef.par(p).name));
			}
		}
		
		for (int p = 0; p < funDef.numPars(); p++)
			funDef.par(p).accept(this);
		funDef.type.accept(this);
		funDef.body.accept(this);
		
		symbolTable.leaveScope();
	}

	public void visit(TypeName typeName) {
		try {
			attrs.declAttr.set(typeName, symbolTable.fndDecl(typeName.name()));
		} catch (CannotFndNameDecl e) {
			throw(new CompilerError("Unknown type at " + typeName + " " + typeName.name()));
		}
	}

	public void visit(VarName varName) {
		try {
			attrs.declAttr.set(varName, symbolTable.fndDecl(varName.name()));
		} catch (CannotFndNameDecl e) {
			throw(new CompilerError("Unknown variable at " + varName + " " + varName.name()));
		}
	}

	public void visit(WhereExpr whereExpr) {
		symbolTable.enterScope();
		
		for (int d = 0; d < whereExpr.numDecls(); d++) 
		{
			try {
				symbolTable.insDecl(whereExpr.decl(d).name, whereExpr.decl(d));
			} catch (CannotInsNameDecl ex) {
				throw(new CompilerError("Cannot redeclare name at " + whereExpr.decl(d) +  " " + whereExpr.decl(d).name));
			}
		}
		for (int d = 0; d < whereExpr.numDecls(); d++) 
			whereExpr.decl(d).accept(this);
		whereExpr.expr.accept(this);
		symbolTable.leaveScope();
	}

}
