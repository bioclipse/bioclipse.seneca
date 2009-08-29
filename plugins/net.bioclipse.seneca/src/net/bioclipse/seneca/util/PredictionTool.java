/* HOSECodeJudge.java
 *
 * Copyright (C) 2006  Stefan Kuhn
 *
 * Contact: c.steinbeck@uni-koeln.de
 *
 * This software is published and distributed under artistic license.
 * The intent of this license is to state the conditions under which this Package
 * may be copied, such that the Copyright Holder maintains some semblance
 * of artistic control over the development of the package, while giving the
 * users of the package the right to use and distribute the Package in a
 * more-or-less customary fashion, plus the right to make reasonable modifications.
 *
 * THIS PACKAGE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF MERCHANTIBILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE.
 *
 * The complete text of the license can be found in a file called LICENSE
 * accompanying this package.
 */
package net.bioclipse.seneca.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.HOSECodeAnalyser;
import org.openscience.cdk.tools.HOSECodeGenerator;

/**
 *  This class offers a stand-alone prediction based on HOSE codes from NMRShiftDB. Apart from this class, you need a data dump.
 * You can get a jar containing class and dump from any nmrshiftdb server with a URL like http://servername/download/NmrshiftdbServlet/predictor.jar?nmrshiftdbaction=predictor.
 * For an example how to use this class, see the main method. For running this class you need the following additional jars: cdk-core.jar, cdk-extra.jar, JNL.jar.
 * Since the HOSE code table is kept in memory, this class needs a lot of memory depending on the size of the database. We recommand to run the JVM with at least 128 MB memory.
 * Also always set all references to this class to null if you no longer need it in order to have it removed by garbage collection.
 *
 * @author     shk3
 * @created    September 23, 2004
 */
public class PredictionTool {

  private static HashMap mapsmap = new HashMap();

  /**
   *Constructor for the PredictionTool object
   *
   * @exception  IOException  Problems reading the HOSE code file.
   */
  public PredictionTool(List<String> symbols) throws IOException {
	String filename = "nmrshiftdb.csv";
    InputStream ins = this.getClass().getClassLoader().getResourceAsStream(filename);
    BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
    String input;
    int counter = 0;
    int stored = 0;
    int found = 0;
    while ((input = reader.readLine()) != null) {
        StringTokenizer st2 = new StringTokenizer(input, "|");
        String symbol = st2.nextToken();
        String code = st2.nextToken();
        if (applicable(code, symbols)) {
        	float min = Float.parseFloat(st2.nextToken());
        	float av = Float.parseFloat(st2.nextToken());
        	float max = Float.parseFloat(st2.nextToken());
        	if (mapsmap.get(symbol) == null) {
        		mapsmap.put(symbol, new HashMap(5));
        	}
        	((HashMap) mapsmap.get(symbol)).put(code,new ValueBean(min,av,max));
        	stored++;
        }
    	counter++;
    	found++;
    	if (counter == 10000) {
    		System.out.print("x");
    		counter = 0;
    	}
    }
    // TODO : log
//    BioclipseConsole.writeToConsole("Using " + stored + "/" + found + " HOSE codes in the PredictionTool.");
  }

  private boolean applicable(String code, List<String> symbols) {
	List<String> codeSymbols = HOSECodeAnalyser.getElements(code);
	for (String codeSymbol : codeSymbols) {
		if (!symbols.contains(codeSymbol)) return false;
	}
	return true;
}

/**
   *  This method does a prediction, either from the database or from the mapsmap initialized in the constructor. This should not be used directly when using the stand-alone predictor; use predict() then.
   *
   * @param  comment                    Contains additional text after processing predictRange().
   * @param  mol                        The molecule the atoms comes from.
   * @param  a                          The atom the shift of which to be predicted.
   * @param  commentWithMinMax          Shall min/max values be included in comments.
   * @param  ignoreSpectrum             A molecule to be ignored in the prediction (null=none).
   * @param  withRange                  Is the range to be calculated as well ? (use only when needed for performance reasons).
   * @param  calculated                 Use calculated spectra.
   * @param  measured                   Use measured spectra.
   * @param  runData                    The current runData object.
   * @param  predictionValuesForApplet  Will become the String to diplay the histogram in the applet, null if not wished.
   * @param  maxSpheresToUse            Restrict number of spheres to use, to use max spheres set -1.
   * @param  cache                      true=Use HOSE_CODES table, false=do join query.
   * @param  hoseCodeOut                Contains the used HOSE_CODE.
   * @param  spheresMax                 Default maximum spheres to use.
   * @param  fromDB                     Do prediction from db or mapsmap?
   * @return                            An array of doubles. Meaning: 0=lower limit, 1=mean, 2=upper limit calculated via confidence limits
   * @exception  Exception              Database problems.
   */
  public static double[] generalPredict(IAtomContainer mol, IAtom a, boolean calculated, boolean measured, int ignoreSpectrum,
		  int ignoreSpectrumEnd, StringBuffer comment, boolean commentWithMinMax, boolean withRange, Map predictionValuesForApplet, int maxSpheresToUse, boolean cache, StringBuffer hoseCodeOut, int spheresMax, boolean fromDB, boolean trueonly) throws Exception {
	  HOSECodeGenerator hcg = new HOSECodeGenerator();
	  double[] returnValues = new double[3];
	  int spheres;
	  for (spheres = maxSpheresToUse; spheres > 0; spheres--) {
		  StringBuffer hoseCodeBuffer = new StringBuffer();
		  StringTokenizer st = new StringTokenizer(hcg.getHOSECode(mol, a, maxSpheresToUse,false), "()/");
		  for (int k = 0; k < spheres; k++) {
			  if (st.hasMoreTokens()) {
				  String partcode = st.nextToken();
				  hoseCodeBuffer.append(partcode);
			  }
			  if (k == 0) {
				  hoseCodeBuffer.append("(");
			  } else if (k == 3) {
				  hoseCodeBuffer.append(")");
			  } else {
				  hoseCodeBuffer.append("/");
			  }
		  }
		  String hoseCode = hoseCodeBuffer.toString();
		  ValueBean l = ((ValueBean) ((HashMap) mapsmap.get(a.getSymbol())).get(hoseCode));
		  if (l != null) {
			  returnValues[0]=l.min;
			  returnValues[1]=l.average;
			  returnValues[2]=l.max;
			  return returnValues;
		  }
	  }
	  returnValues[0]=-1;
	  returnValues[1]=-1;
	  returnValues[2]=-1;
	  return returnValues;
  }


  /**
   *  Does a prediction.
   *
   * @param  mol            The molecule.
   * @param  atom           The atom for which to predict. Typ of spectrum corresponds to element of this atom.
   * @return                An array of doubles. Meaning: 0=lower limit, 1=mean, 2=upper limit calculated via confidence limits, 3=median, 4=used spheres, 5=number of values, 6=standard deviation, 7=min value, 8=max value
   * @exception  Exception  Description of Exception
   */
  public double[] predict(IAtomContainer mol, IAtom atom) throws Exception {
    return generalPredict(mol, atom, true, true, -1, -1, new StringBuffer(), false, true, null, 6, false, null, 6, false, true);
  }

  class ValueBean{
	  public float min;
	  public float average;
	  public float max;

	  public ValueBean(float a, float b, float c){
		  min=a;
		  average=b;
		  max=c;
	  }
  }
}

