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

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.IMolecularDescriptor;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import squonk.jobs.cdk.util.MoleculeObject.Representation;

/**
 * Class that allows CDK descriptors to be calculated.
 *
 * @author timbo
 */
public abstract class DescriptorCalculator {

    private static final Logger LOG = Logger.getLogger(DescriptorCalculator.class.getName());

    protected IMolecularDescriptor descriptor;
    protected String key;
    protected Representation representation;
    protected String[] propNames;
    protected final Map<String, Integer> executionStats = new HashMap<>();


    /**
     * The key to use when generating usage stats
     *
     * @return
     */
    public String getKey() {
        return key;
    }

    public String[] getPropertyNames() {
        return propNames;
    }

    public static DescriptorCalculator[] createCalculators(MolecularDescriptors.Descriptor[] descriptors)
            throws InstantiationException, IllegalAccessException {
        // prepare the calculators
        List<DescriptorCalculator> calculators = new ArrayList<>();
        for (
                MolecularDescriptors.Descriptor d : descriptors) {
            calculators.add(d.create());
        }
        return calculators.toArray(new DescriptorCalculator[descriptors.length]);
    }

    /**
     * Sub-classes must override and implement the calculation
     *
     * @param mol
     * @throws Exception
     */
    public abstract void calculate(MoleculeObject mol) throws Exception;

    /**
     * Calculate for this stream of molecules.
     * NOTE: you must call some terminal method on the stream that is returned by this method to trigger the
     * calculations on the stream.
     *
     * @param mols
     */
    public Stream<MoleculeObject> calculate(Stream<MoleculeObject> mols) {
        Stream<MoleculeObject> result = mols.peek(m -> {
            try {
                calculate(m);
            } catch (Exception e) {
                LOG.info("Failed to process molecule");
            }
        });
        return result;
    }

    /**
     * Static method that calculate for this Iterator of IAtomContainer molecules and set of
     * {@link DescriptorCalculator}s.
     * NOTE: you must call some terminal method on the stream that is returned by this method to trigger the
     * calculations on the stream.
     *
     * @param mols
     * @param calculators
     * @param errorCount An AtomicInteger that is used to record the error count.
     */
    public static Stream<MoleculeObject> calculate(
            Iterator<IAtomContainer> mols,
            DescriptorCalculator[] calculators,
            AtomicInteger errorCount)
            throws Exception {

        Stream<MoleculeObject> stream = StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(mols, Spliterator.ORDERED), true)
                .map(MoleculeObject::new);

        return calculate(stream, calculators, errorCount);
    }

    /**
     * Static method that calculate for this Stream of {@link MoleculeObject}s and set of
     * {@link DescriptorCalculator}s.
     * NOTE: you must call some terminal method on the stream that is returned by this method to trigger the
     * calculations on the stream.
     *
     * @param mols
     * @param calculators
     * @param errorCount An AtomicInteger that is used to record the error count.
     */
    public static Stream<MoleculeObject> calculate(
            Stream<MoleculeObject> mols,
            DescriptorCalculator[] calculators,
            AtomicInteger errorCount)
            throws Exception {

        AtomicInteger count = new AtomicInteger(0);
        Stream<MoleculeObject> result = mols.peek(m -> {
            for (DescriptorCalculator calc : calculators) {
                try {
                    count.incrementAndGet();
                    calc.calculate(m);
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    LOG.log(Level.INFO, "Failed to process molecule " + count.intValue(), e);
                }
            }
        });

        return result;
    }

    /**
     * Get the molecule in the {@link MoleculeObject.Representation} needed by the calculation
     *
     * @param mo
     * @return
     * @throws Exception
     */
    protected IAtomContainer prepareMolecule(MoleculeObject mo) throws Exception {
        return mo.getRepresentation(representation);
    }

    public Map<String, Integer> getExecutionStats() {
        return executionStats;
    }

    protected int incrementExecutionCount(int count) {
        return ExecutionStats.increment(executionStats, MolecularDescriptors.STATS_PREFIX + "." + getKey(), count);
    }

}
