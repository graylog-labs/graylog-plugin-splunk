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
package com.graylog.splunk.output;

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
        return "com.graylog.splunk.output.SplunkOutputPlugin";
    }

    @Override
    public String getName() {
        return "Splunk Output";
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
        return new Version(0, 3, 0);
    }

    @Override
    public String getDescription() {
        return "Writes messages to your Splunk installation via TCP.";
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
