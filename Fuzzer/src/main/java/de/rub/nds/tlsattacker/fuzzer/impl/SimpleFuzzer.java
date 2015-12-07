/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS.
 *
 * Copyright (C) 2015 Chair for Network and Data Security, Ruhr University
 * Bochum (juraj.somorovsky@rub.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package de.rub.nds.tlsattacker.fuzzer.impl;

import de.rub.nds.tlsattacker.fuzzer.config.SimpleFuzzerConfig;
import de.rub.nds.tlsattacker.fuzzer.util.CertificateHelper;
import de.rub.nds.tlsattacker.fuzzer.util.FuzzingHelper;
import de.rub.nds.tlsattacker.modifiablevariable.util.ModifiableVariableAnalyzer;
import de.rub.nds.tlsattacker.modifiablevariable.util.ModifiableVariableField;
import de.rub.nds.tlsattacker.tls.config.ConfigHandler;
import de.rub.nds.tlsattacker.tls.config.ConfigHandlerFactory;
import de.rub.nds.tlsattacker.tls.config.GeneralConfig;
import de.rub.nds.tlsattacker.tls.config.WorkflowTraceSerializer;
import de.rub.nds.tlsattacker.tls.constants.ConnectionEnd;
import de.rub.nds.tlsattacker.tls.exceptions.ConfigurationException;
import de.rub.nds.tlsattacker.tls.protocol.ModifiableVariableHolder;
import de.rub.nds.tlsattacker.tls.workflow.TlsContext;
import de.rub.nds.tlsattacker.tls.workflow.TlsContextAnalyzer;
import de.rub.nds.tlsattacker.tls.workflow.WorkflowExecutor;
import de.rub.nds.tlsattacker.tls.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.transport.TransportHandler;
import de.rub.nds.tlsattacker.util.ServerStartCommandExecutor;
import de.rub.nds.tlsattacker.util.UnoptimizedDeepCopy;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x509.Certificate;

/**
 * 
 * @author Juraj Somorovsky <juraj.somorovsky@rub.de>
 */
public class SimpleFuzzer extends Fuzzer {

    public static Logger LOGGER = LogManager.getLogger(SimpleFuzzer.class);

    private final SimpleFuzzerConfig fuzzerConfig;

    private String fuzzingName = "";

    private boolean interruptFuzzing;

    public SimpleFuzzer(SimpleFuzzerConfig fuzzerConfig, GeneralConfig generalConfig) {
	super(generalConfig);
	this.fuzzerConfig = fuzzerConfig;
    }

    @Override
    public void startFuzzer() {

	ConfigHandler configHandler = ConfigHandlerFactory.createConfigHandler("client");
	configHandler.initializeGeneralConfig(generalConfig);

	String folder = initializeLogFolder();

	try {
	    ServerStartCommandExecutor sce = null;
	    if (fuzzerConfig.containsServerCommand()) {
		sce = startTestServer(fuzzerConfig.getResultingServerCommand());
	    }

	    Certificate certificate = CertificateHelper.fetchCertificate(fuzzerConfig);

	    startFuzzing(configHandler, certificate, sce, folder);

	    // if (fuzzerConfig.containsServerCommand() &&
	    // !sce.isServerTerminated()) {
	    // sce.terminateServer();
	    // LOGGER.info(sce.getServerOutputString());
	    // LOGGER.info(sce.getServerErrorOutputString());
	    // }
	} catch (IOException | JAXBException | IllegalAccessException | IllegalArgumentException ex) {
	    throw new ConfigurationException(ex.getLocalizedMessage(), ex);
	}
    }

    private void startFuzzing(ConfigHandler configHandler, Certificate certificate, ServerStartCommandExecutor sce,
	    String folder) throws IOException, ConfigurationException, JAXBException, IllegalAccessException,
	    IllegalArgumentException {
	switch (fuzzerConfig.getFuzzingType()) {
	    case RANDOM:
		startRandomFuzzing(configHandler, certificate, sce, folder);
		break;
	    case SYSTEMATIC:
		startSystematicFuzzing(configHandler, certificate, sce, folder);
		break;
	}
    }

    /**
     * Starts random fuzzing. Modifies protocol flow, fuzzes random fields.
     * Everything is completely random in every iteration.
     * 
     * @param configHandler
     * @param certificate
     * @param sce
     * @param folder
     * @throws ConfigurationException
     * @throws JAXBException
     * @throws IOException
     */
    private void startRandomFuzzing(ConfigHandler configHandler, Certificate certificate,
	    ServerStartCommandExecutor sce, String folder) throws ConfigurationException, JAXBException, IOException {
	long step = 0;
	interruptFuzzing = false;
	while (!interruptFuzzing) {
	    if (fuzzerConfig.containsServerCommand() && fuzzerConfig.isRestartServerInEachInteration()) {
		sce = startTestServer(fuzzerConfig.getResultingServerCommand());
	    }
	    TransportHandler transportHandler = configHandler.initializeTransportHandler(fuzzerConfig);
	    TlsContext tlsContext = configHandler.initializeTlsContext(fuzzerConfig);
	    WorkflowExecutor workflowExecutor = configHandler.initializeWorkflowExecutor(transportHandler, tlsContext);
	    WorkflowTrace workflow = tlsContext.getWorkflowTrace();
	    tlsContext.setServerCertificate(certificate);

	    // protocol flow modification in messages of my side
	    executeProtocolModification(workflow, tlsContext.getMyConnectionEnd());
	    // random field modifications
	    executeRandomFieldModification(workflow);

	    try {
		workflowExecutor.executeWorkflow();
	    } catch (Exception ex) {
		LOGGER.debug(ex);
	    } finally {
		transportHandler.closeConnection();
		step++;
	    }
	    // if the server was terminated, terminate fuzzing
	    analyzeServerTerminationAndWriteFile(sce, folder, step, workflow);
	    // if the workflow contains an unexpected fields / messages, write
	    // them to a file
	    analyzeResultingTlsContextAndWriteFile(tlsContext, folder, step, "random");
	    if (fuzzerConfig.isRestartServerInEachInteration()) {
		sce.terminateServer();
	    }
	}
    }

    private void startSystematicFuzzing(ConfigHandler configHandler, Certificate certificate,
	    ServerStartCommandExecutor sce, String folder) throws JAXBException, IOException, IllegalAccessException,
	    IllegalArgumentException {
	long step = 0;
	interruptFuzzing = false;

	while (!interruptFuzzing) {
	    try {
		TlsContext tmpTlsContext = configHandler.initializeTlsContext(fuzzerConfig);
		WorkflowTrace tmpWorkflow = tmpTlsContext.getWorkflowTrace();

		executeProtocolModification(tmpWorkflow, tmpTlsContext.getMyConnectionEnd());
		addRandomRecords(tmpWorkflow, ConnectionEnd.CLIENT);

		List<ModifiableVariableField> fields = ModifiableVariableAnalyzer
			.getAllModifiableVariableFieldsRecursively(tmpWorkflow);
		for (int fieldNumber = 0; fieldNumber < fields.size(); fieldNumber++) {
		    if (!FuzzingHelper.isModifiableVariableModificationAllowed(fields.get(fieldNumber).getField(),
			    fuzzerConfig.getModifiableVariableTypes(), fuzzerConfig.getModifiableVariableFormats(),
			    fuzzerConfig.getModifiedVariableWhitelist(), fuzzerConfig.getModifiedVariableBlacklist())) {
			System.out.println("skipping " + fields.get(fieldNumber).getField().getName());
			continue;
		    }
		    for (int i = 0; i < fuzzerConfig.getMaxSystematicModifications(); i++) {
			if (fuzzerConfig.containsServerCommand() && fuzzerConfig.isRestartServerInEachInteration()) {
			    sce = startTestServer(fuzzerConfig.getResultingServerCommand());
			}
			TlsContext tlsContext = configHandler.initializeTlsContext(fuzzerConfig);
			WorkflowTrace workflow = (WorkflowTrace) UnoptimizedDeepCopy.copy(tmpWorkflow);
			tlsContext.setWorkflowTrace(workflow);
			List<ModifiableVariableField> currentFields = ModifiableVariableAnalyzer
				.getAllModifiableVariableFieldsRecursively(workflow);
			ModifiableVariableField mvField = currentFields.get(fieldNumber);
			FuzzingHelper.executeModifiableVariableModification(
				(ModifiableVariableHolder) mvField.getObject(), mvField.getField());
			TransportHandler transportHandler = configHandler.initializeTransportHandler(fuzzerConfig);
			WorkflowExecutor workflowExecutor = configHandler.initializeWorkflowExecutor(transportHandler,
				tlsContext);
			tlsContext.setServerCertificate(certificate);
			try {
			    workflowExecutor.executeWorkflow();
			} catch (Exception ex) {
			    LOGGER.debug(ex.getLocalizedMessage(), ex);
			}
			transportHandler.closeConnection();
			step++;
			// if the server was terminated, terminate fuzzing
			analyzeServerTerminationAndWriteFile(sce, folder, step, workflow);
			// if the workflow contains an unexpected fields /
			// messages,
			// write them to a file
			String fieldName = fields.get(fieldNumber).getField().getName();
			analyzeResultingTlsContextAndWriteFile(tlsContext, folder, step, fieldName);

			if (fuzzerConfig.isRestartServerInEachInteration()) {
			    sce.terminateServer();
			}
		    }
		}
	    } catch (ConfigurationException ex) {
		LOGGER.info(ex.getLocalizedMessage(), ex);
	    }
	}
    }

    private void executeRandomFieldModification(WorkflowTrace workflow) {
	while (FuzzingHelper.executeFuzzingUnit(fuzzerConfig.getModifyVariablePercentage())) {
	    FuzzingHelper.executeRandomModifiableVariableModification(workflow, ConnectionEnd.CLIENT,
		    fuzzerConfig.getModifiableVariableTypes(), fuzzerConfig.getModifiableVariableFormats(),
		    fuzzerConfig.getModifiedVariableWhitelist(), fuzzerConfig.getModifiedVariableBlacklist());
	}
    }

    private void executeProtocolModification(WorkflowTrace workflow, ConnectionEnd myConnectionEnd) {
	if (fuzzerConfig.isExecuteProtocolModification()) {
	    while (FuzzingHelper.executeFuzzingUnit(fuzzerConfig.getDuplicateMessagePercentage())) {
		FuzzingHelper.duplicateRandomProtocolMessage(workflow, myConnectionEnd);
	    }
	    while (FuzzingHelper.executeFuzzingUnit(fuzzerConfig.getNotSendingMessagePercantage())) {
		FuzzingHelper.getRandomProtocolMessage(workflow, myConnectionEnd).setGoingToBeSent(false);
	    }
	}
    }

    private void addRandomRecords(WorkflowTrace workflow, ConnectionEnd myConnectionEnd) {
	while (FuzzingHelper.executeFuzzingUnit(fuzzerConfig.getAddRecordPercentage())) {
	    FuzzingHelper.addRecordsAtRandom(workflow, myConnectionEnd);
	}
    }

    /**
     * Analyzes whether the server was terminated. If yes, the fuzzing is
     * stopped
     * 
     * @param sce
     * @param folder
     * @param step
     * @param workflow
     * @throws IOException
     * @throws JAXBException
     */
    private void analyzeServerTerminationAndWriteFile(ServerStartCommandExecutor sce, String folder, long step,
	    WorkflowTrace workflow) throws IOException, JAXBException {
	if (fuzzerConfig.containsServerCommand() && sce.isServerTerminated()
		&& !fuzzerConfig.isRestartServerInEachInteration()) {
	    FileOutputStream fos = new FileOutputStream(folder + "/terminated" + Long.toString(step) + ".xml");
	    WorkflowTraceSerializer.write(fos, workflow);
	    interruptFuzzing = true;
	}
    }

    /**
     * Analyzes the resulting workflow. It stores the workflow if the workflow
     * contains a missing message, or if it contains an unexpected new message,
     * or if it contains a modified message.
     * 
     * @param tlsContext
     * @param folder
     * @param step
     * @param fieldName
     * @throws JAXBException
     * @throws IOException
     */
    private void analyzeResultingTlsContextAndWriteFile(TlsContext tlsContext, String folder, long step,
	    String fieldName) throws JAXBException, IOException {
	if (TlsContextAnalyzer.containsFullWorkflowWithMissingMessage(tlsContext)
		|| TlsContextAnalyzer.containsServerFinishedWithModifiedHandshake(tlsContext)
		// ||
		// TlsContextAnalyzer.containsAlertAfterMissingMessage(tlsContext)
		// == TlsContextAnalyzer.AnalyzerResponse.NO_ALERT
		|| TlsContextAnalyzer.containsFullWorkflowWithModifiedMessage(tlsContext)) {
	    String fileNameBasic = createFileName(folder, step, tlsContext, fieldName);
	    FileOutputStream fos = new FileOutputStream(fileNameBasic + ".xml");
	    WorkflowTraceSerializer.write(fos, tlsContext.getWorkflowTrace());
	    if (CertificateHelper.containsModifiedCertificate(tlsContext)) {
		String fileName = fileNameBasic + "-cert.info";
		CertificateHelper.writeModifiedCertInfoToFile(tlsContext, fileName);
	    }
	    if (fuzzerConfig.isInterruptAfterFirstFinding()) {
		interruptFuzzing = true;
	    }
	}
    }

    private ServerStartCommandExecutor startTestServer(String serverCommand) throws IOException {
	ServerStartCommandExecutor sce = new ServerStartCommandExecutor(serverCommand);
	sce.startServer();
	try {
	    Thread.sleep(200);
	} catch (InterruptedException ex) {
	}
	return sce;
    }

    private String initializeLogFolder() throws ConfigurationException {
	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
	Calendar cal = Calendar.getInstance();
	String folder = "/tmp/" + fuzzingName + dateFormat.format(cal.getTime());
	File f = new File(folder);
	boolean created = f.mkdir();
	if (!created) {
	    throw new ConfigurationException("Unable to create a log folder " + folder);
	}
	return folder;
    }

    private String createFileName(String folder, long step, TlsContext tlsContext, String fieldName) {
	String fileNameBasic = folder + "/" + Long.toString(step);
	if (TlsContextAnalyzer.containsFullWorkflowWithMissingMessage(tlsContext)) {
	    fileNameBasic += "-missing-";
	}
	if (TlsContextAnalyzer.containsServerFinishedWithModifiedHandshake(tlsContext)) {
	    fileNameBasic += "-modifiedhandshake-";
	}
	if (TlsContextAnalyzer.containsFullWorkflowWithModifiedMessage(tlsContext)) {
	    fileNameBasic += "-fullmod-";
	}
	fileNameBasic += fieldName;
	return fileNameBasic;
    }

    public void setFuzzingName(String fuzzingName) {
	this.fuzzingName = fuzzingName;
    }
}