package net.bioclipse.seneca.business;

import net.bioclipse.seneca.editor.TemperatureAndScoreListener;


public interface IJavaSenecaManager extends ISenecaManager {

    public void addFinishListener(IFinishListener listener);
    public void addTempeatureAndScoreListener(TemperatureAndScoreListener listener);
}
