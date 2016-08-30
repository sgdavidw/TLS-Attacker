/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2016 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package Analyzer;

import Config.Analyzer.AnalyzeTimeRuleConfig;
import Config.Analyzer.EarlyHeartbeatRuleConfig;
import Config.Analyzer.UniqueFlowsRuleConfig;
import Config.EvolutionaryFuzzerConfig;
import Result.Result;
import TestVector.TestVectorSerializer;
import de.rub.nds.tlsattacker.tls.constants.ConnectionEnd;
import de.rub.nds.tlsattacker.tls.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.tls.constants.ProtocolMessageType;
import de.rub.nds.tlsattacker.tls.protocol.ProtocolMessage;
import de.rub.nds.tlsattacker.tls.protocol.handshake.HandshakeMessage;
import de.rub.nds.tlsattacker.tls.workflow.WorkflowTrace;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBException;

/**
 * 
 * @author ic0ns
 */
public class EarlyHeartbeatRule extends Rule {
    private EarlyHeartbeatRuleConfig config;
    private int found = 0;

    public EarlyHeartbeatRule(EvolutionaryFuzzerConfig evoConfig) {
	super(evoConfig, "early_heartbeat.rule");
	File f = new File(evoConfig.getAnalyzerConfigFolder() + configFileName);
	if (f.exists()) {
	    config = JAXB.unmarshal(f, EarlyHeartbeatRuleConfig.class);
	}
	if (config == null) {
	    config = new EarlyHeartbeatRuleConfig();
	    writeConfig(config);
	}
	prepareConfigOutputFolder();
    }

    @Override
    public boolean applys(Result result) {
	WorkflowTrace trace = result.getExecutedVector().getTrace();
	if (!trace.getActualReceivedProtocolMessagesOfType(ProtocolMessageType.HEARTBEAT).isEmpty()) {
	    return hasHeartbeatWithoutFinished(trace) || hasHeartbeatBeforeFinished(trace);
	} else {
	    return false;
	}
    }

    @Override
    public void onApply(Result result) {
	found++;
	File f = new File(evoConfig.getOutputFolder() + config.getOutputFolder() + result.getId());
	try {
	    result.getExecutedVector()
		    .getTrace()
		    .setDescription(
			    "WorkflowTrace has a Heartbeat from the Server before the Server send his finished message!");
	    f.createNewFile();
	    TestVectorSerializer.write(f, result.getExecutedVector());
	} catch (JAXBException | IOException E) {
	    LOG.log(Level.SEVERE,
		    "Could not write Results to Disk! Does the Fuzzer have the rights to write to "
			    + f.getAbsolutePath(), E);
	}
    }

    @Override
    public void onDecline(Result result) {
    }

    @Override
    public String report() {
	if (found > 0) {
	    return "Found " + found + " Traces with EarlyHeartBeat messages from the Server\n";
	} else {
	    return null;
	}
    }

    @Override
    public EarlyHeartbeatRuleConfig getConfig() {
	return config;
    }

    public boolean hasHeartbeatWithoutFinished(WorkflowTrace trace) {
	List<HandshakeMessage> finishedMessages = trace
		.getActuallyRecievedHandshakeMessagesOfType(HandshakeMessageType.FINISHED);
	List<ProtocolMessage> heartBeatMessages = trace
		.getActualReceivedProtocolMessagesOfType(ProtocolMessageType.HEARTBEAT);
	return (finishedMessages.isEmpty() && !heartBeatMessages.isEmpty());
    }

    public boolean hasHeartbeatBeforeFinished(WorkflowTrace trace) {
	return trace.actuallyReceivedTypeBeforeType(ProtocolMessageType.HEARTBEAT, HandshakeMessageType.FINISHED);
    }

    private static final Logger LOG = Logger.getLogger(EarlyHeartbeatRule.class.getName());

}
