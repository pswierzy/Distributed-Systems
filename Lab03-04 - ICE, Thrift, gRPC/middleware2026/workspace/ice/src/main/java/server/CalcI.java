package server;

import Demo.A;
import Demo.Calc;
import Demo.EmptySequenceError;
import com.zeroc.Ice.Current;

public class CalcI implements Calc {
	private static final long serialVersionUID = -2448962912780867770L;
	long counter = 0;

	@Override
	public long add(int a, int b, Current __current) {
		System.out.println("ADD: a = " + a + ", b = " + b + ", result = " + (a + b));
        operation(a, b, __current);

        return a + b;
	}

	@Override
	public long subtract(int a, int b, Current __current) {
        System.out.println("SUBSTRACT: a = " + a + ", b = " + b + ", result = " + (a + b));
        operation(a, b, __current);

        return a - b;
	}

    private void operation(int a, int b, Current __current) {
        System.out.println("For the object " + __current.id.category + "/" + __current.id.name);

        if (a > 1000 || b > 1000) {
            try {
                Thread.sleep(6000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        if (!__current.ctx.isEmpty()) {
            System.out.println("There are some properties in the context");
        }
    }

    @Override
    public double avg(long[] seq, Current __current) throws EmptySequenceError {
        System.out.println("AVG");
        if (seq == null || seq.length == 0) {
            System.err.println("Error: Sequence is empty.");
            throw new EmptySequenceError();
        }
        long sum = 0;
        for (long val: seq) {
            sum += val;
        }
        double result = (double) sum / seq.length;
        System.out.println("AVG result = " + result);

        return result;
    }


    @Override
	public /*synchronized*/ void op(A a1, short b1, Current current) {
		System.out.println("OP" + (++counter));
		try {
			Thread.sleep(500);
		} catch (java.lang.InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
}