package net.bioclipse.seneca.structgen;

import java.util.ArrayList;
import java.util.Iterator;

public class AnnealingGraph implements Iterable<AnnealingGraph.Point> {

	public final class Point {
		public final int x;
		public final int y;

		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public String toString() {
			return "[" + this.x + ", " + this.y + "]";
		}
	}

	private ArrayList<AnnealingGraph.Point> points;
	private int maxT;
	private int currentStep;
	private float scaleX;
	private float scaleY;

	public AnnealingGraph(int maxT, int maxSteps, int width, int height) {
		this.points = new ArrayList<AnnealingGraph.Point>();
		this.maxT = maxT;
		this.currentStep = 0;
		this.scaleX = (float)width / (float)maxSteps ;
		this.scaleY = (float)height / (float)maxT ;
	}

	public AnnealingGraph.Point createPoint(int x, int y) {
		return this.new Point(x, y);
	}

	public AnnealingGraph.Point getLastPoint() {
		return this.points.get(this.points.size() - 1);
	}

	public int size() {
		return this.points.size();
	}

	public void addData(int temperature) {
		assert temperature <= this.maxT && temperature > 0;

		// TODO : what if the x is < 1?
		// TODO : what if the currentStep is > maxSteps?
		int x = (int)(this.currentStep *  this.scaleX);
		int y = (int)((this.maxT - temperature) * this.scaleY);

		this.points.add(new AnnealingGraph.Point(x, y));
		this.currentStep++;
	}

	public Iterator<Point> iterator() {
		return this.points.iterator();
	}

	public static void main(String[] args) {
		AnnealingGraph g = new AnnealingGraph(1000, 100, 200, 100);
		g.addData(999);
		g.addData(996);
		g.addData(991);

		for (AnnealingGraph.Point p : g) {
			System.out.println(p);
		}
	}

}
