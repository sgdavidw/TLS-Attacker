/**
 * TLS-Attacker - A Modular Penetration Testing Framework for TLS
 *
 * Copyright 2014-2017 Ruhr University Bochum / Hackmanit GmbH
 *
 * Licensed under Apache License 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package de.rub.nds.tlsattacker.core.protocol.preparator;

import de.rub.nds.modifiablevariable.util.ArrayConverter;
import de.rub.nds.tlsattacker.core.constants.HandshakeMessageType;
import de.rub.nds.tlsattacker.core.protocol.message.CertificateMessage;
import de.rub.nds.tlsattacker.core.workflow.TlsContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.bouncycastle.crypto.tls.Certificate;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Robert Merget - robert.merget@rub.de
 */
public class CertificateMessagePreparatorTest {

    private CertificateMessagePreparator preparator;
    private CertificateMessage message;
    private TlsContext context;

    @Before
    public void setUp() {
        context = new TlsContext();
        message = new CertificateMessage();
        preparator = new CertificateMessagePreparator(context, message);
    }

    /**
     * Test of prepareHandshakeMessageContents method, of class
     * CertificateMessagePreparator.
     * 
     * @throws java.io.IOException
     */
    @Test
    public void testPrepare() throws IOException {
        Certificate cert = Certificate
                .parse(new ByteArrayInputStream(
                        ArrayConverter
                                .hexStringToByteArray("0003ee0003eb308203e7308202cfa003020102020900b9eed4d955a59eb3300d06092a864886f70d01010505003070310b300906035504061302554b31163014060355040a0c0d4f70656e53534c2047726f757031223020060355040b0c19464f522054455354494e4720505552504f534553204f4e4c593125302306035504030c1c4f70656e53534c205465737420496e7465726d656469617465204341301e170d3131313230383134303134385a170d3231313031363134303134385a3064310b300906035504061302554b31163014060355040a0c0d4f70656e53534c2047726f757031223020060355040b0c19464f522054455354494e4720505552504f534553204f4e4c593119301706035504030c105465737420536572766572204365727430820122300d06092a864886f70d01010105000382010f003082010a0282010100f384f39236dcb246ca667ae529c5f3492822d3b9fee0dee438ceee221ce9913b94d0722f8785594b66b1c5f57a855dc20fd32e295836cc486ba2a2b526ce67e247b6df49d23ffaa210b7c297447e87346d6df28bb4552bd621de534b90eafdeaf938352bf4e69a0ef6bb12ab8721c32fbcf406b88f8e10072795e542cbd1d5108c92acee0fdc234889c9c6930c2202e774e72500abf80f5c10b5853b6694f0fb4d570655212225dbf3aaa960bf4daa79d1ab9248ba198e12ec68d9c6badfec5a1cd843fee752c9cf02d0c77fc97eb094e35344580b2efd2974b5069b5c448dfb3275a43aa8677b87320a508de1a2134a25afe61cb125bfb499a253d3a202bf110203010001a3818f30818c300c0603551d130101ff04023000300e0603551d0f0101ff0404030205e0302c06096086480186f842010d041f161d4f70656e53534c2047656e657261746564204365727469666963617465301d0603551d0e0416041482bccf000013d1f739259a27e7afd2ef201b6eac301f0603551d2304183016801436c36c88e795feb0bdecce3e3d86ab218187dada300d06092a864886f70d01010505000382010100a9bd4d574074fe96e92bd678fdb363ccf40b4d12ca5a748d9bf261e6fd06114384fc17a0ec636336b99e366ab1025a6a5b3f6aa1ea0565ac7e401a486588d1394dd34b77e9c8bb2b9e5af408343947b90208319af1d917c5e9a6a5964b6d40a95b6528cbcb0003826337d3adb1963b76f51716027bbd5353467234d608649dbb43fb64b149077709617a421711300cd9275cf571b6f01830f37ef1853f327e4aafb310f76cc6854b2d27ad0a205cfb8d197034b9755f7c87d5c3ec931341fc7303b98d1afef726864903a9c5823f800d2949b18fed241bfecf589046e7a887d41e79ef996d189f3e8b8207c143c7e025b6f1d300d740ab4b7f2b7a3ea6994c54")));
        context.getConfig().setOurCertificate(cert);
        preparator.prepare();
        assertArrayEquals(
                message.getX509CertificateBytes().getValue(),
                ArrayConverter
                        .hexStringToByteArray("0003eb308203e7308202cfa003020102020900b9eed4d955a59eb3300d06092a864886f70d01010505003070310b300906035504061302554b31163014060355040a0c0d4f70656e53534c2047726f757031223020060355040b0c19464f522054455354494e4720505552504f534553204f4e4c593125302306035504030c1c4f70656e53534c205465737420496e7465726d656469617465204341301e170d3131313230383134303134385a170d3231313031363134303134385a3064310b300906035504061302554b31163014060355040a0c0d4f70656e53534c2047726f757031223020060355040b0c19464f522054455354494e4720505552504f534553204f4e4c593119301706035504030c105465737420536572766572204365727430820122300d06092a864886f70d01010105000382010f003082010a0282010100f384f39236dcb246ca667ae529c5f3492822d3b9fee0dee438ceee221ce9913b94d0722f8785594b66b1c5f57a855dc20fd32e295836cc486ba2a2b526ce67e247b6df49d23ffaa210b7c297447e87346d6df28bb4552bd621de534b90eafdeaf938352bf4e69a0ef6bb12ab8721c32fbcf406b88f8e10072795e542cbd1d5108c92acee0fdc234889c9c6930c2202e774e72500abf80f5c10b5853b6694f0fb4d570655212225dbf3aaa960bf4daa79d1ab9248ba198e12ec68d9c6badfec5a1cd843fee752c9cf02d0c77fc97eb094e35344580b2efd2974b5069b5c448dfb3275a43aa8677b87320a508de1a2134a25afe61cb125bfb499a253d3a202bf110203010001a3818f30818c300c0603551d130101ff04023000300e0603551d0f0101ff0404030205e0302c06096086480186f842010d041f161d4f70656e53534c2047656e657261746564204365727469666963617465301d0603551d0e0416041482bccf000013d1f739259a27e7afd2ef201b6eac301f0603551d2304183016801436c36c88e795feb0bdecce3e3d86ab218187dada300d06092a864886f70d01010505000382010100a9bd4d574074fe96e92bd678fdb363ccf40b4d12ca5a748d9bf261e6fd06114384fc17a0ec636336b99e366ab1025a6a5b3f6aa1ea0565ac7e401a486588d1394dd34b77e9c8bb2b9e5af408343947b90208319af1d917c5e9a6a5964b6d40a95b6528cbcb0003826337d3adb1963b76f51716027bbd5353467234d608649dbb43fb64b149077709617a421711300cd9275cf571b6f01830f37ef1853f327e4aafb310f76cc6854b2d27ad0a205cfb8d197034b9755f7c87d5c3ec931341fc7303b98d1afef726864903a9c5823f800d2949b18fed241bfecf589046e7a887d41e79ef996d189f3e8b8207c143c7e025b6f1d300d740ab4b7f2b7a3ea6994c54"));
        assertTrue(message.getCertificatesLength().getValue() == 1006);
        assertTrue(message.getType().getValue() == HandshakeMessageType.CERTIFICATE.getValue());
        assertTrue(message.getLength().getValue() == 1009);
    }

}
