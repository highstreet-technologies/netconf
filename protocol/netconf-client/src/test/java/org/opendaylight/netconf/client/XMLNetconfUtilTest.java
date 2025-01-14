/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.client;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import org.junit.Test;
import org.opendaylight.netconf.api.xml.XmlUtil;
import org.w3c.dom.Element;

public class XMLNetconfUtilTest {
    @Test
    public void testXPath() throws Exception {
        final XPathExpression correctXPath = XMLNetconfUtil.compileXPath("/top/innerText");
        final IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> XMLNetconfUtil.compileXPath("!@(*&$!"));
        assertThat(ex.getMessage(), startsWith("Error while compiling xpath expression "));
        final Object value = XmlUtil.evaluateXPath(correctXPath,
                XmlUtil.readXmlToDocument("<top><innerText>value</innerText></top>"), XPathConstants.NODE);
        assertEquals("value", ((Element) value).getTextContent());
    }
}
