package net.bioclipse.seneca.structgen;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * @author maclean
 *
 * Displays a graph of the change in temperature as the Annealing proceeds.
 */
public class AnnealingLogPane extends Canvas implements PaintListener {

	private AnnealingGraph graph;
	private static final int TICK_LENGTH = 3;
	private static final int BORDER_WIDTH = 5;
	private static final int PREF_WIDTH = 300;
	private static final int PREF_HEIGHT = 200;
	private Color gridColor;
	private Color backgroundColor;
	private int xScale;
	private int yScale;

	public AnnealingLogPane(Composite parent, int maxT, int maxS) {
		super(parent, SWT.NO_SCROLL | SWT.NO_BACKGROUND | SWT.NO_FOCUS);

		this.setBackground(getDisplay().getSystemColor(SWT.COLOR_GRAY));
		this.addPaintListener(this);

		this.graph = new AnnealingGraph(maxT, maxS, PREF_WIDTH, PREF_HEIGHT);

		// TODO : get the scale from the AnnealingLog...
		this.xScale = 20;
		this.yScale = 20;

		Display display = getDisplay();
		this.backgroundColor = display.getSystemColor(SWT.COLOR_GRAY);
		this.gridColor = display.getSystemColor(SWT.COLOR_DARK_GRAY);
	}

	public Point computeSize(int wHint, int hHint, boolean changed) {
		// ignore the hints, and provide a size
		return new Point(PREF_WIDTH, PREF_HEIGHT);
	}

	public void paintControl(PaintEvent e) {
		Image image = paintToImage();
		e.gc.drawImage(image, 0, 0);
		image.dispose();
	}

	public Image paintToImage() {
		Display display = getDisplay();
		Rectangle r = getBounds();

		Image image = new Image(display, r);
		GC gc = new GC(image);
		gc.setBackground(this.backgroundColor);
		gc.fillRectangle(r);
		gc.setForeground(this.gridColor);
		gc.drawRectangle(BORDER_WIDTH, BORDER_WIDTH,
				r.width - 2 * BORDER_WIDTH, r.height - 2 * BORDER_WIDTH);

		// draw ticks and grid
		int lowerline = PREF_HEIGHT - BORDER_WIDTH + TICK_LENGTH;
		for (int w = BORDER_WIDTH; w < PREF_WIDTH; w += xScale) {
			gc.drawLine(w, BORDER_WIDTH, w, lowerline);
		}

		int leftline = BORDER_WIDTH - TICK_LENGTH;
		for (int h = BORDER_WIDTH; h < PREF_WIDTH; h += yScale) {
			gc.drawLine(leftline, h, PREF_WIDTH - BORDER_WIDTH, h);
		}

		// draw the graph lines
		AnnealingGraph.Point last = graph.createPoint(0, 0);
		for (AnnealingGraph.Point p : graph) {
			// TODO : fix the range, and re-use Colors
			int h = PREF_HEIGHT * (PREF_WIDTH / PREF_HEIGHT);
			Color currentColor = colorRamp(display, h - p.x, 0, h);
			gc.setForeground(currentColor);
			gc.drawLine(last.x + BORDER_WIDTH, last.y + BORDER_WIDTH,
						p.x + BORDER_WIDTH, p.y + BORDER_WIDTH);
			last = p;
			currentColor.dispose();
		}
		gc.dispose();
		return image;
	}

	public void addPoint(int x) {
		this.graph.addData(x);
		this.redraw();
	}

	// TODO : move into the AnnealingLog class
	private Color colorRamp(Display display,
			double v, double vmin, double vmax) {
		if (v < vmin) v = vmin;
		if (v > vmax) v = vmax;
		double dv = vmax - vmin;

		try {
			double r = 1.0;
			double g = 1.0;
			double b = 1.0;

			if (v < (vmin + 0.25 * dv)) {
				r = 0.0;
				g = 4 * (v - vmin) / dv;
			} else if (v < (vmin + 0.5 * dv)) {
				r = 0.0;
				b = 1.0 + 4.0 * (vmin + 0.25 * dv - v) / dv;
			} else if (v  <(vmin + 0.75 * dv)) {
				r = 4.0 * (v - vmin - 0.5 * dv) / dv;
				b = 0.0;
			} else {
				g = 1.0 + 4.0 * (vmin + 0.75 * dv - v) / dv;
				b = 0.0;
			}

			return makeColor(display, r, g, b);
		} catch (ArithmeticException a) {
			// divide-by-zero errors
			return display.getSystemColor(SWT.COLOR_BLACK);
		} catch (IllegalArgumentException ie) {
			return display.getSystemColor(SWT.COLOR_BLACK);
		}
	}

	// XXX this is a separate function in case
	private Color makeColor(Display display, double r, double g, double b) {
		return new Color(display,
						 (int)(r * 255),
						 (int)(g * 255),
						 (int)(b * 255));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final Display display = new Display();

		final org.eclipse.swt.widgets.Shell shell
			= new org.eclipse.swt.widgets.Shell(display);

		shell.setLayout(new FillLayout());
		final int maxT = 100;
		final int maxS = 50;
		final AnnealingLogPane pane = new AnnealingLogPane(shell, maxT, maxS);

		shell.pack();
		shell.open();
		Thread t = new Thread() {
			public void run() {
				java.util.Random r = new java.util.Random();
				for (int temp = maxT; temp > 0; temp -= r.nextInt(5)) {
					final int i = temp;
					display.asyncExec(new Runnable() {
						public void run() {
							pane.addPoint(i);
						}
					});
					try {
						sleep(100);
					} catch (InterruptedException e) {

					}
				}
			}
		};
		t.start();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}
}
