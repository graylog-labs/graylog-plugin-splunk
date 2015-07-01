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

    public TCPSender(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    @Override
    public void initialize() {
        group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(group).channel(NioSocketChannel.class);

        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new StringEncoder(Charsets.UTF_8));
            }
        });

        try {
            channel = b.connect(hostname, port).sync().channel();
        } catch (InterruptedException ignored) {
            // noop
        }
    }

    @Override
    public void stop() {
        if (group != null) {
            group.shutdownGracefully();
        }
    }

    @Override
    public void send(Message message) {
        if(channel == null || !channel.isActive()) {
            LOG.error("Could not send message. Channel not open.");
            return;
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

        channel.writeAndFlush(splunkMessage.append("\r\n").toString());
    }

    private Object noNewLines(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            value = ((String) value).replace("\n", "").replace("\r", "");
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
