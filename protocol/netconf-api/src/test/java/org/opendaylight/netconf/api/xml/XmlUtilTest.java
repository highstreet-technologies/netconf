/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.api.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXParseException;

public class XmlUtilTest {
    private static final String XML_SNIPPET = """
        <top xmlns="namespace">
            <innerText>value</innerText>
            <innerPrefixedText xmlns:pref="prefixNamespace">prefix:value</innerPrefixedText>
            <innerPrefixedText xmlns="randomNamespace" xmlns:pref="prefixNamespace">prefix:value</innerPrefixedText>
        </top>""";

    @Test
    public void testCreateElement() throws Exception {
        final Document document = XmlUtil.newDocument();
        final Element top = XmlUtil.createElement(document, "top", Optional.of("namespace"));

        top.appendChild(XmlUtil.createTextElement(document, "innerText", "value", Optional.of("namespace")));
        top.appendChild(XmlUtil.createTextElementWithNamespacedContent(document, "innerPrefixedText", "pref",
                "prefixNamespace", "value", Optional.of("namespace")));
        top.appendChild(XmlUtil.createTextElementWithNamespacedContent(document, "innerPrefixedText", "pref",
                "prefixNamespace", "value", Optional.of("randomNamespace")));

        document.appendChild(top);
        assertEquals("top", XmlUtil.createDocumentCopy(document).getDocumentElement().getTagName());

        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreWhitespace(true);

        final Diff diff = XMLUnit.compareXML(XMLUnit.buildControlDocument(XML_SNIPPET), document);
        assertTrue(diff.toString(), diff.similar());
    }

    @Test
    public void testXXEFlaw() {
        assertThrows(SAXParseException.class, () -> XmlUtil.readXmlToDocument("""
            <!DOCTYPE foo [\s\s
            <!ELEMENT foo ANY >
            <!ENTITY xxe SYSTEM "file:///etc/passwd" >]>
            <hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
              <capabilities>
                <capability>urn:ietf:params:netconf:base:1.0 &xxe;</capability>
              </capabilities>
              </hello>]]>]]>"""));
    }

    @Test
    public void testEmptyLines() throws Exception {
        // Adapted from https://bugs.openjdk.org/secure/attachment/93338/XmlBugExample.java
        final var input = """
            <users>
                <!-- pre-existing entry BEGIN -->
                <user>
                    <!-- a user -->
                    <name>A name</name>
                    <email>An email</email>
                </user>
                <!-- pre-existing entry END -->
            </users>
            """;

        assertEquals(input, XmlUtil.toString(XMLUnit.buildControlDocument(input)));
    }
}
