package compiler.phase.frames;

import java.util.*;

import compiler.data.acc.*;
import compiler.data.ast.*;
import compiler.data.ast.attr.*;
import compiler.data.ast.code.*;
import compiler.data.frm.*;
import compiler.data.typ.*;

/**
 * Frame and access evaluator.
 * 
 * @author juliette
 */
public class EvalFrames extends FullVisitor {

	private final Attributes attrs;
	
	private Stack<Frame> frames;
	
	public EvalFrames(Attributes attrs) {
		this.attrs = attrs;
		frames = new Stack<Frame>();
		
	}
	
	public void visit(FunCall funCall) {
		if(!(frames.isEmpty()))
		{
			Frame curFrame = frames.pop();
			long outCallSize = 0;
			for (int a = 0; a < funCall.numArgs(); a++)
			{
				funCall.arg(a).accept(this);	
				long size = attrs.typAttr.get(funCall.arg(a)).actualTyp().size();
				if(size < 8)
					size = 8;
				outCallSize += size;
			}
			long size = attrs.typAttr.get(funCall).actualTyp().size();
			if(size < 8)
				size = 8;
			outCallSize += size;
			
			outCallSize = (outCallSize > curFrame.outCallSize) ?  outCallSize : curFrame.outCallSize;
			
			frames.push(new Frame(curFrame.level, curFrame.label, curFrame.inpCallSize, curFrame.locVarsSize, curFrame.tmpVarsSize, curFrame.hidRegsSize, outCallSize));
		}
	}
	
	public void visit(FunDef funDef) {		
		String label = "F___" + funDef.name;

		long inpCallSize = 8; //static link
		
		long locVarsSize = 0;

		long tmpVarsSize = 0;
		
		long hidRegsSize = 0;

		long outCallSize = 8; //static link

		Typ type = null;
		
		long size = 0;
		
		for (int p = 0; p < funDef.numPars(); p++)
		{
			type = attrs.typAttr.get(funDef.par(p)).actualTyp();
			size = type.size();
			if(size < 8)
				size = 8;
			if(frames.empty())
				attrs.accAttr.set(funDef.par(p), new OffsetAccess(0, inpCallSize, size));
			else
				attrs.accAttr.set(funDef.par(p), new OffsetAccess(frames.peek().level + 1, inpCallSize, size));
			inpCallSize += size;
		}
		
		size = attrs.typAttr.get(funDef.type).actualTyp().size();
		if(size < 8)
			size = 8;
		
		inpCallSize = 10;
		
		inpCallSize = (inpCallSize > size) ?  inpCallSize : size;
		
		if(frames.empty())
			frames.push(new Frame(0, label, inpCallSize, locVarsSize, tmpVarsSize, hidRegsSize, outCallSize));
		else
			frames.push(new Frame(frames.peek().level + 1, frames.peek().label + "__fun_" + funDef.name, inpCallSize, locVarsSize, tmpVarsSize, hidRegsSize, outCallSize));
		
		for (int p = 0; p < funDef.numPars(); p++)
			funDef.par(p).accept(this);
		funDef.type.accept(this);
		funDef.body.accept(this);
		Frame curFrame = frames.pop();
		if(curFrame.outCallSize == 8)
			attrs.frmAttr.set(funDef,new Frame(curFrame.level, curFrame.label, curFrame.inpCallSize, curFrame.locVarsSize, curFrame.tmpVarsSize, curFrame.hidRegsSize, 0));
		else
			attrs.frmAttr.set(funDef, curFrame);
	}

	

	public void visit(RecType recType) {
		long offset = 0;
		long size = 0;
		for (int c = 0; c < recType.numComps(); c++) 
		{
			recType.comp(c).accept(this);
			size = attrs.typAttr.get(recType.comp(c)).size();
			if(size < 8)
				size = 8;
			attrs.accAttr.set(recType.comp(c), new OffsetAccess(-1, offset, size));
			offset += size;
		}
	}
	
	public void visit(VarDecl varDecl) {
		varDecl.type.accept(this);
		
		Typ type = attrs.typAttr.get(varDecl).actualTyp();

		if(!(frames.isEmpty()))
		{	
			Frame curFrame = frames.pop();
			long size = type.size();
			if(size < 8)
				size = 8;
			attrs.accAttr.set(varDecl, new OffsetAccess(curFrame.level, (curFrame.locVarsSize + size) * (-1), size));
			long newSize = curFrame.locVarsSize + size;
			frames.push(new Frame(curFrame.level, curFrame.label, curFrame.inpCallSize, newSize, curFrame.tmpVarsSize, curFrame.hidRegsSize, curFrame.outCallSize));
		}
		else
		{
			String label = "_" + varDecl.name;
			long size = type.size();
			if(size < 8)
				size = 8;
			attrs.accAttr.set(varDecl, new StaticAccess(label, size));
		}
	}
}
