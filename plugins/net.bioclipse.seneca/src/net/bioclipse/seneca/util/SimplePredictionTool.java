package net.bioclipse.seneca.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
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
public class SimplePredictionTool {

  private static HashMap mapsmap = new HashMap(460000);

  /**
   *Constructor for the PredictionTool object
   *
   * @exception  IOException  Problems reading the HOSE code file.
   */
  public SimplePredictionTool() throws IOException {
	  float av;
	String filename = "nmrshiftdb.csv";
    InputStream ins = this.getClass().getClassLoader().getResourceAsStream(filename);
    BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
    String input;
    int counter = 0;
    while ((input = reader.readLine()) != null) {
        StringTokenizer st2 = new StringTokenizer(input, "|");
        String symbol = st2.nextToken();
        String code = st2.nextToken();
        //if (code.equals("C-3;=CCO(,C,&/")) System.out.println(code.hashCode());
        st2.nextToken();
        av = Float.parseFloat(st2.nextToken());
        st2.nextToken();
        if (mapsmap.get(symbol) == null) {
          mapsmap.put(symbol, new HashMap(5));
        }
        ((HashMap) mapsmap.get(symbol)).put(code.hashCode(),new Float(av));
        counter++;
        if (counter == 10000) {
        	System.out.print("x");
        	counter = 0;
        }
    }
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
  public static float generalPredict(IAtomContainer mol, IAtom a, boolean calculated, boolean measured, int ignoreSpectrum,
		  int ignoreSpectrumEnd, StringBuffer comment, boolean commentWithMinMax, boolean withRange, Map predictionValuesForApplet, int maxSpheresToUse, boolean cache, StringBuffer hoseCodeOut, int spheresMax, boolean fromDB, boolean trueonly) throws Exception {
	  HOSECodeGenerator hcg = new HOSECodeGenerator();
//	  double[] returnValues = new double[3];
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

		  Float shift = (Float) ((HashMap) mapsmap.get(a.getSymbol())).get(hoseCode.hashCode());
		  if (shift != null)
		  {
			  return shift.floatValue();
		  }
	  }
	  return -1;
  }


  /**
   *  Does a prediction.
   *
   * @param  mol            The molecule.
   * @param  atom           The atom for which to predict. Typ of spectrum corresponds to element of this atom.
   * @return                An array of doubles. Meaning: 0=lower limit, 1=mean, 2=upper limit calculated via confidence limits, 3=median, 4=used spheres, 5=number of values, 6=standard deviation, 7=min value, 8=max value
   * @exception  Exception  Description of Exception
   */
  public float predict(IAtomContainer mol, IAtom atom) throws Exception {
    return generalPredict(mol, atom, true, true, -1, -1, new StringBuffer(), false, true, null, 6, false, null, 6, false, true);
  }

}

