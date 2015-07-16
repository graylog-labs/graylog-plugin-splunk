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

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SplunkSenderThread {

    private static final Logger LOG = LoggerFactory.getLogger(SplunkSenderThread.class);
    private final ReentrantLock lock;
    private final Condition connectedCond;
    private final AtomicBoolean keepRunning = new AtomicBoolean(true);
    private final Thread senderThread;
    private Channel channel;

    public SplunkSenderThread(final BlockingQueue<String> queue) {
        this.lock = new ReentrantLock();
        this.connectedCond = lock.newCondition();

        this.senderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String message = null;

                while (keepRunning.get()) {
                    // wait until we are connected to the Splunk server before polling log events from the queue
                    lock.lock();
                    try {
                        while (channel == null || !channel.isActive()) {
                            try {
                                connectedCond.await();
                            } catch (InterruptedException e) {
                                if (!keepRunning.get()) {
                                    // bail out if we are awoken because the application is stopping
                                    break;
                                }
                            }
                        }
                        // we are connected, let's start sending logs
                        try {
                            // if we have a lingering event already, try to send that instead of polling a new one.
                            if (message == null) {
                                message = queue.poll(100, TimeUnit.MILLISECONDS);
                            }
                            // if we are still connected, convert LoggingEvent to Splunk and send it
                            // but if we aren't connected anymore, we'll have already pulled an event from the queue,
                            // which we keep hanging around in this thread and in the next loop iteration will block until we are connected again.
                            if (message != null && channel != null && channel.isActive()) {
                                // Write the Splunk message to the pipeline. The protocol specific channel handler
                                // will take care of encoding.
                                channel.writeAndFlush(message);
                                message = null;
                            }
                        } catch (InterruptedException e) {
                            // ignore, when stopping keepRunning will be set to false outside
                        }
                    } finally {
                        lock.unlock();
                    }
                }

                LOG.debug("SplunkSenderThread exiting!");
            }
        });

        this.senderThread.setName("SplunkSenderThread-" + senderThread.getId());
    }

    public void start(Channel channel) {
        lock.lock();
        try {
            this.channel = channel;
            this.connectedCond.signalAll();
        } finally {
            lock.unlock();
        }
        senderThread.start();
    }

    public void stop() {
        keepRunning.set(false);
        senderThread.interrupt();
    }

}
