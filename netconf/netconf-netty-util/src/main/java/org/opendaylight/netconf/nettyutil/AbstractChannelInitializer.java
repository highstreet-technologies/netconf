/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.nettyutil;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;
import org.opendaylight.netconf.api.NetconfSession;
import org.opendaylight.netconf.api.messages.FramingMechanism;
import org.opendaylight.netconf.nettyutil.handler.FramingMechanismHandlerFactory;
import org.opendaylight.netconf.nettyutil.handler.NetconfEOMAggregator;
import org.opendaylight.netconf.nettyutil.handler.NetconfHelloMessageToXMLEncoder;
import org.opendaylight.netconf.nettyutil.handler.NetconfXMLToHelloMessageDecoder;

public abstract class AbstractChannelInitializer<S extends NetconfSession> {
    public static final String NETCONF_MESSAGE_DECODER = "netconfMessageDecoder";
    public static final String NETCONF_MESSAGE_AGGREGATOR = "aggregator";
    public static final String NETCONF_MESSAGE_ENCODER = "netconfMessageEncoder";
    public static final String NETCONF_MESSAGE_FRAME_ENCODER = "frameEncoder";
    public static final String NETCONF_SESSION_NEGOTIATOR = "negotiator";

    public void initialize(final Channel ch, final Promise<S> promise) {
        ch.pipeline().addLast(NETCONF_MESSAGE_AGGREGATOR, new NetconfEOMAggregator());
        initializeMessageDecoder(ch);
        ch.pipeline().addLast(NETCONF_MESSAGE_FRAME_ENCODER,
                FramingMechanismHandlerFactory.createHandler(FramingMechanism.EOM));
        initializeMessageEncoder(ch);

        initializeSessionNegotiator(ch, promise);
    }

    protected void initializeMessageEncoder(final Channel ch) {
        // Special encoding handler for hello message to include additional header if available,
        // it is thrown away after successful negotiation
        ch.pipeline().addLast(NETCONF_MESSAGE_ENCODER, new NetconfHelloMessageToXMLEncoder());
    }

    protected void initializeMessageDecoder(final Channel ch) {
        // Special decoding handler for hello message to parse additional header if available,
        // it is thrown away after successful negotiation
        ch.pipeline().addLast(NETCONF_MESSAGE_DECODER, new NetconfXMLToHelloMessageDecoder());
    }

    /**
     * Insert session negotiator into the pipeline. It must be inserted after message decoder
     * identified by {@link AbstractChannelInitializer#NETCONF_MESSAGE_DECODER}, (or any other custom decoder processor)
     */
    protected abstract void initializeSessionNegotiator(Channel ch, Promise<S> promise);
}
