package net.bioclipse.seneca.job;

import net.bioclipse.seneca.structgen.AnnealingLogPane;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class StochasticJobMonitor extends Composite implements IProgressMonitor {

	private boolean isCancelled;
	private String taskName;
	private String subTaskName;

	private AnnealingLogPane logPane;
	private ProgressIndicator progress;

	private static final int PREF_WIDTH = 300;
	private static final int PREF_HEIGHT = 300;

	public StochasticJobMonitor(Composite parent, int maxT, int maxS) {
		super(parent, SWT.NO_SCROLL);

		this.isCancelled = false;

		GridLayout l = new GridLayout();
		l.numColumns = 1;
		l.makeColumnsEqualWidth = true;
		l.marginBottom = 5;
		l.marginLeft = 5;
		l.marginRight = 5;
		l.marginTop = 5;
		setLayout(l);

		this.logPane = new AnnealingLogPane(this, maxT, maxS);
		this.progress = new ProgressIndicator(this, SWT.HORIZONTAL);

		this.pack();
	}

	public Point computeSize(int wHint, int hHint, boolean changed) {
		// ignore the hints, and provide a size
		return new Point(PREF_WIDTH, PREF_HEIGHT);
	}

	public void beginTask(String taskName, int totalWork) {
		if (totalWork == IProgressMonitor.UNKNOWN || totalWork == 0) {
			this.progress.beginAnimatedTask();
		} else {
			this.progress.beginTask(totalWork);
		}
		this.taskName = taskName;
	}

	public void done() {
		this.progress.sendRemainingWork();
		this.progress.done();
	}

	public void internalWorked(double work) {
		this.progress.worked(work);
	}

	public boolean isCanceled() {
		return this.isCancelled;
	}

	public void setCanceled(boolean value) {
		this.isCancelled = true;
	}

	public void setTaskName(String name) {
		// TODO Auto-generated method stub

	}

	public void subTask(String name) {
		// TODO Auto-generated method stub

	}

	public void worked(int work) {
		this.internalWorked(work);
	}

	public void addPoint(int i) {
		this.logPane.addPoint(i);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final Display display = new Display();

		final org.eclipse.swt.widgets.Shell shell
			= new org.eclipse.swt.widgets.Shell(display);


		final int maxT = 100;
		final int maxS = 50;
		final StochasticJobMonitor monitor
			=  new StochasticJobMonitor(shell, maxT, maxS);
		shell.pack();
		shell.open();

		Thread t = new Thread() {
			public void run() {
				display.asyncExec(new Runnable() {
					public void run() {
						monitor.beginTask("test", maxS);
					}
				});
				java.util.Random r = new java.util.Random();
				for (int temp = maxT; temp > 0; temp -= r.nextInt(5)) {
					final int i = temp;
					display.asyncExec(new Runnable() {
						public void run() {
							monitor.addPoint(i);
						}
					});
					try {
						sleep(100);
					} catch (InterruptedException e) {

					}
					display.asyncExec(new Runnable() {
						public void run() {
							monitor.worked(1);
						}
					});
				}
				display.asyncExec(new Runnable() {
					public void run() {
						monitor.done();
					}
				});

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
