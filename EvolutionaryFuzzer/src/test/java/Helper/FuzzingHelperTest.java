/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2016 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
package Helper;

import de.rub.nds.tlsattacker.modifiablevariable.util.ModifiableVariableField;
import de.rub.nds.tlsattacker.modifiablevariable.util.ModifiableVariableListHolder;
import de.rub.nds.tlsattacker.tls.constants.ConnectionEnd;
import de.rub.nds.tlsattacker.tls.protocol.ModifiableVariableHolder;
import de.rub.nds.tlsattacker.tls.protocol.handshake.ClientHelloMessage;
import de.rub.nds.tlsattacker.tls.protocol.handshake.ServerHelloMessage;
import de.rub.nds.tlsattacker.tls.workflow.WorkflowTrace;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 
 * @author Robert Merget - robert.merget@rub.de
 */
public class FuzzingHelperTest {

    public FuzzingHelperTest() {
    }

    /**
     * Test of pickRandomField method, of class FuzzingHelper.
     */
    @Test
    public void testPickRandomField() {
	List<ModifiableVariableField> fields = new ArrayList<>();
	fields.add(new ModifiableVariableField());
	fields.add(new ModifiableVariableField());
	fields.add(new ModifiableVariableField());
	ModifiableVariableField result = FuzzingHelper.pickRandomField(fields);
	assertNotNull("Failure: Should return a Field", result);
    }

    /**
     * Test of getModifiableVariableHolders method, of class FuzzingHelper.
     */
    @Test
    public void testGetModifiableVariableHolders() {
	WorkflowTrace trace = new WorkflowTrace();
	trace.add(new ClientHelloMessage(ConnectionEnd.CLIENT));
	List<ModifiableVariableHolder> result = FuzzingHelper.getModifiableVariableHolders(trace, ConnectionEnd.CLIENT);
	assertTrue("Failure: WorkflowTrace should contain atleast one Holder", result.size() > 0);
    }

    /**
     * Test of addRecordAtRandom method, of class FuzzingHelper.
     */
    @Test
    public void testAddRecordsAtRandom() {
	WorkflowTrace trace = new WorkflowTrace();
	trace.add(new ClientHelloMessage());
	ConnectionEnd messageIssuer = ConnectionEnd.CLIENT;
	FuzzingHelper.addRecordAtRandom(trace, messageIssuer);
    }

    /**
     * Test of removeRandomMessage method, of class FuzzingHelper.
     */
    @Test
    public void testRemoveRandomMessage() {
	WorkflowTrace tempTrace = new WorkflowTrace();
	tempTrace.add(new ClientHelloMessage(ConnectionEnd.CLIENT));
	tempTrace.add(new ServerHelloMessage(ConnectionEnd.SERVER));

	FuzzingHelper.removeRandomMessage(tempTrace);
	assertTrue("Failure: Workflow should contain only one Message after.",
		tempTrace.getProtocolMessages().size() == 1);
    }

    /**
     * Test of addRandomMessage method, of class FuzzingHelper.
     */
    @Test
    public void testAddRandomMessage() {
	WorkflowTrace tempTrace = new WorkflowTrace();
	FuzzingHelper.addRandomMessage(tempTrace);
	assertTrue(
		"A Workflowtrace should contain 2 Messages after we added one at Random. (The arbitary Message for the Server is always added)",
		tempTrace.getProtocolMessages().size() == 2);
    }

    /**
     * Test of duplicateRandomProtocolMessage method, of class FuzzingHelper.
     */
    @Test
    public void testDuplicateRandomProtocolMessage() {
	WorkflowTrace trace = new WorkflowTrace();
	trace.add(new ClientHelloMessage(ConnectionEnd.CLIENT));
	ConnectionEnd messageIssuer = ConnectionEnd.CLIENT;
	FuzzingHelper.duplicateRandomProtocolMessage(trace, messageIssuer);
	assertTrue("Failure: After Duplicating the Trace should contain 2 Messages",
		trace.getProtocolMessages().size() == 2);
    }

    /**
     * Test of getAllModifiableVariableHoldersRecursively method, of class
     * FuzzingHelper.
     */
    @Test
    public void testGetAllModifiableVariableHoldersRecursively() {
	WorkflowTrace trace = new WorkflowTrace();
	trace.add(new ClientHelloMessage(ConnectionEnd.CLIENT));
	ConnectionEnd myPeer = ConnectionEnd.CLIENT;
	List<ModifiableVariableListHolder> result = FuzzingHelper.getAllModifiableVariableHoldersRecursively(trace,
		myPeer);
	assertTrue("Failure: Trace should contain more than zero Holders", result.size() > 0);
    }

}
