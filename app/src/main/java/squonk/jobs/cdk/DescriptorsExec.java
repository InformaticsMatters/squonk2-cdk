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
package squonk.jobs.cdk;

import org.apache.commons.cli.*;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import squonk.jobs.cdk.util.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Stream;


public class DescriptorsExec {

    private static final Logger LOG = Logger.getLogger(DescriptorsExec.class.getName());
    private static final DMLogger DMLOG = new DMLogger();

    private CommandLine cmd;

    protected DescriptorsExec(CommandLine cmd) {
        this.cmd = cmd;
    }

    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption("h", "help", false, "Display help");
        options.addOption(Option.builder("i")
                .longOpt("input")
                .hasArg()
                .argName("file")
                .desc("Input file with molecules (.sdf)")
                .required()
                .build());
        options.addOption(Option.builder("o")
                .longOpt("output")
                .hasArg()
                .argName("file")
                .desc("Output file for molecules (.sdf)")
                .required()
                .build());
        options.addOption("a", "all", false, "Calculate all descriptors");
        options.addOption(null, "alogp", false, "Calculate ALogP and AMR");
        options.addOption(null, "xlogp", false, "Calculate XLogP");
        options.addOption(null, "jplogp", false, "Calculate JPLogP");
        options.addOption(null, "hba", false, "Calculate H-bond acceptors");
        options.addOption(null, "hbd", false, "Calculate H-bond donors");
        options.addOption(null, "wiener", false, "Calculate Wiener numbers");
        options.addOption(null, "tpsa", false, "Calculate TPSA");
        options.addOption(null, "fcsp3", false, "Calculate Fractional SP3 carbons");
        options.addOption(null, "rotb", false, "Calculate rotatable bond count");
        options.addOption(null, "rings", false, "Calculate number of 3-9 membered rings");
        options.addOption(Option.builder(null)
                .longOpt("addhs")
                .hasArg()
                .argName("addhs")
                .desc("Include hydrogens in output (true/false). If not specified then no changes made")
                .build());

        if (args.length == 0 | (args.length == 1 && ("-h".equals(args[0]) | "--help".equals(args[0])))) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(null);
            formatter.printHelp("app", options);
        } else {

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            StringBuilder builder = new StringBuilder(DescriptorsExec.class.getName());
            for (String arg: args) {
                builder.append(" ").append(arg);
            }
            DMLOG.logEvent(DMLogger.Level.INFO,  builder.toString());

            DescriptorsExec exec = new DescriptorsExec(cmd);
            exec.calculate();
        }
    }

    private void calculate() throws Exception {
        if (cmd.hasOption("a")) {
            calculate(MolecularDescriptors.Descriptor.values());
        } else {
            List<MolecularDescriptors.Descriptor> descriptors = new ArrayList<>();

            if (cmd.hasOption("alogp")) {
                descriptors.add(MolecularDescriptors.Descriptor.ALogP);
            }
            if (cmd.hasOption("xlogp")) {
                descriptors.add(MolecularDescriptors.Descriptor.XLogP);
            }
            if (cmd.hasOption("jplogp")) {
                descriptors.add(MolecularDescriptors.Descriptor.JPLogP);
            }
            if (cmd.hasOption("hba")) {
                descriptors.add(MolecularDescriptors.Descriptor.HBondAcceptorCount);
            }
            if (cmd.hasOption("hbd")) {
                descriptors.add(MolecularDescriptors.Descriptor.HBondDonorCount);
            }
            if (cmd.hasOption("wiener")) {
                descriptors.add(MolecularDescriptors.Descriptor.WienerNumbers);
            }
            if (cmd.hasOption("tpsa")) {
                descriptors.add(MolecularDescriptors.Descriptor.TPSA);
            }
            if (cmd.hasOption("fcsp3")) {
                descriptors.add(MolecularDescriptors.Descriptor.FractionalCSP3);
            }
            if (cmd.hasOption("rotb")) {
                descriptors.add(MolecularDescriptors.Descriptor.RotatableBondCount);
            }
            if (cmd.hasOption("rings")) {
                descriptors.add(MolecularDescriptors.Descriptor.SmallRingCount);
            }

            calculate(descriptors.toArray(new MolecularDescriptors.Descriptor[descriptors.size()]));
        }
    }

    private void calculate(MolecularDescriptors.Descriptor[] descriptors) throws Exception {
        if (descriptors.length == 0) {
            throw new IllegalArgumentException("ERROR: No descriptors specified");
        }
        DMLOG.logEvent(DMLogger.Level.INFO,"Calculating " + descriptors.length + " descriptors");
        DescriptorCalculator[] calculators = DescriptorCalculator.createCalculators(descriptors);

        String inputFile = cmd.getOptionValue("input");
        String outputFile = cmd.getOptionValue("output");

        final AtomicInteger count = new AtomicInteger(0);
        final AtomicInteger errors = new AtomicInteger(0);

        MoleculeObject.Representation outputRepresentation = MoleculeObject.Representation.Original;
        if (cmd.hasOption("addhs")) {
            boolean b = Boolean.parseBoolean(cmd.getParsedOptionValue("addhs").toString());
            if (b) {
                outputRepresentation = MoleculeObject.Representation.ExplicitH;
            } else {
                outputRepresentation = MoleculeObject.Representation.ImplicitH;
            }
            LOG.info("Including hydrogens: " + outputRepresentation);
        }

        final MoleculeObject.Representation repr = outputRepresentation;

        Path path = Paths.get(outputFile);
        Path dir = path.getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }

        if (!inputFile.endsWith(".sdf")) {
            DMLOG.logEvent(DMLogger.Level.WARNING,"File " + inputFile +
                    " does not look like a SD-file. Behaviour might be unpredictable");
        }
        try (IteratingSDFReader reader = MoleculeUtils.createSDFReader(inputFile)) {

            Stream<MoleculeObject> stream = DescriptorCalculator.calculate(reader, calculators, errors);

            try (SDFWriter writer = MoleculeUtils.createSDFWriter(outputFile)) {

                stream.forEachOrdered(mo -> {
                    try {
                        count.incrementAndGet();
                        writer.write(mo.getRepresentation(repr));
                    } catch (CDKException e) {
                        errors.incrementAndGet();
                        LOG.info("Failed to write molecule " + count.intValue());
                    }
                });
                DMLOG.logEvent(DMLogger.Level.INFO,
                        String.format("Processed %s molecules, %s errors.", count.intValue(), errors.intValue()));
                int cost = count.intValue() * descriptors.length;
                DMLOG.logCost((float)cost, false);
            }
        }
    }

}
