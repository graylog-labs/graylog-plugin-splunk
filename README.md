# Splunk output plugin for Graylog

This plugin enables you to forward a stream of data from Graylog to any Splunk
setup.

![](https://github.com/Graylog2/plugin-output-splunk/blob/master/images/screenshot1.png)

## Use cases



## Installing the plugin

[Download the plugin](https://github.com/Graylog2/plugin-output-splunk/releases)
and place the `.jar` file in your Graylog plugin directory. The plugin directory
is the `plugins/` folder relative from your `graylog-server` directory by default
and can be configured in your `graylog.conf` file.

Restart `graylog-server` and you are done.

## Configuring Splunk

In your Splunk web interface, go to *Settings -> Data Inputs* and add a new
TCP input. Use any port and leave both the *Source name override* and
*Only accept connection from* configuration options empty.

Click on *Next* to configure more details of the data input Graylog will send
data to.

Set the *Sourcetype* to *Miscellaneous -> generic_single_line* and leave the
other options as they are. If you know what you are doing you can of course
change any other settings as you wish.

![](https://github.com/Graylog2/plugin-output-splunk/blob/master/images/screenshot2.png)

Click on *Review* and then *Submit*. Remember the TCP port you configured
because you will have to configure Graylog to send data to it in the next step.

## Configuring Graylog

In Graylog, go to the outputs configuration of a stream and add a new "Splunk
output" like this:

![](https://github.com/Graylog2/plugin-output-splunk/blob/master/images/screenshot3.png)

All messages coming into that stream should now be forwarded to your Splunk setup
in realtime.
