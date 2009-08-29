/*******************************************************************************
 * Copyright (c) 2005-2007 Bioclipse Project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ola Spjuth - core API and implementation
 *     Egon Willighagen - tuned for SENECA
 *******************************************************************************/
package net.bioclipse.seneca.views;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

import net.bioclipse.cdk.jchempaint.view.SWTFontManager;
import net.bioclipse.cdk.jchempaint.widgets.JChemPaintEditorWidget;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.FastScatterPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.Renderer;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicBondGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;

public class BestStructureView extends ViewPart {

    public static final String ID = 
        "net.bioclipse.seneca.views.BestStructureView";

	private Renderer renderer;
	private JChemPaintEditorWidget jcpwidget;

	private IMolecule bestMolecule = null;
	private Frame fileTableFrame;
	
	List<Double> temps = new ArrayList<Double>();
	List<Double> scores = new ArrayList<Double>();
  JFreeChart continuousChart;

    private int maxSteps;

	private final static StructureDiagramGenerator sdg =
	    new StructureDiagramGenerator();
	
	public BestStructureView() {
        List<IGenerator> generators = makeGenerators();
    	renderer = new Renderer(generators, new SWTFontManager(null));
    	renderer.getRenderer2DModel().setIsCompact(true);    	
    	renderer.getRenderer2DModel().setDrawNumbers(false);
  }

	public void createPartControl(Composite parent) {
	    SashForm sash = new SashForm(parent,SWT.VERTICAL);
    	jcpwidget = new JChemPaintEditorWidget(sash, SWT.PUSH );
      Composite contChartcomposite = new Composite(sash, SWT.EMBEDDED);
      FillLayout layout = new FillLayout(SWT.VERTICAL);
      contChartcomposite.setLayout(layout);
      fileTableFrame = SWT_AWT.new_Frame(contChartcomposite);
      
      String xAxisLabel = "Steps";
      String yAxisLabel = "Temperature/Score";
      String title = "Annealing progress";
      XYSeries series = new XYSeries("Signal");
      for (int i = 0; i < (maxSteps!=0 ? maxSteps : temps.size()); i++) {
          if(i<temps.size())
              series.add(i, temps.get( i ));
          else
              series.add(i, 0);
      }

      XYDataset xyDataset = new XYSeriesCollection(series);
      continuousChart = ChartFactory.createXYAreaChart(title, xAxisLabel,
          yAxisLabel, xyDataset, PlotOrientation.VERTICAL, false,
          true, false);
      continuousChart.setAntiAlias(false);

      XYPlot continuousPlot = continuousChart.getXYPlot();
      
      continuousPlot.setRenderer(new StandardXYItemRenderer());
      ChartPanel chart = new ChartPanel(continuousChart);
      fileTableFrame.add( chart );
      //fileTableFrame.validate();
      //fileTableFrame.repaint();

	}

	public void setFocus() {
		// nothing to do
	}

	public void setBestStructure(IMolecule molecule) {
		this.bestMolecule = molecule;
		this.updateView();
	}
	
	private List<IGenerator> makeGenerators() {
	    List<IGenerator> generators = new ArrayList<IGenerator>();
	    generators.add(new BasicBondGenerator());
	    generators.add(new BasicAtomGenerator());
	    return generators;
	}

	private void updateView() {
		try {
			sdg.setMolecule((IMolecule)bestMolecule.clone());
			sdg.generateCoordinates();
			this.bestMolecule = sdg.getMolecule();
			this.getViewSite().getShell().getDisplay().syncExec(
			                 new Runnable() {
			                   public void run(){
			                       jcpwidget.setAtomContainer( bestMolecule );
			                   }
			                 });
		} catch (Exception exception) {
			System.out.println("Error: " + exception.getMessage());
			exception.printStackTrace();
		}
	}
	
	public void updateViewTemp(){
      XYSeries series = new XYSeries("Temp");
      for (int i = 0; i < (maxSteps!=0 ? maxSteps : temps.size()); i++) {
          if(i<temps.size())
              series.add(i, temps.get( i ));
          else
              series.add(i, 0);
      }


    XYSeriesCollection xyDataset = new XYSeriesCollection(series);
    XYSeries scoreseries = new XYSeries("Score");
    for (int i = 0; i < (maxSteps!=0 ? maxSteps : temps.size()); i++) {
        if(i<temps.size())
            scoreseries.add(i, scores.get( i ));
        else
            scoreseries.add(i, 0);
    }
    xyDataset.addSeries( scoreseries );
    continuousChart.getXYPlot().setDataset( xyDataset );

	}
	
	public void addTemperatureValue(double temp, double score){
	    temps.add( temp );
	    scores.add( score );
	    updateViewTemp();
	}

  public void setMaxSteps( int maxSteps ) {
      this.maxSteps=maxSteps;
  }

  public void reset() {
      temps=new ArrayList<Double>();
      scores=new ArrayList<Double>();
      updateViewTemp();
  }
}
