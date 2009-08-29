package net.bioclipse.seneca.util;

import java.util.ArrayList;
import java.util.List;

public class SpectrumComparator {
	
	public float getSimilarity(List spectrum1, List spectrum2)
	{
		ArrayList spec1 = new ArrayList(spectrum1);
		ArrayList spec2 = new ArrayList(spectrum2);
		float simSum = 100/spectrum1.size();
		float similarity = 0;
		Float testsignal = null;
		Float bestfit = null;
		Float thisSignal = null;
		float diff, testdiff;
		//System.out.println("simSum: " + simSum);
		do{
			testsignal = (Float)spec1.get(0);
			diff = 100000;
			testdiff = 10000;
			//System.out.println("testsignal.floatValue(): " + testsignal.floatValue());
			for (int f = 0; f < spec2.size(); f++)
			{
				thisSignal = (Float)spec2.get(f);
				//System.out.println("thisSignal.floatValue(): " + thisSignal.floatValue());
				testdiff = Math.abs(thisSignal.floatValue() - testsignal.floatValue());
				if (testdiff < diff)
				{
					diff = testdiff;
					bestfit = thisSignal;
				}
			}
			//System.out.println("diff: " + diff);
			if (diff < simSum) similarity += simSum - diff;
			spec2.remove(bestfit);
			spec1.remove(testsignal);
		}
		while(spec2.size() > 0);
		return similarity/100;
	}

}
