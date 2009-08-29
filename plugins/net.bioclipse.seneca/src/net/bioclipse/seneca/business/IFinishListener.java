package net.bioclipse.seneca.business;

/*
 * This is used for being notified of jobs being finished.
 * Ugly hack. The BioclipseJob stuff should do that.
 */

public interface IFinishListener {

    public void finished();
}
