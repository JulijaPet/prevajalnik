package compiler.phase.codegen;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.Vector;

import compiler.*;
import compiler.common.report.InternalCompilerError;
import compiler.data.frg.CodeFragment;
import compiler.data.frg.ConstFragment;
import compiler.data.frg.DataFragment;
import compiler.data.frg.Fragment;
import compiler.data.imc.*;
import compiler.data.imc.MOVE;
import compiler.data.inst.*;
import compiler.phase.*;
import compiler.phase.codegen.DirectedGraph.Edge;

/**
 * 
 * @author juliette
 */
public class CodeGen extends Phase {
	
	public CodeGen(Task task) {
		super(task, "codegen");
		
		this.task = task;  
		start();
	 }
	 
	@Override
	public void close() {
		super.close();
	}

	private LinkedList<String> list;
	private Vector<Operator> temp;
	private Stack<String> sklad;
	private DirectedGraph<String> graph;
	private DirectedGraph<String> graph2;
	private int K;
	private Fragment Fragment;

	
	private void start()
	{
		K = 4;
		System.out.println("\tLOC #3FFFFFFFFFFFFFF8");
		System.out.println("sp_Base	GREG @-1");
		System.out.println("fp_Base	GREG 0");
		System.out.println("FP IS $253");
		System.out.println("SP IS $254");
		System.out.println("IO IS $255");
		
		System.out.println("\tLOC Data_Segment");
		System.out.println("D_Base\tGREG @");
		for (Fragment fragment : task.fragments.values()) {
			if (fragment instanceof ConstFragment){
				ConstFragment string = (ConstFragment) fragment;
				if(string.string.length() > 3 && string.string.charAt(string.string.length()-2) == 'n' && string.string.charAt(string.string.length()-3) == '\\')
					System.out.printf("%s	BYTE %s,10,0\n", fragment.label,string.string.substring(0, string.string.length()-3)+"\"");
				else
					System.out.printf("%s	BYTE %s,0,0\n", fragment.label, string.string);
				System.out.println("\tLOC @+8");
			}
		}
		
		for (Fragment fragment : task.fragments.values()) {
			if (fragment instanceof DataFragment){
				System.out.printf("%s\tLOC\t@+%d\n", fragment.label, ((DataFragment) fragment).width);
			}
		}

		
		System.out.println("chrBuf\tOCTA 0");
		System.out.println("\tBYTE	0");
		System.out.println("\tLOC (@+7)&-8");
		
		System.out.println("intBuf\tOCTA @+21");
		System.out.println("\tLOC (@+7)&-8");
		
		System.out.println("\tLOC #100");
		System.out.println("Main\tJMP _");

		String reg_01 = "$5";
		String reg_02 = "$6";
		String reg_03 = "$7";
		String reg_04 = "$8";
		System.out.printf("_printStr\tADD %s,SP,8\n",reg_01);
		System.out.printf("\tLDO IO,%s,0\n",reg_01);
		System.out.println("\tTRAP 0,Fputs,StdOut");
		System.out.println("\tPOP 0,0");
		System.out.printf("_printChr\tLDO %s,SP,8\n",reg_01);
		System.out.printf("\tSTB %s,chrBuf\n",reg_01);
		System.out.println("\tLDA IO,chrBuf");
		System.out.println("\tTRAP 0,Fputs,StdOut");
		System.out.println("\tPOP 0,0");
		System.out.printf("_printInt\tLDO %s,SP,8\n",reg_01);
		System.out.printf("\tCMP %s,%s,0\n",reg_04,reg_01);
		System.out.printf("\tZSN %s,%s,1\n",reg_04,reg_04);
		System.out.printf("\tBZ %s,cont\n",reg_04);
		System.out.printf("\tNEG %s,0,%s\n",reg_01,reg_01);
		System.out.printf("cont\tLDA %s,intBuf\n",reg_02);
		System.out.printf("\tADD %s,%s,20\n",reg_02,reg_02);
		System.out.printf("loop\tSUB %s,%s,1\n",reg_02,reg_02);
		System.out.printf("\tDIV %s,%s,10\n",reg_01,reg_01);
		System.out.printf("\tGET %s,6\n",reg_03);
		System.out.printf("\tADD %s,%s,48\n",reg_03,reg_03);
		System.out.printf("\tSTB %s,%s,0\n",reg_03,reg_02);
		System.out.printf("\tCMP %s,%s,0\n",reg_03,reg_01);
		System.out.printf("\tZSZ %s,%s,1\n",reg_03,reg_03);
		System.out.printf("\tPBZ %s,loop\n",reg_03);
		System.out.printf("\tBZ %s,cont2\n",reg_04);
		System.out.printf("\tSUB %s,%s,1\n",reg_02,reg_02);
		System.out.printf("\tSETL %s,45\n",reg_01);
		System.out.printf("\tSTB %s,%s,0\n",reg_01,reg_02);
		System.out.printf("cont2\tSTO %s,SP,8\n",reg_02);
		System.out.printf("\tGET %s,rJ\n",reg_01);
		System.out.println("\tPUSHJ 6,_printStr");
		System.out.printf("\tPUT rJ,%s\n",reg_01);
		System.out.println("\tPOP 0,0");
		
		//System.out.println("	LOC #100");
		
		for (Fragment fragment : task.fragments.values()) 
		{
			if (fragment instanceof CodeFragment) 
			{
				Fragment = fragment;
				list = new LinkedList<String>();
				temp = new Vector<Operator>();
				//System.out.println("++++++++++ C O D E  F R A G M E N T ++++++++++");
				//Operator oper = new Operator(((CodeFragment)fragment).label);
				//oper.izpis0();
			//	temp.add(oper);

				System.out.print(((CodeFragment)fragment).label+"\t");

				
				execute((CodeFragment)fragment);
				/*System.out.println("++++++++++++++++ T H E  E N D ++++++++++++++++");
				System.out.println();
				System.out.println("++++++++++++++++++ G R A P H +++++++++++++++++");*/

				graph = new DirectedGraph<String>();
				graph2 = new DirectedGraph<String>();
				sklad = new Stack<String>();
				liveness();
				
				simplify();
				
				coloring();
				
				//System.out.println(graph.toString());
				
				prolog();
				for(int i = 0; i < temp.size(); i++)
				{
					Operator o = temp.get(i);
					if(o.oper != null && o.a != null && o.b != null && o.c != null)
					{
						System.out.printf("\t" + o.oper, o.a, o.b, o.c);
						System.out.println();
					}
					else if(o.oper != null && o.a != null && o.b != null)
					{
						System.out.printf("\t" + o.oper, o.a, o.b);
						System.out.println();
					}
					else if(o.oper != null && o.a != null && o.c != null)
					{
						System.out.printf("\t" + o.oper, o.a, o.c);
						System.out.println();
					}
					else if(o.oper != null && o.a != null)
					{
						System.out.printf("\t" + o.oper, o.a);
						System.out.println();
					}
					else if(o.oper != null)
					{
						System.out.printf(o.oper);
						System.out.println();
					}
				}
				epilog();
				/*System.out.println("++++++++++++++++ T H E  E N D ++++++++++++++++");
				System.out.println();*/
				
			}
		}  
		
		System.out.println("\tTRAP 0,Halt,0");
	}
	
	private void coloring()
	{
		LinkedList<LinkedList<String>> vertex = new LinkedList<LinkedList<String>>();
		boolean preliv = false;
		String t = "";
		
		while(!sklad.isEmpty())
		{
			t = sklad.pop();
			//System.out.println(t);
			boolean vstavljen = false;
			for(int i = 0; i < vertex.size(); i++)
			{
				boolean contains = false;
				for(int j = 0; j < vertex.get(i).size(); j++)
				{
					String v = vertex.get(i).get(j);
					if(v != null)
					{
						for(Edge<String> edge : graph2.neighbors.get(t))
						{
							if((edge.vertex).equals(v))
							{
								contains = true;
								break;
							}
						}
					}
				}
				
				if(!contains)
				{
					vertex.get(i).add(t);
					vstavljen = true;
					break;
				}
			}
			if(!vstavljen)
			{
				if(vertex.size() < K)
				{
					LinkedList<String> l = new LinkedList<String>();
					l.add(t);
					vertex.add(l);
				}
				else
				{
					preliv = true;
				}
					
			}
			if(preliv)
				break;
		}
		//System.out.println("----------------------------------------------------------------------");
		if(preliv)
		{
			//System.out.println(t);
			izbrisiRegister(t);
			correct(t);
			graph = new DirectedGraph<String>();
			graph2 = new DirectedGraph<String>();
			sklad = new Stack<String>();
			liveness();
			simplify();
			coloring();
		}
		else
		{
			for(int i = 0; i < vertex.size(); i++)
			{		
				for(int j = 0; j < vertex.get(i).size(); j++)
				{
					String v = vertex.get(i).get(j);
					for(int k = 0; k < temp.size(); k++)
					{
						if(temp.get(k).a != null && temp.get(k).a.equals(v))
							temp.get(k).a = "$" + (i+5);
						if(temp.get(k).b != null && temp.get(k).b.equals(v))
							temp.get(k).b = "$" + (i+5);
						if(temp.get(k).c != null && temp.get(k).c.equals(v))
							temp.get(k).c = "$" + (i+5);
					}
				}
			}
		}
	}
	
	private void izbrisiRegister(String peek) {
		  for(int i = 0; i < list.size(); i++)
		   if(list.get(i).equals(peek)){
		    list.remove(i);
		    break;
		   }  
		 }
	
	private void liveness()
	{
		LinkedList<Integer> begin = new LinkedList<Integer>();
		LinkedList<Integer> end = new LinkedList<Integer>();
		for(int i = 0; i < list.size(); i++)
		{		 
			boolean zacetek = false;
			int counter = 0;
			int s = 0;
			int e = 0;
			for(int j = 0; j < temp.size(); j++)
			{		 
				Operator o = temp.get(j);
				if(o.a != null && o.a.equals(list.get(i)))
				{
					if(!zacetek)
					{
						s = counter;
						zacetek = true;
					}
					else
						e = counter;
				}
				if(o.b != null && o.b.equals(list.get(i)))
				{
					if(!zacetek)
					{
						s = counter;
						zacetek = true;
					}
					else
						e = counter;
				}
				if(o.c != null && o.c.equals(list.get(i)))
				{
					if(!zacetek)
					{
						s = counter;
						zacetek = true;
					}
					else
						e = counter;
				}
				counter++;
			}
			begin.add(s);
			end.add(e);
		}

		for(int i = 0; i < list.size(); i++)
		{
			graph.add(list.get(i));
			graph2.add(list.get(i));
			for(int j = 0; j < list.size(); j++){
				if(i != j){
					if( (begin.get(j) < end.get(i) && begin.get(j) >= begin.get(i)) || (begin.get(i) < end.get(j) && begin.get(i) >= begin.get(j)) )
					{
						graph.add(list.get(i), list.get(j));
						graph2.add(list.get(i), list.get(j));
					}
				}
			}
		}
	}
	
	private void simplify()
	{
		int sizeGraph = graph.neighbors.size();
		for(int i = 0; i < sizeGraph; i++)
		{
			for(Iterator<Entry<String, List<Edge<String>>>> it = graph.neighbors.entrySet().iterator(); it.hasNext(); ) 
			{
				Entry<String, List<Edge<String>>> entry = it.next();
				String v = entry.getKey();
				int size = graph.neighbors.get(v).size();
				if(size < K)
				{
					for(String v2 : graph.neighbors.keySet())
					{
						if(v.equals(v2))
							continue;
						else
						{
							int index = 0;
							for(Edge<String> edge : graph.neighbors.get(v2))
							{
								 
								if((edge.vertex).equals(v))
								{
									List<Edge<String>> a = graph.neighbors.get(v2);
									a.remove(index);
									graph.neighbors.put(v2, a);
									break;
								}
								index++;
							}
						}
					}
					sklad.push(v);
					it.remove();
				}
			}
		}
		if(graph.neighbors.size() > 0)
			spill();
	}
	
	private int returnSmallest(){
		  int min = 999;
		  for (String g : graph.neighbors.keySet()) {
		   int j = Integer.parseInt(g.substring(1));
		   if(j < min)
		    min = j;
		  }  
		  return min;  
	}
	
	private void  spill()
	{
		if(graph == null)
			return;
		String v = "T"+returnSmallest();
		sklad.push(v);
		for(Iterator<Entry<String, List<Edge<String>>>> it = graph.neighbors.entrySet().iterator(); it.hasNext(); ) 
		{
			Entry<String, List<Edge<String>>> entry = it.next();
			String v3 = entry.getKey();
			if(v3.equals(v))
			{
				for(String v2 : graph.neighbors.keySet())
				{
					if(v.equals(v2))
						continue;
					else
					{
						int index = 0;
						for(Edge<String> edge : graph.neighbors.get(v2))
						{
							 
							if((edge.vertex).equals(v))
							{
								List<Edge<String>> a = graph.neighbors.get(v2);
								a.remove(index);
								graph.neighbors.put(v2, a);
								break;
							}
							index++;
						}
					}
				}
			it.remove();
			break;
			}
			
		}
		if(graph.neighbors.size() > 0)
			simplify();
	}
	
	private void correct(String s) {
		((CodeFragment)Fragment).frame.tmpVarsSize += 8;
		int en;
		Operator opr;
		for(int i = 0; i < temp.size(); i++){
			en = i;
			opr = temp.get(i);
			if (opr.a != null && opr.a.equals(s)) {				 
				 String t1 = "T" +  TEMP.newTempName();
				 list.add(t1);
				 opr.a = t1;
				 
				 String t2 = "T" +  TEMP.newTempName();
				 list.add(t2);
				 
				 temp.add(i + 1,new Operator("STO %s,FP,%s", t1, t2));
				 temp.add(i + 1,new Operator("NEG %s,0," + (((CodeFragment)Fragment).frame.tmpVarsSize + ((CodeFragment)Fragment).frame.locVarsSize + 16), t2));
                 en += 1;				 
			}
			
			if ((opr.b != null && opr.b.equals(s)) ||  (opr.c != null && opr.c.equals(s))) {
				 String t1 = "T" +  TEMP.newTempName();
				 list.add(t1);
				 
				 String t2 = "T" +  TEMP.newTempName();
				 list.add(t2);
				 
				 if (opr.b != null && opr.b.equals(s))
					 opr.b = t1;				 
				 
				 if (opr.c != null && opr.c.equals(s))
					 opr.c = t1;				 
                
				 //System.out.println((((CodeFragment)Fragment).frame.tmpVarsSize + ((CodeFragment)Fragment).frame.locVarsSize + 16));
				 temp.add(i, new Operator("LDO %s,FP,%s", t1,t2));
				 temp.add(i, new Operator("NEG %s,0," + (((CodeFragment)Fragment).frame.tmpVarsSize + ((CodeFragment)Fragment).frame.locVarsSize + 16), t2));
                en += 1;
            }
            i = en;			
		}
	}
	
	
	
	private void execute(CodeFragment codeFrg) {
		if (codeFrg == null)
			return;  
		execute(codeFrg.linCode.stmts()); 
	}
	
	private void prolog()
	{
		String reg_01 = "$5";
		String reg_02 = "$6";
		Operator oper = new Operator("\tADD %s,FP,0", reg_01);
		oper.izpis1();
		oper = new Operator("\tADD FP,SP,0");
		oper.izpis0();
		String frame_size = "" + ((CodeFragment)Fragment).frame.size;
		oper = new Operator("\tSETL %s,%s",reg_02, frame_size);
		oper.izpis2();
		oper = new Operator("\tSUB SP,SP,%s", reg_02);
		oper.izpis1();
		String oldFPoffset = "" + (((CodeFragment)Fragment).frame.locVarsSize + 8);
		oper = new Operator("\tSETL %s,%s",reg_02, oldFPoffset);
		oper.izpis2();
		oper = new Operator("\tSUB %s,FP,%s",reg_02,reg_02);
		oper.izpis2();
		oper = new Operator("\tSTO %s,%s,0",reg_01,reg_02);
		oper.izpis2();
		oper = new Operator("\tGET %s,rJ",reg_01);
		oper.izpis1();
		oper = new Operator("\tSUB %s,%s,8",reg_02,reg_02);
		oper.izpis2();
		oper = new Operator("\tSTO %s,%s,0",reg_01,reg_02);
		oper.izpis2();
	}
	
	private void epilog()
	{
		String reg_01 = "$5";
		String reg_02 = "$6";
		Operator oper = new Operator("\tSTO %s,FP,0",reg_01);
		oper.izpis1();
		String oldFPoffset = "" + (((CodeFragment)Fragment).frame.locVarsSize + 8);
		oper = new Operator("\tSETL %s,%s",reg_02, oldFPoffset);
		oper.izpis2();
		oper = new Operator("\tSUB %s,FP,%s",reg_02,reg_02);
		oper.izpis2();
		oper = new Operator("\tLDO %s,%s,0",reg_01,reg_02);
		oper.izpis2();
		oper = new Operator("\tSUB %s,%s,8",reg_02,reg_02);
		oper.izpis2();
		oper = new Operator("\tLDO %s,%s,0",reg_02,reg_02);
		oper.izpis2();
		oper = new Operator("\tPUT rJ,%s",reg_02);
		oper.izpis1();
		oper = new Operator("\tADD SP,FP,0");
		oper.izpis0();
		oper = new Operator("\tADD FP,%s,0",reg_01);
		oper.izpis1();
		if(!((CodeFragment)Fragment).label.equals("_"))
		{
			oper = new Operator("\tPOP 0,0");
			oper.izpis0();
		}
		
	}

	private void execute(Vector<IMCStmt> stmts) {
		int pc = 0;
		while (true) {
			if (pc >= stmts.size())
				return;
			IMCStmt stmt = stmts.get(pc);
			
			if (stmt instanceof LABEL)
			{
				Operator label = new Operator(((LABEL) stmt).label + "\tSWYM");
				//label.izpis0();
				temp.add(label);
				pc++;
				continue;
			}
			if (stmt instanceof CJUMP) {
				
				String a = execute(((CJUMP) stmt).cond);
				
				String b = ((CJUMP) stmt).posLabel;
				Operator pos = new Operator("BNZ %s,%s", a, b);
				//pos.izpis2();
				temp.add(pos);
				
				String c = ((CJUMP) stmt).negLabel;
				TEMP tmp = new TEMP(TEMP.newTempName());
				String r = "T" + tmp.name;
				Operator neg = new Operator("BZ %s,%s",r, c);
				//neg.izpis1();
				temp.add(neg);
				list.add(r);
				
				pc++;
				continue;
			}
			if (stmt instanceof MOVE) {
				String a = "";
				String b = "";
				if (((MOVE) stmt).dst instanceof TEMP) {
					b = execute(((MOVE) stmt).src);
					a = "T" + ((TEMP) (((MOVE) stmt).dst)).name;
					if(!list.contains(a))
						list.add(a);
					Operator oper = new Operator("ADD %s,%s,0", a, b);
					//oper.izpis2();
					temp.add(oper);
				}
				if (((MOVE) stmt).dst instanceof MEM) {
					a = execute(((MEM) (((MOVE) stmt).dst)).addr);
					b = execute(((MOVE) stmt).src);
					Operator oper = new Operator("STO %s,%s,0", b, a);
					//oper.izpis2();
					temp.add(oper);
				}
				pc++;
				continue;
		  	}

			if (stmt instanceof JUMP) {

				String dest = ((JUMP) stmt).label;
				
				Operator oper = new Operator("JMP %s", dest);
				//oper.izpis1();
				temp.add(oper);
				
				pc++;
				continue;
			}
			pc++;
		}
	}
	  
	private String execute(IMCExpr expr) {
		
		if (expr instanceof CONST) {
			TEMP tmp = new TEMP(TEMP.newTempName());
			String r = "T" + tmp.name;
			if(((CONST) expr).value < 0)
			{
				Operator oper = new Operator("SETL %s,%s", r, (((CONST) expr).value)*(-1) + "");
				temp.add(oper);
				list.add(r);
				oper = new Operator("NEG %s,0,%s", r, r);
				temp.add(oper);
			}
			else
			{
				String a = ""+((CONST) expr).value;
				Operator oper = new Operator("SETL %s,%s", r, a);
				//oper.izpis2();
				temp.add(oper);
				list.add(r);
			}
			return r;
		}  

		else if (expr instanceof BINOP) {
			String a = execute(((BINOP) expr).expr1);
			String b = execute(((BINOP) expr).expr2);  
			
			TEMP tmp = new TEMP(TEMP.newTempName());
			String r = "T" + tmp.name;
			
			list.add(r);

			TEMP t = new TEMP(TEMP.newTempName());
			String rez = "T" + t.name;
			
			Operator oper;
			switch (((BINOP) expr).oper) {
			case OR:
				oper = new Operator("OR %s,%s,%s", r, a, b);
				//oper.izpis3();
				temp.add(oper);
				return r;
			case AND:
				oper = new Operator("AND %s,%s,%s", r, a, b);
				//oper.izpis3();
				temp.add(oper);
				return r;
			case EQU:
				oper = new Operator("CMP %s,%s,%s", r, a, b);
				//oper.izpis3();
				temp.add(oper);
				
				t = new TEMP(TEMP.newTempName());
				rez = "T" + t.name;
				oper = new Operator("ZSZ %s,%s,1", rez, r);
				//oper.izpis2();
				temp.add(oper);
				
				list.add(rez);
				return rez;
			case NEQ:
				oper = new Operator("CMP %s,%s,%s", r, a, b);
				//oper.izpis3();
				temp.add(oper);
				
				t = new TEMP(TEMP.newTempName());
				rez = "T" + t.name;
				oper = new Operator("ZSNZ %s,%s,1", rez, r);
				//oper.izpis2();
				temp.add(oper);
				
				list.add(rez);
				return rez;
			case LTH:
				oper = new Operator("CMP %s,%s,%s", r, a, b);
				//oper.izpis3();
				temp.add(oper);
				
				oper = new Operator("ADD %s,%s,1", r, r); 
				//oper.izpis2();
				temp.add(oper);
				
				t = new TEMP(TEMP.newTempName());
				rez = "T" + t.name;
				oper = new Operator("ZSZ %s,%s,1", rez, r);
				//oper.izpis2();
				temp.add(oper);
				
				list.add(rez);
				return rez;
			case GTH:
				oper = new Operator("CMP %s,%s,%s", r, a, b); 
				//oper.izpis2();
				temp.add(oper);
				
				t = new TEMP(TEMP.newTempName());
				rez = "T" + t.name;
				oper = new Operator("AND %s,%s,1", rez, r);
				//oper.izpis2();
				temp.add(oper);

				list.add(rez);
				return rez;
				
			case LEQ:
				oper = new Operator("CMP %s,%s,%s", r, a, b);
				//oper.izpis3();
				temp.add(oper);
				
				t = new TEMP(TEMP.newTempName());
				rez = "T" + t.name;
				oper = new Operator("ZSNP %s,%s,1", rez, r);
				//oper.izpis2();
				temp.add(oper);

				list.add(rez);
				return rez;
			case GEQ:
				oper = new Operator("CMP %s,%s,%s", r, a, b);
				//oper.izpis3();
				temp.add(oper);
				
				t = new TEMP(TEMP.newTempName());
				rez = "T" + t.name;
				oper = new Operator("ZSNN %s,%s,1", rez, r);
				//oper.izpis2();
				temp.add(oper);

				list.add(rez);
				return rez;
			case ADD:
				oper = new Operator("ADD %s,%s,%s", r, a, b);
				//oper.izpis3();
				temp.add(oper);
				return r;
			case SUB:
				oper = new Operator("SUB %s,%s,%s", r, a, b);
				//oper.izpis3();
				temp.add(oper);
				return r;
			case MUL:
				oper = new Operator("MUL %s,%s,%s", r, a, b);
				//oper.izpis3();
				temp.add(oper);
				return r;
			case DIV:
				oper = new Operator("DIV %s,%s,%s", r, a, b);
				//oper.izpis3();
				temp.add(oper);
				return r;
			case MOD:
				oper = new Operator("DIV %s,%s,%s", r, a, b);
				//oper.izpis3();
				temp.add(oper);
				
				t = new TEMP(TEMP.newTempName());
				rez = "T" + t.name;
				oper = new Operator("MUL %s,%s,%s", rez, r, b);
				//oper.izpis3();
				temp.add(oper);
				
				list.add(rez);
				
				TEMP t2 = new TEMP(TEMP.newTempName());
				String rezultat = "T" + t2.name;
				oper = new Operator("SUB %s,%s,%s", rezultat, a, rez);
				//oper.izpis3();
				temp.add(oper);

				list.add(rezultat);
				return rezultat;
			default:
				return "";
			}
		}

		else if (expr instanceof CALL) {
			
			CALL call = (CALL) expr;  
			
			long d = 0;
			Operator oper = new Operator("STO %s,%s,%s", "FP", "SP", "" + d);
			temp.add(oper);
			d += 8;
			for (int arg = 1; arg < call.numArgs(); arg++) {
				String value = execute(call.args(arg));
				oper = new Operator("STO %s,%s,%s", value, "SP", "" + d);
				//oper.izpis3();
				temp.add(oper);
				d += call.widths(arg);
				//System.out.println(d);
			}	
			
			oper = new Operator("PUSHJ %s,%s", "" + K, call.label);
			//oper.izpis1();
			temp.add(oper);
			
			TEMP tmp = new TEMP(TEMP.newTempName());
			String r = "T" + tmp.name;
			list.add(r);
			oper = new Operator("LDO %s,%s,0", r, "SP");
			//oper.izpis2();
			temp.add(oper);
			return r;
		}

		else if (expr instanceof MEM) {
			String a = execute(((MEM) expr).addr);
			TEMP tmp = new TEMP(TEMP.newTempName());
			String r = "T" + tmp.name;
			list.add(r);
			Operator oper = new Operator("LDO %s,%s,0", r, a);
			//oper.izpis2();
			temp.add(oper);
			return r;
		}

		else if (expr instanceof NAME) {
			TEMP tmp = new TEMP(TEMP.newTempName());
			String r = "T" + tmp.name;
			
			list.add(r);
			String a = ((NAME) expr).name;
			Operator oper = new Operator("LDA %s,%s", r, a);
			//oper.izpis2();
			temp.add(oper);
			return r;
		}

		else if (expr instanceof NOP) {
			TEMP tmp = new TEMP(TEMP.newTempName());
			String r = "T" + tmp.name;
			list.add(r);
			Operator oper = new Operator("SET %s,0", r);
			//oper.izpis1();
			temp.add(oper);
			//System.out.println("SWYM"); //TODO
			return r;
		}

		else if (expr instanceof TEMP) {
			String a = "T"+((CodeFragment)Fragment).FP;
			String r = "T" + ((TEMP) expr).name;
			if(a.equals(r))
			{
				return "FP";
			}
			if(!list.contains(r))
				list.add(r);
			return r;
		}

		else if (expr instanceof UNOP) {
			String a = execute(((UNOP) expr).expr);

			TEMP tmp = new TEMP(TEMP.newTempName());
			String r = "T" + tmp.name;
			list.add(r);
			
			Operator oper;
			switch (((UNOP) expr).oper) {
			case ADD:
				oper = new Operator("ADD %s,%s,0", r, a);
				temp.add(oper);
				//oper.izpis2();
				return r;
			case SUB:
				oper = new Operator("NEG %s,0,%s", r, a);
				temp.add(oper);
				//oper.izpis2();
				return r;
			case NOT:
				oper = new Operator("XOR %s,%s,1", r, a); 
				temp.add(oper);
				//oper.izpis2();
				return r;
			default:
				return "";
			}
			
		}
		throw new InternalCompilerError();
	}
}