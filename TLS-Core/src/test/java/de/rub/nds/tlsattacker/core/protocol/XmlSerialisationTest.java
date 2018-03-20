/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2017 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.core.protocol;

import de.rub.nds.tlsattacker.core.protocol.message.ProtocolMessage;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTraceSerializer;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class XmlSerialisationTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testProtocolMessages() throws Exception {
        List<ProtocolMessage> messageList = MessageFactory.generateProtocolMessages();
        for (ProtocolMessage message : messageList) {
            WorkflowTrace trace = new WorkflowTrace();
            trace.addTlsAction(new SendAction(message));
            trace.addTlsAction(new ReceiveAction(message));
            File f = folder.newFile();
            WorkflowTraceSerializer.write(f, trace);
            WorkflowTrace newWorkflowTrace = WorkflowTraceSerializer.read(new FileInputStream(f));
            assertTrue(newWorkflowTrace.getTlsActions().size() == 2);

            assertTrue("Message failed: " + message.getClass().getName(), ((SendAction) newWorkflowTrace
                    .getTlsActions().get(0)).getMessages().size() == 1);
            assertTrue("Message failed: " + message.getClass().getName(), ((SendAction) newWorkflowTrace
                    .getTlsActions().get(0)).getMessages().get(0).getClass().equals(message.getClass()));
            assertTrue("Message failed: " + message.getClass().getName(), ((ReceiveAction) newWorkflowTrace
                    .getTlsActions().get(1)).getExpectedMessages().size() == 1);
            assertTrue("Message failed: " + message.getClass().getName(), ((ReceiveAction) newWorkflowTrace
                    .getTlsActions().get(1)).getExpectedMessages().get(0).getClass().equals(message.getClass()));
        }
    }
}
