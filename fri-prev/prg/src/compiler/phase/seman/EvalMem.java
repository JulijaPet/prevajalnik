package compiler.phase.seman;

import compiler.data.ast.*;
import compiler.data.ast.attr.*;
import compiler.data.ast.code.*;
import compiler.data.typ.*;

/**
 * @author juliette
 */
public class EvalMem extends FullVisitor {
	
	private final Attributes attrs;
	
	public EvalMem(Attributes attrs) {
		this.attrs = attrs;
	}
	
	public void visit(AtomExpr atomExpr) {
		attrs.memAttr.set(atomExpr, false);
	}

	public void visit(BinExpr binExpr) {
		binExpr.fstExpr.accept(this);
		binExpr.sndExpr.accept(this);
		
		switch (binExpr.oper) {
		case REC:
		{
			attrs.memAttr.set(binExpr, true);
			break;
		}
		case ARR:
		{
			attrs.memAttr.set(binExpr, true);
			break;
		}
		default:
			attrs.memAttr.set(binExpr, false);
			break;
		}
	}

	public void visit(CastExpr castExpr) {
		castExpr.type.accept(this);
		castExpr.expr.accept(this);
		
		Typ t = attrs.typAttr.get(castExpr);
		if (t != null && t.actualTyp() instanceof PtrTyp)
			attrs.memAttr.set(castExpr, true);
		else
			attrs.memAttr.set(castExpr, false);
	}


	public void visit(CompName compName) {
		attrs.memAttr.set(compName, true);
	}

	public void visit(Exprs exprs) {
		for (int e = 0; e < exprs.numExprs(); e++)
			exprs.expr(e).accept(this);
		
		Typ t = attrs.typAttr.get(exprs.expr(exprs.numExprs() - 1));
		if (t != null && t.actualTyp() instanceof PtrTyp)
			attrs.memAttr.set(exprs, true);
		else
			attrs.memAttr.set(exprs, false);
	}


	public void visit(ForExpr forExpr) {
		forExpr.var.accept(this);
		forExpr.loBound.accept(this);
		forExpr.hiBound.accept(this);
		forExpr.body.accept(this);
		
		Typ t = attrs.typAttr.get(forExpr);
		if (t != null && t.actualTyp() instanceof PtrTyp)
			attrs.memAttr.set(forExpr, true);
		else
			attrs.memAttr.set(forExpr, false);
	}

	public void visit(FunCall funCall) {
		for (int a = 0; a < funCall.numArgs(); a++)
			funCall.arg(a).accept(this);
		
		Typ t = attrs.typAttr.get(funCall);
		if (t != null && t.actualTyp() instanceof PtrTyp)
			attrs.memAttr.set(funCall, true);
		else
			attrs.memAttr.set(funCall, false);
	}

	public void visit(IfExpr ifExpr) {
		ifExpr.cond.accept(this);
		ifExpr.thenExpr.accept(this);
		ifExpr.elseExpr.accept(this);

		Typ t = attrs.typAttr.get(ifExpr);
		if (t != null && t.actualTyp() instanceof PtrTyp)
			attrs.memAttr.set(ifExpr, true);
		else
			attrs.memAttr.set(ifExpr, false);
	}

	public void visit(Program program) {
		program.expr.accept(this);
		
		Typ t = attrs.typAttr.get(program);
		if (t != null && t.actualTyp() instanceof PtrTyp)
			attrs.memAttr.set(program, true);
		else
			attrs.memAttr.set(program, false);
	}

	public void visit(UnExpr unExpr) {
		unExpr.subExpr.accept(this);
		switch (unExpr.oper) {
		case VAL:
		{
			attrs.memAttr.set(unExpr, true);
			break;
		}
		default:
			attrs.memAttr.set(unExpr, false);
			break;
		}
	}



	public void visit(VarName varName) {
		attrs.memAttr.set(varName, true);
	}

	public void visit(WhereExpr whereExpr) {
		whereExpr.expr.accept(this);
		for (int d = 0; d < whereExpr.numDecls(); d++)
			whereExpr.decl(d).accept(this);
		
		attrs.memAttr.set(whereExpr, false);
	}

	public void visit(WhileExpr whileExpr) {
		whileExpr.cond.accept(this);
		whileExpr.body.accept(this);
		
		Typ t = attrs.typAttr.get(whileExpr);
		if (t != null && t.actualTyp() instanceof PtrTyp)
			attrs.memAttr.set(whileExpr, true);
		else
			attrs.memAttr.set(whileExpr, false);
	}
}
