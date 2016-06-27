package compiler.phase.seman;

import compiler.common.report.CompilerError;
import compiler.data.ast.*;
import compiler.data.ast.AtomExpr.AtomTypes;
import compiler.data.ast.attr.*;
import compiler.data.ast.code.*;

/**
 * Computes the value of simple integer constant expressions.
 * 
 * <p>
 * Simple integer constant expressions consists of integer constants and five
 * basic arithmetic operators (<code>ADD</code>, <code>SUB</code>,
 * <code>MUL</code>, <code>DIV</code>, and <code>MOD</code>).
 * </p>
 * 
 * <p>
 * This is needed during type resolving and type checking to compute the correct
 * array types.
 * </p>
 * 
 * @author juliette
 */
public class EvalValue extends FullVisitor {

	private final Attributes attrs;
	
	public EvalValue(Attributes attrs) {
		this.attrs = attrs;
	}
	
	public void visit(AtomExpr atomExpr) {
		if(atomExpr.type == AtomTypes.INTEGER)
		{	
			try {
				Long n = Long.parseLong(atomExpr.value);
				attrs.valueAttr.set(atomExpr, n);
			} catch (NumberFormatException ex) {
				throw(new CompilerError("Illegal integer value at " + atomExpr + " " + atomExpr.value));
			}

		}
	}


	public void visit(BinExpr binExpr) {
		binExpr.fstExpr.accept(this);
		binExpr.sndExpr.accept(this);
		Long fstExpr = attrs.valueAttr.get(binExpr.fstExpr);
		Long sndExpr = attrs.valueAttr.get(binExpr.sndExpr);
		switch (binExpr.oper) {
		case ADD:
		{
			if(fstExpr != null && sndExpr != null)
				attrs.valueAttr.set(binExpr, fstExpr + sndExpr);
			break;
		}
		case SUB:
		{
			if(fstExpr != null && sndExpr != null)
				attrs.valueAttr.set(binExpr, fstExpr - sndExpr);
			break;
		}	
		case MUL:
		{
			if(fstExpr != null && sndExpr != null)
				attrs.valueAttr.set(binExpr, fstExpr * sndExpr);
			break;
		}
		case DIV:
		{
			if(fstExpr != null && sndExpr != null)
				attrs.valueAttr.set(binExpr, fstExpr / sndExpr);
			break;
		}
		case MOD:
		{
			if(fstExpr != null && sndExpr != null)
				attrs.valueAttr.set(binExpr, fstExpr % sndExpr);
			break;
		}
		default:
			break;	
		}
	}

	public void visit(UnExpr unExpr) {
		unExpr.subExpr.accept(this);
		Long subExpr = attrs.valueAttr.get(unExpr.subExpr);
		switch (unExpr.oper) {
		case ADD:
		{
			if(subExpr != null)
				attrs.valueAttr.set(unExpr, +subExpr);
			break;
		}
		case SUB:
		{
			if(subExpr != null)
				attrs.valueAttr.set(unExpr, -subExpr);
			break;
		}	
		default:
			break;
		}
	}

}
