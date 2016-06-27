package compiler.data.inst;

/**
 * @author juliette
 */
public  class Operator extends Instructions {
	
	public String a;
	
	public String b;
	
	public String c;

	public Operator(String oper) {
		super(oper);
	}
	
	public Operator(String oper, String a) {
		super(oper);
		this.a = a;
	}
	
	public Operator(String oper, String a, String b) {
		super(oper);
		this.a = a;
		this.b = b;
	}
	
	public Operator(String oper, String a, String b, String c) {
		super(oper);
		this.a = a;
		this.b = b;
		this.c = c;
	}
	
	public void izpis0()
	{

		System.out.printf("   " + oper);
		System.out.println();
	}
	
	public void izpis1()
	{

		System.out.printf("   " + oper, a);
		System.out.println();
	}
	
	public void izpis2()
	{
		System.out.printf("   " + oper, a, b);
		System.out.println();
	}
	
	public void izpis3()
	{
		System.out.printf("   " + oper, a, b, c);
		System.out.println();
	}
	
}
