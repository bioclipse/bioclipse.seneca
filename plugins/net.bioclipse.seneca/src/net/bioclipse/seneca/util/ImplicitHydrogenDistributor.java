package net.bioclipse.seneca.util;

import org.openscience.cdk.config.Elements;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * Needs to be moved to the CDK.
 *
 * @author egonw
 */
public class ImplicitHydrogenDistributor {

	public static IAtomContainer generate(IAtomContainer atomContainer) {
		IAtomContainer result = atomContainer.getBuilder().newInstance(IAtomContainer.class);
		int hCount = countHydrogens(atomContainer);
		System.out.println("Hydrogens to distribute: " + hCount);

		// copy all non hydrogens
		for (IAtom atom : atomContainer.atoms()) {
			String atomSymbol = atom.getSymbol();
			if (!atomSymbol.equals(Elements.HYDROGEN.getSymbol())) {
				result.addAtom(atom);
				if (atomSymbol.equals(Elements.NITROGEN.getSymbol()) ||
					atomSymbol.equals(Elements.CARBON.getSymbol())) {
					if (hCount >= 2) {
						atom.setImplicitHydrogenCount(2);
						hCount = hCount-2;
					}
				} else if (atomSymbol.equals(Elements.OXYGEN.getSymbol())) {
					if (hCount >= 1) {
						atom.setImplicitHydrogenCount(1);
						hCount = hCount-1;
					}
				}
			}
		}
		if (hCount > 0) {
			// distribute the rest over the carbons
			System.out.println("#hydrogens to distribute: " + hCount);
			for (IAtom atom : result.atoms()) {
				if (hCount == 0) break;	// XXX altered - may not work : check!
				if (atom.getSymbol().equals(Elements.CARBON.getSymbol())) {
					// ok, only added two so far, so can add another one
					atom.setImplicitHydrogenCount(3);
					hCount--;
				}
			}
		}
		System.out.println("Hydrogens distributed: " + countImplicitHydrogens(result));
		System.out.println("any left overs?: " + hCount);

		return result;
	}

	private static int countHydrogens(IAtomContainer container) {
		int hCount = 0;
		for (IAtom atom : container.atoms()) {
			if (atom.getSymbol().equals(Elements.HYDROGEN.getSymbol()))
				hCount++;
		}
		return hCount;
	}

	private static int countImplicitHydrogens(IAtomContainer container) {
		int hCount = 0;
		for (IAtom atom : container.atoms()) {
			hCount += atom.getImplicitHydrogenCount();
		}
		return hCount;
	}
}
