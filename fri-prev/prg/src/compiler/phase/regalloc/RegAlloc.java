package compiler.phase.regalloc;

import compiler.*;
import compiler.phase.*;

/**
 * 
 * @author juliette
 */
public class RegAlloc extends Phase {

	  
	public RegAlloc(Task task) {
		super(task, "codegen");
	  
		this.task = task;  

	 }
	 
	@Override
	public void close() {
		super.close();
	}
	
}
