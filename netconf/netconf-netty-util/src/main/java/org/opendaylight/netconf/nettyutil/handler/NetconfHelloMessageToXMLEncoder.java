/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.nettyutil.handler;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.xml.transform.TransformerException;
import org.opendaylight.netconf.api.NetconfMessage;
import org.opendaylight.netconf.api.messages.HelloMessage;
import org.opendaylight.netconf.api.messages.NetconfHelloMessageAdditionalHeader;

/**
 * Customized NetconfMessageToXMLEncoder that serializes additional header with
 * session metadata along with
 * {@link HelloMessage}
 * . Used by netconf clients to send information about the user, ip address,
 * protocol etc.
 *
 * <p>
 * Hello message with header example:
 *
 * <p>
 *
 * <pre>
 * {@code
 * [tomas;10.0.0.0/10000;tcp;1000;1000;;/home/tomas;;]
 * < hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
 * < capabilities>
 * < capability>urn:ietf:params:netconf:base:1.0< /capability>
 * < /capabilities>
 * < /hello>
 * }
 * </pre>
 */
public final class NetconfHelloMessageToXMLEncoder extends NetconfMessageToXMLEncoder {
    @Override
    @VisibleForTesting
    public void encode(final ChannelHandlerContext ctx, final NetconfMessage msg, final ByteBuf out)
            throws IOException, TransformerException {
        Preconditions.checkState(msg instanceof HelloMessage, "Netconf message of type %s expected, was %s",
                HelloMessage.class, msg.getClass());
        Optional<NetconfHelloMessageAdditionalHeader> headerOptional = ((HelloMessage) msg).getAdditionalHeader();

        // If additional header present, serialize it along with netconf hello message
        if (headerOptional.isPresent()) {
            out.writeBytes(headerOptional.orElseThrow().toFormattedString().getBytes(StandardCharsets.UTF_8));
        }

        super.encode(ctx, msg, out);
    }
}
