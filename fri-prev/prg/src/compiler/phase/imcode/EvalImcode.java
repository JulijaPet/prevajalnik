package compiler.phase.imcode;

import java.util.*;

import compiler.common.report.*;
import compiler.data.acc.*;
import compiler.data.ast.*;
import compiler.data.ast.attr.*;
import compiler.data.ast.code.*;
import compiler.data.frg.*;
import compiler.data.frm.*;
import compiler.data.imc.*;

/**
 * Evaluates intermediate code.
 * 
 * @author juliette
 */
public class EvalImcode extends FullVisitor {

	private final Attributes attrs;

	private final HashMap<String, Fragment> fragments;

	private Stack<CodeFragment> codeFragments = new Stack<CodeFragment>();

	public EvalImcode(Attributes attrs, HashMap<String, Fragment> fragments) {
		this.attrs = attrs;
		this.fragments = fragments;
	}
	
	@Override
	public void visit(AtomExpr atomExpr) {
		switch (atomExpr.type) {
		case INTEGER:
			try {
				long value = Long.parseLong(atomExpr.value);
				attrs.imcAttr.set(atomExpr, new CONST(value));
			} catch (NumberFormatException ex) {
				Report.warning(atomExpr, "Illegal integer constant.");
			}
			break;
		case BOOLEAN:
			if (atomExpr.value.equals("true"))
				attrs.imcAttr.set(atomExpr, new CONST(1));
			if (atomExpr.value.equals("false"))
				attrs.imcAttr.set(atomExpr, new CONST(0));
			break;
		case CHAR:
			if (atomExpr.value.charAt(1) == 92)
			{
				switch(atomExpr.value.charAt(2)){
				    case 39:
				    	attrs.imcAttr.set(atomExpr, new CONST(39));
				     break;
				    case 34:
				    	attrs.imcAttr.set(atomExpr, new CONST(34));
				     break;
				    case 92: 
				    	attrs.imcAttr.set(atomExpr, new CONST(92));
				     break;
				    case 110:
				    	attrs.imcAttr.set(atomExpr, new CONST(10));
				     break;
				    case 116:
				    	attrs.imcAttr.set(atomExpr, new CONST(9));
				     break;
				}
			}
			else
				attrs.imcAttr.set(atomExpr, new CONST(atomExpr.value.charAt(1)));
			break;
		case STRING:
			String label = LABEL.newLabelName();
			attrs.imcAttr.set(atomExpr, new NAME(label));
			ConstFragment fragment = new ConstFragment(label, atomExpr.value);
			attrs.frgAttr.set(atomExpr, fragment);
			fragments.put(fragment.label, fragment);
			break;
		case PTR:
			attrs.imcAttr.set(atomExpr, new CONST(0));
			break;
		case VOID:
			attrs.imcAttr.set(atomExpr, new NOP());
			break;
		}
	}
	
	@Override
	public void visit(BinExpr binExpr) {
		binExpr.fstExpr.accept(this);
		binExpr.sndExpr.accept(this);
		
		
		switch (binExpr.oper) {
		case ADD:
		{
			IMCExpr imcExpr1 = (IMCExpr) attrs.imcAttr.get(binExpr.fstExpr);
			IMCExpr imcExpr2 = (IMCExpr) attrs.imcAttr.get(binExpr.sndExpr);
			attrs.imcAttr.set(binExpr, new BINOP(BINOP.Oper.ADD, imcExpr1,imcExpr2));
			break;
		}
		case SUB:
		{
			IMCExpr imcExpr1 = (IMCExpr) attrs.imcAttr.get(binExpr.fstExpr);
			IMCExpr imcExpr2 = (IMCExpr) attrs.imcAttr.get(binExpr.sndExpr);
			attrs.imcAttr.set(binExpr, new BINOP(BINOP.Oper.SUB, imcExpr1,imcExpr2));
			break;
		}
		case MUL:
		{
			IMCExpr imcExpr1 = (IMCExpr) attrs.imcAttr.get(binExpr.fstExpr);
			IMCExpr imcExpr2 = (IMCExpr) attrs.imcAttr.get(binExpr.sndExpr);
			attrs.imcAttr.set(binExpr, new BINOP(BINOP.Oper.MUL, imcExpr1,imcExpr2));
			break;
		}
		case DIV:
		{
			IMCExpr imcExpr1 = (IMCExpr) attrs.imcAttr.get(binExpr.fstExpr);
			IMCExpr imcExpr2 = (IMCExpr) attrs.imcAttr.get(binExpr.sndExpr);
			attrs.imcAttr.set(binExpr, new BINOP(BINOP.Oper.DIV, imcExpr1,imcExpr2));
			break;
		}
		case MOD:
		{
			IMCExpr imcExpr1 = (IMCExpr) attrs.imcAttr.get(binExpr.fstExpr);
			IMCExpr imcExpr2 = (IMCExpr) attrs.imcAttr.get(binExpr.sndExpr);
			attrs.imcAttr.set(binExpr, new BINOP(BINOP.Oper.MOD, imcExpr1,imcExpr2));
			break;
		}
		case AND:
		{
			IMCExpr imcExpr1 = (IMCExpr) attrs.imcAttr.get(binExpr.fstExpr);
			IMCExpr imcExpr2 = (IMCExpr) attrs.imcAttr.get(binExpr.sndExpr);
			attrs.imcAttr.set(binExpr, new BINOP(BINOP.Oper.AND, imcExpr1,imcExpr2));
			break;
		}
		case OR:
		{
			IMCExpr imcExpr1 = (IMCExpr) attrs.imcAttr.get(binExpr.fstExpr);
			IMCExpr imcExpr2 = (IMCExpr) attrs.imcAttr.get(binExpr.sndExpr);
			attrs.imcAttr.set(binExpr, new BINOP(BINOP.Oper.OR, imcExpr1,imcExpr2));
			break;
		}
		case EQU:
		{
			IMCExpr imcExpr1 = (IMCExpr) attrs.imcAttr.get(binExpr.fstExpr);
			IMCExpr imcExpr2 = (IMCExpr) attrs.imcAttr.get(binExpr.sndExpr);
			attrs.imcAttr.set(binExpr, new BINOP(BINOP.Oper.EQU, imcExpr1,imcExpr2));
			break;
		}
		case NEQ:
		{
			IMCExpr imcExpr1 = (IMCExpr) attrs.imcAttr.get(binExpr.fstExpr);
			IMCExpr imcExpr2 = (IMCExpr) attrs.imcAttr.get(binExpr.sndExpr);
			attrs.imcAttr.set(binExpr, new BINOP(BINOP.Oper.NEQ, imcExpr1,imcExpr2));
			break;
		}
		case LTH:
		{
			IMCExpr imcExpr1 = (IMCExpr) attrs.imcAttr.get(binExpr.fstExpr);
			IMCExpr imcExpr2 = (IMCExpr) attrs.imcAttr.get(binExpr.sndExpr);
			attrs.imcAttr.set(binExpr, new BINOP(BINOP.Oper.LTH, imcExpr1,imcExpr2));
			break;
		}
		case GTH:
		{
			IMCExpr imcExpr1 = (IMCExpr) attrs.imcAttr.get(binExpr.fstExpr);
			IMCExpr imcExpr2 = (IMCExpr) attrs.imcAttr.get(binExpr.sndExpr);
			attrs.imcAttr.set(binExpr, new BINOP(BINOP.Oper.GTH, imcExpr1,imcExpr2));
			break;
		}
		case LEQ:
		{
			IMCExpr imcExpr1 = (IMCExpr) attrs.imcAttr.get(binExpr.fstExpr);
			IMCExpr imcExpr2 = (IMCExpr) attrs.imcAttr.get(binExpr.sndExpr);
			attrs.imcAttr.set(binExpr, new BINOP(BINOP.Oper.LEQ, imcExpr1,imcExpr2));
			break;
		}
		case GEQ:
		{
			IMCExpr imcExpr1 = (IMCExpr) attrs.imcAttr.get(binExpr.fstExpr);
			IMCExpr imcExpr2 = (IMCExpr) attrs.imcAttr.get(binExpr.sndExpr);
			attrs.imcAttr.set(binExpr, new BINOP(BINOP.Oper.GEQ, imcExpr1,imcExpr2));
			break;
		}	
		case ASSIGN:
		{
			IMCExpr imcExpr1 = (IMCExpr) attrs.imcAttr.get(binExpr.fstExpr);
			IMCExpr imcExpr2 = (IMCExpr) attrs.imcAttr.get(binExpr.sndExpr);
			attrs.imcAttr.set(binExpr, new SEXPR(new MOVE(imcExpr1,imcExpr2), new NOP()));
			break;
		}
		case ARR:
		{
			IMCExpr imcExpr1 = (IMCExpr) attrs.imcAttr.get(binExpr.fstExpr);
			IMCExpr imcExpr2 = (IMCExpr) attrs.imcAttr.get(binExpr.sndExpr);
			long size = attrs.typAttr.get(binExpr.sndExpr).actualTyp().size();
			attrs.imcAttr.set(binExpr, new MEM(new BINOP(BINOP.Oper.ADD, imcExpr1, new BINOP(BINOP.Oper.MUL, imcExpr2, new CONST(size))) ,size));
			break;
		}
		case REC:
		{
			IMCExpr imcExpr1 = (IMCExpr) attrs.imcAttr.get(binExpr.fstExpr);
			IMCExpr imcExpr2 = (IMCExpr) attrs.imcAttr.get(binExpr.sndExpr);
			long size = attrs.typAttr.get(binExpr.sndExpr).actualTyp().size();
			attrs.imcAttr.set(binExpr, new MEM(new BINOP(BINOP.Oper.ADD, imcExpr1, imcExpr2) ,size));
			break;
		}
		default:
			break;
		}
	}
	
	@Override
	public void visit(CastExpr castExpr) {
		castExpr.type.accept(this);
		castExpr.expr.accept(this);
		attrs.imcAttr.set(castExpr, attrs.imcAttr.get(castExpr.expr));
	}
	
	@Override
	public void visit(CompName compName) {
		Decl decl = attrs.declAttr.get(compName);
		if(decl instanceof CompDecl)
		{
			CompDecl compDecl = (CompDecl)decl;
			Access access = attrs.accAttr.get(compDecl);
			if(access instanceof OffsetAccess)
			{
				OffsetAccess offsetAccess = (OffsetAccess)access;
				attrs.imcAttr.set(compName, new CONST(offsetAccess.offset));
			}
		}
	}
	
	@Override
	public void visit(Exprs exprs) {
			Vector<IMCStmt> stmts = new Vector<IMCStmt>();
			for (int e = 0; e < exprs.numExprs(); e++)
			{
				exprs.expr(e).accept(this);
				if(e != exprs.numExprs()-1)
					stmts.add(new ESTMT((IMCExpr)attrs.imcAttr.get(exprs.expr(e))));

			}
			
			attrs.imcAttr.set(exprs, new SEXPR(new STMTS(stmts), (IMCExpr) attrs.imcAttr.get(exprs.expr(exprs.numExprs()-1))));
	}
	
	@Override
	public void visit(ForExpr forExpr) {
		forExpr.var.accept(this);
		forExpr.loBound.accept(this);
		forExpr.hiBound.accept(this);
		forExpr.body.accept(this);
		
		IMCExpr var = (IMCExpr)attrs.imcAttr.get(forExpr.var);
		
		Vector<IMCStmt> stmts = new Vector<IMCStmt>();
		
		MOVE move = new MOVE(var, (IMCExpr)attrs.imcAttr.get(forExpr.loBound));
		
		stmts.add(move);
		
		String a = LABEL.newLabelName();
		String b = LABEL.newLabelName();
		String c = LABEL.newLabelName();
		
		//stmts.add(new LABEL(entry));
		
		CJUMP cJump = new CJUMP(new BINOP(BINOP.Oper.LEQ, (IMCExpr)attrs.imcAttr.get(forExpr.var), (IMCExpr)attrs.imcAttr.get(forExpr.hiBound)), a, c);
		
		stmts.add(cJump);
		
		stmts.add(new LABEL(a));
		
		ESTMT estmt = new ESTMT((IMCExpr) attrs.imcAttr.get(forExpr.body));
		
		stmts.add(estmt);
		
		cJump = new CJUMP(new BINOP(BINOP.Oper.LTH, (IMCExpr)attrs.imcAttr.get(forExpr.var), (IMCExpr)attrs.imcAttr.get(forExpr.hiBound)), b, c);
		
		stmts.add(cJump);
		
		stmts.add(new LABEL(b));
		
		move = new MOVE(var, new BINOP(BINOP.Oper.ADD, var, new CONST(1)));
		
		stmts.add(move);
		
		JUMP jump = new JUMP(a);
		
		stmts.add(jump);
		
		stmts.add(new LABEL(c));
		
		attrs.imcAttr.set(forExpr, new SEXPR(new STMTS(stmts), new NOP())); 
	}
	
	@Override
	public void visit(FunCall funCall) {
		Decl decl = attrs.declAttr.get(funCall);
		if(decl instanceof FunDef)
		{
			FunDef fundef = (FunDef)decl;
			Frame frame = attrs.frmAttr.get(fundef);
			
			Vector<IMCExpr> arguments = new Vector<IMCExpr>();
			Vector<Long> width = new Vector<Long>();
			
			CodeFragment code = codeFragments.peek();
	
			/*if(code.label.equals(funCall.name()))
			{
				MEM SL = new MEM(new TEMP(code.FP), 8);
				arguments.add(SL);
			}
			else
			{
				TEMP SL = new TEMP(code.FP);
				arguments.add(SL);
			}*/
			
			MEM SL = new MEM(new TEMP(code.FP), 8);
			arguments.add(SL);
			
			width.add((long) 8); //static link
			
			for (int a = 0; a < funCall.numArgs(); a++)
			{
				funCall.arg(a).accept(this);
				arguments.add((IMCExpr) attrs.imcAttr.get(funCall.arg(a)));
				width.add(attrs.typAttr.get(funCall.arg(a)).actualTyp().size());
			}
			
			attrs.imcAttr.set(funCall, new CALL(frame.label, arguments, width)); 
		}
		else
		{
			FunDecl fundecl = (FunDecl)decl;
			
			Vector<IMCExpr> arguments = new Vector<IMCExpr>();
			Vector<Long> width = new Vector<Long>();
			
			CodeFragment code = codeFragments.peek();
			MEM SL = new MEM(new TEMP(code.FP), 8);
			
			arguments.add(SL);
			width.add((long) 8); //static link
			
			for (int a = 0; a < funCall.numArgs(); a++)
			{
				funCall.arg(a).accept(this);
				arguments.add((IMCExpr) attrs.imcAttr.get(funCall.arg(a)));
				width.add(attrs.typAttr.get(funCall.arg(a)).actualTyp().size());
			}
			attrs.imcAttr.set(funCall, new CALL("_"+fundecl.name, arguments, width)); 
			
		}
	}
	
	@Override
	public void visit(FunDef funDef) {
		Frame frame = attrs.frmAttr.get(funDef);
		int FP = TEMP.newTempName();
		int RV = TEMP.newTempName();
		CodeFragment tmpFragment = new CodeFragment(frame, FP, RV, null);
		codeFragments.push(tmpFragment);

		for (int p = 0; p < funDef.numPars(); p++)
			funDef.par(p).accept(this);
		funDef.type.accept(this);
		funDef.body.accept(this);

		codeFragments.pop();
		IMCExpr expr = (IMCExpr) attrs.imcAttr.get(funDef.body);
		MOVE move = new MOVE(new TEMP(RV), expr);
		Fragment fragment = new CodeFragment(tmpFragment.frame, tmpFragment.FP, tmpFragment.RV, move);
		attrs.frgAttr.set(funDef, fragment);
		attrs.imcAttr.set(funDef, move);
		fragments.put(fragment.label, fragment);
	}
	
	@Override
	public void visit(IfExpr ifExpr) {
		ifExpr.cond.accept(this);
		ifExpr.thenExpr.accept(this);
		ifExpr.elseExpr.accept(this);
		
		Vector<IMCStmt> stmts = new Vector<IMCStmt>();
		
		String a = LABEL.newLabelName();
		String b = LABEL.newLabelName();
		String c = LABEL.newLabelName();
		
		CJUMP cJump = new CJUMP((IMCExpr) attrs.imcAttr.get(ifExpr.cond), a, b);
		
		stmts.add(cJump);
		
		stmts.add(new LABEL(a));
		
		ESTMT estmt = new ESTMT((IMCExpr) attrs.imcAttr.get(ifExpr.thenExpr));
		
		stmts.add(estmt);
		
		JUMP jump = new JUMP(c);
		
		stmts.add(jump);
		
		stmts.add(new LABEL(b));
		
		estmt = new ESTMT((IMCExpr) attrs.imcAttr.get(ifExpr.elseExpr));
		
		stmts.add(estmt);
		
		stmts.add(new LABEL(c));
		
		attrs.imcAttr.set(ifExpr, new SEXPR(new STMTS(stmts), new NOP()));
	}
	
	@Override
	public void visit(Program program) {
		//program.expr.accept(this);
		Frame frame = new Frame(0, "_", 0, 0, 0, 0, 0);
		int FP = TEMP.newTempName();
		int RV = TEMP.newTempName();
		CodeFragment tmpFragment = new CodeFragment(frame, FP, RV, null);
		codeFragments.push(tmpFragment);

		program.expr.accept(this);
		
		codeFragments.pop();

		MOVE move;
		if(attrs.imcAttr.get(program.expr) instanceof IMCStmt)
			move = new MOVE(new TEMP(1), new SEXPR((IMCStmt) attrs.imcAttr.get(program.expr), new NOP()));
		else
			move = new MOVE(new TEMP(1), (IMCExpr) attrs.imcAttr.get(program.expr));
		Fragment fragment = new CodeFragment(tmpFragment.frame, tmpFragment.FP, tmpFragment.RV, move);
		attrs.frgAttr.set(program, fragment);
		fragments.put(fragment.label, fragment);
	}
	
	@Override
	public void visit(UnExpr unExpr) {
		unExpr.subExpr.accept(this);
		switch (unExpr.oper) {
		case ADD:
		{
			IMCExpr imcExpr = (IMCExpr) attrs.imcAttr.get(unExpr.subExpr);
			attrs.imcAttr.set(unExpr, new UNOP(UNOP.Oper.ADD, imcExpr));
			break;
		}
		case SUB:
		{
			IMCExpr imcExpr = (IMCExpr) attrs.imcAttr.get(unExpr.subExpr);
			attrs.imcAttr.set(unExpr, new UNOP(UNOP.Oper.SUB, imcExpr));
			break;
		}
		case NOT:
		{
			IMCExpr imcExpr = (IMCExpr) attrs.imcAttr.get(unExpr.subExpr);
			attrs.imcAttr.set(unExpr, new UNOP(UNOP.Oper.NOT, imcExpr));
			break;
		}
		case VAL:
		{
			attrs.imcAttr.set(unExpr, new MEM((IMCExpr) attrs.imcAttr.get(unExpr.subExpr), attrs.typAttr.get(unExpr.subExpr).actualTyp().size()));
			break; 
		}
		case MEM:
		{
			attrs.imcAttr.set(unExpr, ((MEM)attrs.imcAttr.get(unExpr.subExpr)).addr);
			break;
		}
		default:
			break;
		}
	}
	
	@Override
	public void visit(VarDecl varDecl) {
		varDecl.type.accept(this);
		Access vardecl = attrs.accAttr.get(varDecl);
		if(vardecl instanceof StaticAccess)
		{
			StaticAccess staticVar = (StaticAccess)vardecl;
			String label = ((StaticAccess) vardecl).label;
			attrs.imcAttr.set(varDecl, new NAME(label));
			DataFragment fragment = new DataFragment(label, staticVar.size);
			attrs.frgAttr.set(varDecl, fragment);
			fragments.put(fragment.label, fragment);
		}
	}

	@Override
	public void visit(VarName varName) {
		Decl decl = attrs.declAttr.get(varName);
		VarDecl vardecl = (VarDecl)decl;
		Access access = attrs.accAttr.get(vardecl);
		if(access instanceof StaticAccess)
		{
			StaticAccess staticAcces = (StaticAccess)access;
			attrs.imcAttr.set(varName, new MEM(new NAME(staticAcces.label),staticAcces.size));
		}
		else if(access instanceof OffsetAccess)
		{
			OffsetAccess offsetAccess = (OffsetAccess)access;
			attrs.imcAttr.set(varName, new MEM(new BINOP(BINOP.Oper.ADD, new TEMP(codeFragments.peek().FP), new CONST(offsetAccess.offset)), offsetAccess.size));
		}
	}
	
	@Override
	public void visit(WhereExpr whereExpr) {
		whereExpr.expr.accept(this);
		
		for (int d = 0; d < whereExpr.numDecls(); d++)
			whereExpr.decl(d).accept(this);
		
		attrs.imcAttr.set(whereExpr, attrs.imcAttr.get(whereExpr.expr));
	}

	@Override
	public void visit(WhileExpr whileExpr) {
		whileExpr.cond.accept(this);
		whileExpr.body.accept(this);
		
		Vector<IMCStmt> stmts = new Vector<IMCStmt>();
		
		String entry = LABEL.newLabelName();
		String loop = LABEL.newLabelName();
		String exit = LABEL.newLabelName();
		
		stmts.add(new LABEL(entry));
		
		IMCExpr cond = (IMCExpr)attrs.imcAttr.get(whileExpr.cond);
		
		CJUMP cJump = new CJUMP(cond, loop, exit);
		
		stmts.add(cJump);
		
		stmts.add(new LABEL(loop));
		
		ESTMT estmt = new ESTMT((IMCExpr) attrs.imcAttr.get(whileExpr.body));
		
		stmts.add(estmt);
		
		JUMP jump = new JUMP(entry);
		
		stmts.add(jump);
		
		stmts.add(new LABEL(exit));
		
		attrs.imcAttr.set(whileExpr, new SEXPR(new STMTS(stmts), new NOP()));
	}

}
