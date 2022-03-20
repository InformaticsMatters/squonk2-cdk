/*
 *  Copyright (c) 2022  Informatics Matters Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package squonk.jobs.cdk.util;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import squonk.jobs.cdk.util.MoleculeObject.Representation;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MoleculeUtils {


    public static final Aromaticity AROMATICITY = new Aromaticity(ElectronDonation.cdk(), Cycles.cdkAromaticSet());

    public static final SilentChemObjectBuilder SILENT_OBJECT_BUILDER = (SilentChemObjectBuilder) SilentChemObjectBuilder.getInstance();
    public static final CDKHydrogenAdder HYDROGEN_ADDER = CDKHydrogenAdder.getInstance(SILENT_OBJECT_BUILDER);

    /**
     * Create a clone of the molecule that has explicit hydrogens.
     *
     * @param mol Assumed to already be initialized and contain implicit hydrogens
     * @return
     * @throws CDKException
     * @throws CloneNotSupportedException
     */
    public static IAtomContainer moleculeWithExplicitHydrogens(IAtomContainer mol)
            throws CDKException, CloneNotSupportedException {
        IAtomContainer clone = mol.clone();
        AtomContainerManipulator.convertImplicitToExplicitHydrogens(clone);
        return clone;
    }

    /**
     *  Create a clone of the molecule that has implicit hydrogens
     *
     * @param mol
     * @return
     * @throws CDKException
     * @throws CloneNotSupportedException
     */
    public static IAtomContainer moleculeWithImplicitHydrogens(IAtomContainer mol)
            throws CDKException, CloneNotSupportedException {
        IAtomContainer noHs = AtomContainerManipulator.removeHydrogens(mol);
        return noHs;
    }

    /**
     * Create the specified representation
     *
     * @param mol
     * @param key
     * @return
     */
    public static IAtomContainer createRepresentation(IAtomContainer mol, Representation key) {

        try {
            switch (key) {
                case Original:
                    return mol.clone();
                case ExplicitH:
                    return moleculeWithExplicitHydrogens(mol);
                case ImplicitH:
                    return moleculeWithImplicitHydrogens(mol);
                default:
                    throw new UnsupportedOperationException("Can't handle representation " + key);
            }
        } catch (CDKException | CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Perform basic initialiazation of a molecule by:
     * <ol>
     * <li>Percieving atom types and configuring atoms</li>
     * <li>Adding implicit hydrogens</li>
     * <li>Detecting aromaticity</li>
     * </ol>
     *
     * @param mol
     * @throws CDKException
     */
    public static void initializeMolecule(IAtomContainer mol) throws CDKException {
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        HYDROGEN_ADDER.addImplicitHydrogens(mol);
        AROMATICITY.apply(mol);
    }

    /**
     * Create a SD-file reader
     *
     * @param file
     * @return
     * @throws FileNotFoundException
     */
    public static IteratingSDFReader createSDFReader(String file) throws FileNotFoundException {
        File sdfFile = new File(file);
        IteratingSDFReader reader = new IteratingSDFReader(
                new FileInputStream(sdfFile), DefaultChemObjectBuilder.getInstance()
        );
        return reader;
    }

    /**
     * Create a SD-file writer
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static SDFWriter createSDFWriter(String file) throws IOException {

        SDFWriter writer = new SDFWriter(new FileWriter(file));
        return writer;
    }

    /**
     * Read this SD-file and generate a list of {@link MoleculeObjects}.
     * @param file
     * @return
     * @throws FileNotFoundException
     */
    public static List<MoleculeObject> readSdf(String file) throws FileNotFoundException {
        IteratingSDFReader reader = createSDFReader(file);
        List<MoleculeObject> mols = new ArrayList<>();
        while (reader.hasNext()) {
            IAtomContainer molecule = (IAtomContainer)reader.next();
            mols.add(new MoleculeObject(molecule));
        }
        return mols;
    }
}
