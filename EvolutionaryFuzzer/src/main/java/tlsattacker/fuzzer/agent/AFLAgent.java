/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2016 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package tlsattacker.fuzzer.agent;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import tlsattacker.fuzzer.instrumentation.Branch;
import tlsattacker.fuzzer.helper.LogFileIDManager;
import tlsattacker.fuzzer.result.AgentResult;
import tlsattacker.fuzzer.server.TLSServer;
import tlsattacker.fuzzer.certificate.ServerCertificateStructure;
import tlsattacker.fuzzer.testvector.TestVector;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.xml.bind.JAXB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tlsattacker.fuzzer.config.EvolutionaryFuzzerConfig;
import tlsattacker.fuzzer.config.FuzzerGeneralConfig;
import tlsattacker.fuzzer.config.agent.AFLAgentConfig;
import tlsattacker.fuzzer.config.mutator.SimpleMutatorConfig;
import tlsattacker.fuzzer.instrumentation.AFLInstrumentationMap;
import tlsattacker.fuzzer.instrumentation.InstrumentationMap;

/**
 * An Agent implemented with the modified Binary Instrumentation used by
 * American Fuzzy Lop
 *
 * @author Robert Merget - robert.merget@rub.de
 */
public class AFLAgent extends Agent {

    static final Logger LOGGER = LogManager.getLogger(AFLAgent.class);

    /**
     * Agent config used
     */
    private final AFLAgentConfig config;

    /**
     * The name of the Agent when referred by command line
     */
    public static final String optionName = "AFL";

    /**
     * Parses a file into a BranchTrace object
     *
     * @param file
     *            File to parse
     * @return Newly generated BranchTrace object
     */
    private InstrumentationMap getInstrumentationMap(File file) {
        long[] bitmap = new long[config.getBitmapSize()];
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = br.readLine()) != null) {
                // Check if the Line can be parsed
                int parsedLocation;
                long parsedValue;
                try {
                    parsedLocation = Integer.parseInt(line.split(":")[0]);
                    parsedValue = Long.parseLong(line.split(":")[1]);
                    bitmap[parsedLocation] = parsedValue;
                } catch (NumberFormatException e) {
                    throw new NumberFormatException("BranchTrace contains unparsable Lines: " + line);
                }
            }
            return new AFLInstrumentationMap(bitmap);
        } catch (IOException ex) {
            LOGGER.error("Could not read BranchTrace from file, using Empty BranchTrace instead", ex);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                LOGGER.error(ex.getLocalizedMessage(), ex);
            }
        }
        return new AFLInstrumentationMap(new long[config.getBitmapSize()]);
    }

    /**
     * The prefix that has to be set in front of the actual server command
     */
    private final String prefix = "AFL/afl-showmap -m none -o [output]/[id] ";

    /**
     * Default Constructor
     *
     * @param generalConfig
     *            The EvolutionaryFuzzerConfig used
     * @param keypair
     *            The key certificate pair the server should be started with
     * @param server
     *            Server used by the Agent
     */
    public AFLAgent(FuzzerGeneralConfig generalConfig ,ServerCertificateStructure keypair, TLSServer server) {
        super(keypair, server);
        timeout = false;
        crash = false;
        File f = new File(generalConfig.getAgentConfigFolder() + "afl.conf");
        if (f.exists()) {
            this.config = JAXB.unmarshal(f, AFLAgentConfig.class);
        } else {
            this.config = new AFLAgentConfig();
            JAXB.marshal(this.config, f);
        }
    }

    @Override
    public void applicationStart() {
        if (running) {
            throw new IllegalStateException("Cannot start a running AFL Agent");
        }
        startTime = System.currentTimeMillis();
        running = true;
        server.start(prefix, keypair.getCertificateFile(), keypair.getKeyFile());
    }

    @Override
    public void applicationStop() {
        if (!running) {
            throw new IllegalStateException("Cannot stop a stopped AFL Agent");
        }
        stopTime = System.currentTimeMillis();
        running = false;
        server.stop();
        if (server.getExitCode() == 2) {
            crash = true;
        }
        if (server.getExitCode() == 1) {
            timeout = true;
        }
    }

    @Override
    public AgentResult collectResults(File branchTrace, TestVector vector) {
        if (running) {
            throw new IllegalStateException("Can't collect Results, Agent still running!");
        }
        if (branchTrace.exists()) {
            InstrumentationMap instrumentationMap = getInstrumentationMap(branchTrace);

            AgentResult result = new AgentResult(crash, timeout, startTime, stopTime, instrumentationMap, vector,
                    LogFileIDManager.getInstance().getFilename(), server);

            return result;
        } else {
            LOGGER.debug("Failed to collect instrumentation output");
            return new AgentResult(crash, timeout, startTime, startTime, new AFLInstrumentationMap(new long[config.getBitmapSize()]),
                    vector, LogFileIDManager.getInstance().getFilename(), server);
        }
    }
}