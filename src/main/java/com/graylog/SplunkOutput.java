package com.graylog;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.graylog.senders.TCPSender;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;
import com.graylog.senders.Sender;

import java.util.List;
import java.util.Map;

public class SplunkOutput implements MessageOutput {

    private static final String CK_SPLUNK_HOST = "splunk_host";
    private static final String CK_SPLUNK_PORT = "splunk_port";
    private static final String CK_SPLUNK_PROTOCOL = "splunk_protocol";

    private boolean running = true;

    private final Configuration configuration;

    private final Sender sender;

    @Inject
    public SplunkOutput(@Assisted Stream stream, @Assisted Configuration configuration) throws MessageOutputConfigurationException {
        this.configuration = configuration;

        // Check configuration.
        if (!checkConfiguration(configuration)) {
            throw new MessageOutputConfigurationException("Missing configuration.");
        }

        // Set up sender.
        sender = new TCPSender(
                configuration.getString(CK_SPLUNK_HOST),
                configuration.getInt(CK_SPLUNK_PORT)
        );
        sender.initialize();

        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void write(Message message) throws Exception {
        if (message == null || message.getFields() == null || message.getFields().isEmpty()) {
            return;
        }

        sender.send(message);
    }

    @Override
    public void write(List<Message> list) throws Exception {
        if (list == null) {
            return;
        }

        for(Message m : list) {
            write(m);
        }
    }

    public boolean checkConfiguration(Configuration c) {
        return c.stringIsSet(CK_SPLUNK_HOST)
                && c.intIsSet(CK_SPLUNK_PORT)
                && c.stringIsSet(CK_SPLUNK_PROTOCOL)
                && (c.getString(CK_SPLUNK_PROTOCOL).equals("UDP") || c.getString(CK_SPLUNK_PROTOCOL).equals("TCP"));
    }

    @FactoryClass
    public interface Factory extends MessageOutput.Factory<SplunkOutput> {
        @Override
        SplunkOutput create(Stream stream, Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Config extends MessageOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest configurationRequest = new ConfigurationRequest();

            configurationRequest.addField(new TextField(
                            CK_SPLUNK_HOST, "Splunk Host", "",
                            "Hostname or IP address of a Splunk instance",
                            ConfigurationField.Optional.NOT_OPTIONAL)
            );

            configurationRequest.addField(new NumberField(
                            CK_SPLUNK_PORT, "Splunk Port", 12999,
                            "Port of a Splunk instance",
                            ConfigurationField.Optional.OPTIONAL)
            );

            final Map<String, String> protocols = ImmutableMap.of(
                    "TCP", "TCP",
                    "UDP", "UDP");
            configurationRequest.addField(new DropdownField(
                            CK_SPLUNK_PROTOCOL, "Splunk Protocol", "TCP", protocols,
                            "Protocol that should be used to send messages to Splunk",
                            ConfigurationField.Optional.OPTIONAL)
            );

            return configurationRequest;
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Splunk", false, "", "Writes messages to your Splunk installation via UDP or TCP.");
        }
    }

}
