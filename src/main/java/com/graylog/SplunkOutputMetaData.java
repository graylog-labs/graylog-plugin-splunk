package com.graylog;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SplunkOutputMetaData implements PluginMetaData {
    @Override
    public String getUniqueId() {
        return "com.graylog.SplunkOutputPlugin";
    }

    @Override
    public String getName() {
        return "SplunkOutput";
    }

    @Override
    public String getAuthor() {
        return "Lennart Koopmann (Graylog, Inc)";
    }

    @Override
    public URI getURL() {
        return URI.create("https://www.graylog.com/");
    }

    @Override
    public Version getVersion() {
        return new Version(0, 1, 0);
    }

    @Override
    public String getDescription() {
        return "Writes messages to your Splunk installation via UDP or TCP.";
    }

    @Override
    public Version getRequiredVersion() {
        return new Version(1, 0, 0);
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return new HashSet<ServerStatus.Capability>() {{
            add(ServerStatus.Capability.SERVER);
        }};
    }
}
