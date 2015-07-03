/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.graylog.senders;

import com.google.common.base.Charsets;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TCPSender implements Sender {

    private static final Logger LOG = LoggerFactory.getLogger(TCPSender.class);

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

    private final String hostname;
    private final int port;

    private Channel channel = null;
    private EventLoopGroup group;

    private Bootstrap bootstrap;

    public TCPSender(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    @Override
    public void initialize() {
        connect();
    }

    private void connect() {
        try {
            LOG.info("Connecting to Splunk.");
            group = new NioEventLoopGroup();
            bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class);

            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new StringEncoder(Charsets.UTF_8));
                }
            });

            channel = bootstrap.connect(hostname, port).sync().channel();
        } catch (Exception e) {
            LOG.error("Error when trying to connect to Splunk.", e);
            disconnect();
        }
    }

    private void disconnect() {
        try {
            if (channel != null) {
                channel.close();
                channel.deregister();
            }
        } catch(Exception e) {
            LOG.warn("Could not close channel. Attempting graceful shutdown of eventloop group next.");
        } finally {
            if (group != null) {
                group.shutdownGracefully();
                LOG.debug("Eventloop group shutdown.");
            }
        }
    }

    @Override
    public void stop() {
        disconnect();
    }

    @Override
    public void send(Message message) {
        if(!isConnected()) {
            LOG.info("Channel not open. Reconnecting.");
            disconnect();
            connect();

            if (!isInitialized()) {
                LOG.warn("Channel still not open. Rejecting this message.");
                return;
            }
        }

        StringBuilder splunkMessage = new StringBuilder();
        splunkMessage.append(message.getTimestamp().toString("yyyy/MM/dd-HH:mm:ss.SSS"))
                .append(" ")
                .append(noNewLines(message.getMessage()))
                .append(" source=").append(escape(message.getField(Message.FIELD_SOURCE)));

        for (Map.Entry<String, Object> field : message.getFields().entrySet()) {
            if (Message.RESERVED_FIELDS.contains(field.getKey()) || field.getKey().equals(Message.FIELD_STREAMS)) {
                continue;
            }

            splunkMessage.append(" ").append(field.getKey()).append("=").append(escape(field.getValue()));
        }

        if (isInitialized()) {
            channel.writeAndFlush(splunkMessage.append("\r\n").toString());
        }
    }

    @Override
    public boolean isInitialized() {
        return channel != null && channel.isActive();
    }

    private boolean isConnected() {
        return isInitialized();
    }

    private Object noNewLines(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            value = ((String) value).replace("\n", " ").replace("\r", " ");
        }

        return value;
    }

    private Object escape(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            Matcher matcher = WHITESPACE_PATTERN.matcher((String) value);
            if (matcher.find()) {
                StringBuilder sb = new StringBuilder()
                    .append("\"").append(value).append("\"");
                value = sb.toString();
            }
        }

        return noNewLines(value);
    }

}
