/*******************************************************************************
 * Copyright (c) 2009  Stefan Kuhn <Stefan Kuhn@ebi.ac.uk>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * www.eclipse.org—epl-v10.html <http://www.eclipse.org/legal/epl-v10.html>
 *
 * Contact: http://www.bioclipse.net/
 ******************************************************************************/
package net.bioclipse.seneca.ga;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.structgen.SingleStructureRandomGenerator;
import org.uncommons.watchmaker.framework.factories.AbstractCandidateFactory;


/**
 * A molecule candidate factory for watchmaker, based on cdk SingleStructureRandomGenerator.
 *
 */
public class MoleculeCandidateFactory extends AbstractCandidateFactory<IMolecule> {

    private IAtomContainer formula;
    private boolean detectAromaticity;
    
    public MoleculeCandidateFactory(IAtomContainer formula, boolean detectAromaticity) {
        this.formula = formula;
        this.detectAromaticity = detectAromaticity;
    }
    
    public IMolecule generateRandomCandidate( Random arg0 ) {
        SingleStructureRandomGenerator ssrg;
        try {
            ssrg = new SingleStructureRandomGenerator();
            ssrg.setAtomContainer(formula);
            IMolecule mol = ssrg.generate();
            rearangeAtoms( mol );
            if(detectAromaticity)
                CDKHueckelAromaticityDetector.detectAromaticity( mol );
            return mol;
        } catch ( Exception e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     *  Sorts atoms like Cs-other heavy atoms om alphabetical order-Hs.
     *
     * @param  mol            The molecule to rearrange.
     * @return                The new molecule as mdl file.
     * @exception  Exception  Problems writing mdl.
     */
    public static IMolecule rearangeAtoms(IMolecule mol){
      Iterator<IAtom> atomsold = mol.atoms().iterator();
      IAtom[] atomsnew = new IAtom[mol.getAtomCount()];
      int k = 0;
      while(atomsold.hasNext()) {
        IAtom atom=atomsold.next();
        if (atom.getSymbol().equals("C")) {
          atomsnew[k++] = atom;
        }
      }
      SortedMap<String, List<IAtom>> map = new TreeMap<String, List<IAtom>>();
      atomsold = mol.atoms().iterator();
      while(atomsold.hasNext()) {
        IAtom atom=(IAtom)atomsold.next();
        if (!atom.getSymbol().equals("C") && !atom.getSymbol().equals("H")) {
            if(map.get( atom.getSymbol())==null )
                map.put( atom.getSymbol(), new ArrayList<IAtom>() );
            map.get( atom.getSymbol() ).add( atom );
        }
      }
      Iterator<String> symbolit = map.keySet().iterator();
      while(symbolit.hasNext()) {
        List<IAtom> atoms=map.get( symbolit.next() );
        for(int i=0;i<atoms.size();i++){
          atomsnew[k++] = atoms.get( i );
        }
      }
      atomsold = mol.atoms().iterator();
      while(atomsold.hasNext()) {
        IAtom atom=atomsold.next();
        if (atom.getSymbol().equals("H")) {
          atomsnew[k++] = atom;
        }
      }
      mol.setAtoms(atomsnew);
      return (mol);
    }
}
