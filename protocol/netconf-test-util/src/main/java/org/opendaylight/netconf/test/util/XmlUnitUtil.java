/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.test.util;

import static org.custommonkey.xmlunit.XMLAssert.assertNodeTestPasses;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.custommonkey.xmlunit.AbstractNodeTester;
import org.custommonkey.xmlunit.NodeTest;
import org.custommonkey.xmlunit.NodeTestException;
import org.custommonkey.xmlunit.NodeTester;
import org.opendaylight.netconf.api.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public final class XmlUnitUtil {
    private XmlUnitUtil() {
        // Hidden on purpose
    }

    public static void assertContainsElementWithText(final Document doc, final String textToFind)
            throws NodeTestException {
        NodeTest nt = new NodeTest(doc);
        NodeTester tester = new AbstractNodeTester() {

            boolean textFound = false;

            @Override
            public void testText(final Text text) throws NodeTestException {
                if (!textFound) {
                    if (text.getData().equalsIgnoreCase(textToFind)) {
                        textFound = true;
                    }
                }
            }

            @Override
            public void noMoreNodes(final NodeTest forTest) throws NodeTestException {
                assertTrue(textFound);
            }
        };
        assertNodeTestPasses(nt, tester, new short[]{Node.TEXT_NODE}, true);
    }

    public static void assertContainsElement(final Document doc, final Element testElement) throws NodeTestException {
        NodeTest nt = new NodeTest(doc);
        NodeTester tester = new AbstractNodeTester() {

            private boolean elementFound = false;

            @Override
            public void testElement(final Element element) throws NodeTestException {
                if (!elementFound) {
                    if (element.isEqualNode(testElement)) {
                        elementFound = true;
                    }
                }
            }

            @Override
            public void noMoreNodes(final NodeTest forTest) throws NodeTestException {
                assertTrue(elementFound);
            }
        };
        assertNodeTestPasses(nt, tester, new short[]{Node.ELEMENT_NODE}, true);
    }

    public static void assertContainsElementWithName(final Document doc,
                                                     final String elementName) throws NodeTestException {
        NodeTest nt = new NodeTest(doc);
        NodeTester tester = new AbstractNodeTester() {

            private boolean elementFound = false;

            @Override
            public void testElement(final Element element) throws NodeTestException {
                if (!elementFound) {
                    if (element.getNodeName() != null && element.getNodeName().equals(elementName)) {
                        elementFound = true;
                    }
                }
            }

            @Override
            public void noMoreNodes(final NodeTest forTest) throws NodeTestException {
                assertTrue(XmlUtil.toString(doc), elementFound);
            }
        };
        assertNodeTestPasses(nt, tester, new short[]{Node.ELEMENT_NODE}, true);
    }

    public static void assertElementsCount(final Document doc, final String elementName, final int expectedCount) {
        NodeTest nt = new NodeTest(doc);
        NodeTester tester = new AbstractNodeTester() {

            private int elementFound = 0;

            @Override
            public void testElement(final Element element) throws NodeTestException {
                if (element.getNodeName() != null && element.getNodeName().equals(elementName)) {
                    elementFound++;
                }
            }

            @Override
            public void noMoreNodes(final NodeTest forTest) throws NodeTestException {
                assertEquals(expectedCount, elementFound);
            }
        };
        assertNodeTestPasses(nt, tester, new short[]{Node.ELEMENT_NODE}, true);
    }
}
