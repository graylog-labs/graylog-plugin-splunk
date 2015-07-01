package com.graylog.senders;

import org.graylog2.plugin.Message;

public interface Sender {

    void initialize();
    void stop();

    void send(Message message);

}
