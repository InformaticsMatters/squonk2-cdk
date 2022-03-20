/*
 * Copyright (c) 2022 Informatics Matters Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package squonk.jobs.cdk.util;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.descriptors.molecular.*;
import org.openscience.cdk.qsar.result.DoubleArrayResult;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.IntegerArrayResult;
import org.openscience.cdk.qsar.result.IntegerResult;

import java.util.logging.Logger;

import squonk.jobs.cdk.util.MoleculeObject.Representation;

/**
 * This class implements various CDK molecular descriptors by extending the {@link DescriptorCalculator} class
 *
 * @author timbo
 */
public class MolecularDescriptors {

    private static final Logger LOG = Logger.getLogger(MolecularDescriptors.class.getName());

    public static final String STATS_PREFIX = "CDK";

    public static final String WIENER_PATH = "WienerPath_CDK";
    public static final String WIENER_POLARITY = "WienerPolarity_CDK";
    public static final String ALOGP_ALOPG = "ALogP_CDK";
    public static final String ALOGP_ALOPG2 = "ALogP2_CDK";
    public static final String ALOGP_AMR = "AMR_CDK";
    public static final String XLOGP = "XLogP_CDK";
    public static final String JPLOGP = "JPLogP_CDK";
    public static final String HBOND_ACCEPTOR_COUNT = "HBA_CDK";
    public static final String HBOND_DONOR_COUNT = "HBD_CDK";
    public static final String TOPOLOGICAL_PSA = "TPSA_CDK";
    public static final String FCSP3 = "FCSP3_CDK";
    public static final String ROTB = "ROTB_CDK";
    public static final String RING_COUNT = "RING_COUNT_CDK";
    public static final String RING_COUNT_ARO = "RING_COUNT_ARO_CDK";
    public static final String RING_SYS = "RING_SYS_CDK";
    public static final String RING_SYS_ARO = "RING_SYS_ARO_CDK";

    /**
     * Definitions of the supported descriptors
     */
    public enum Descriptor {

        ALogP("ALogP", ALogPCalculator.class,
                new String[]{ALOGP_ALOPG,ALOGP_ALOPG2,ALOGP_AMR},
                new Class[] {Double.class,Double.class,Double.class}),
        XLogP("XLogP", XLogPCalculator.class, new String[]{XLOGP}, new Class[] {Double.class}),
        JPLogP("JPLogP", JPLogPCalculator.class, new String[]{JPLOGP}, new Class[] {Double.class}),
        HBondDonorCount("HBondDonorCount", HBondDonorCountCalculator.class, new String[]{HBOND_DONOR_COUNT}, new Class[] {Integer.class}),
        HBondAcceptorCount("HBondAcceptorCount", HBondAcceptorCountCalculator.class, new String[]{HBOND_ACCEPTOR_COUNT}, new Class[] {Integer.class}),
        WienerNumbers("WienerNumbers", WienerNumberCalculator.class, new String[]{WIENER_PATH,WIENER_POLARITY}, new Class[] {Double.class,Double.class}),
        TPSA("TPSA", TPSACalculator.class, new String[]{TOPOLOGICAL_PSA}, new Class[] {Double.class}),
        FractionalCSP3("FCSP3", FractionalCSP3Calculator.class, new String[]{FCSP3}, new Class[] {Double.class}),
        RotatableBondCount("RotB", RotatableBondCountCalculator.class, new String[]{ROTB}, new Class[] {Integer.class}),
        SmallRingCount("RingCount", SmallRingCountCalculator.class,
                new String[]{RING_COUNT, RING_COUNT_ARO, RING_SYS, RING_SYS_ARO},       // other ring counts are present
                new Class[] {Integer.class,Integer.class,Integer.class,Integer.class}); // but not used

        public String key;
        public Class implClass;
        public String[] defaultPropNames;
        public Class[] propTypes;

        Descriptor(String key, Class cls, String[] defaultPropNames, Class[] propTypes) {
            assert defaultPropNames.length == propTypes.length;
            this.key = key;
            this.implClass = cls;
            this.defaultPropNames = defaultPropNames;
            this.propTypes = propTypes;
        }

        /**
         * Instantiate the descriptor definition creating a {@link DescriptorCalculator} that can be used to perform
         * calculations. Non-default property names can be specified.
         *
         * @param propNames Property names that will be used rather than the default ones.
         * @return
         * @throws InstantiationException
         * @throws IllegalAccessException
         */
        public DescriptorCalculator create(String[] propNames) throws InstantiationException, IllegalAccessException {
            DescriptorCalculator inst = (DescriptorCalculator) this.implClass.newInstance();
            inst.key = this.key;
            inst.propNames = propNames;
            return inst;
        }

        /**
         * Instantiate the descriptor definition creating a {@link DescriptorCalculator} that can be used to perform
         * calculations. Default property names are used.
         *
         * @return
         * @throws InstantiationException
         * @throws IllegalAccessException
         */
        public DescriptorCalculator create() throws InstantiationException, IllegalAccessException {
            return create(this.defaultPropNames);
        }
    }


    abstract static class IntegerCalculator extends DescriptorCalculator {

        protected IntegerCalculator(Representation representation) {
            this.representation = representation;
        }

        @Override
        public void calculate(MoleculeObject mo) throws Exception {
            IAtomContainer mol = prepareMolecule(mo);
            String prop = propNames[0];
            DescriptorValue result = descriptor.calculate(mol);
            IntegerResult retval = (IntegerResult) result.getValue();
            mo.setProperty(prop, retval.intValue());
            incrementExecutionCount(1);
        }

    }

    abstract static class DoubleCalculator extends DescriptorCalculator {

        protected DoubleCalculator(Representation representation) {
            this.representation = representation;
        }

        @Override
        public void calculate(MoleculeObject mo) throws Exception {

            IAtomContainer mol = prepareMolecule(mo);
            String prop = propNames[0];
            DescriptorValue result = descriptor.calculate(mol);
            DoubleResult retval = (DoubleResult) result.getValue();
            if (retval != null) {
                putDoubleIfProperValue(mo, prop, retval.doubleValue());
                incrementExecutionCount(1);
            }
        }
    }

    abstract static class IntegerArrayCalculator extends DescriptorCalculator {

        protected IntegerArrayCalculator(Representation representation) {
            this.representation = representation;
        }

        @Override
        public void calculate(MoleculeObject mo) throws Exception {

            IAtomContainer mol = prepareMolecule(mo);
            DescriptorValue result = descriptor.calculate(mol);
            IntegerArrayResult retval = (IntegerArrayResult) result.getValue();
            if (retval != null) {
                for (int i = 0; i < propNames.length; i++) {
                    String prop = propNames[i];
                    mo.setProperty(prop, retval.get(i));
                }
                incrementExecutionCount(1);
            }
        }
    }

    abstract static class DoubleArrayCalculator extends DescriptorCalculator {

        protected DoubleArrayCalculator(Representation representation) {
            this.representation = representation;
        }

        @Override
        public void calculate(MoleculeObject mo) throws Exception {

            IAtomContainer mol = prepareMolecule(mo);
            DescriptorValue result = descriptor.calculate(mol);
            DoubleArrayResult retval = (DoubleArrayResult) result.getValue();
            if (retval != null) {
                for (int i = 0; i < propNames.length; i++) {
                    String prop = propNames[i];
                    putDoubleIfProperValue(mo, prop, retval.get(i));
                }
                incrementExecutionCount(1);
            }
        }
    }

    private static void putDoubleIfProperValue(MoleculeObject mo, String propName, Double d) {
        if (d != null && !d.isNaN() && !d.isInfinite()) {
            mo.setProperty(propName, d);
        }
    }

    static class HBondDonorCountCalculator extends IntegerCalculator {

        protected HBondDonorCountCalculator() {
            super(Representation.Original);
            descriptor = new HBondDonorCountDescriptor();
        }
    }

    static class HBondAcceptorCountCalculator extends IntegerCalculator {

        HBondAcceptorCountCalculator() {
            super(Representation.Original);
            descriptor = new HBondAcceptorCountDescriptor();
        }
    }

    static class WienerNumberCalculator extends DoubleArrayCalculator {

        WienerNumberCalculator() {
            super(Representation.Original);
            descriptor = new WienerNumbersDescriptor();
        }
    }

    static class ALogPCalculator extends DoubleArrayCalculator {

        ALogPCalculator() throws CDKException {
            super(Representation.ExplicitH);
            descriptor = new ALOGPDescriptor();
        }
    }

    static class XLogPCalculator extends DoubleCalculator {

        public XLogPCalculator() {
            super(Representation.ExplicitH);
            descriptor = new XLogPDescriptor();
        }
    }

    static class JPLogPCalculator extends DoubleCalculator {

        public JPLogPCalculator() {
            super(Representation.ExplicitH);
            descriptor = new JPlogPDescriptor();
        }
    }

    static class TPSACalculator extends DoubleCalculator {

        public TPSACalculator() {
            super(Representation.Original);
            descriptor = new TPSADescriptor();
        }
    }

    static class FractionalCSP3Calculator extends DoubleCalculator {

        public FractionalCSP3Calculator() {
            super(Representation.Original);
            descriptor = new FractionalCSP3Descriptor();
        }
    }

    static class RotatableBondCountCalculator extends IntegerCalculator {

        public RotatableBondCountCalculator() {
            super(Representation.Original);
            descriptor = new RotatableBondsCountDescriptor();
        }
    }

    static class SmallRingCountCalculator extends IntegerArrayCalculator {

        public SmallRingCountCalculator() {
            super(Representation.Original);
            descriptor = new SmallRingDescriptor();
        }
    }
}
