/*
 * Copyright (c) 2017 WolkAbout Technology s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.wolkabout.wolk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class CommandQueue extends LinkedBlockingQueue<CommandQueue.Command> {
    private static final Logger LOG = LoggerFactory.getLogger(CommandQueue.class);

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    public CommandQueue() {
        executorService.scheduleAtFixedRate((new Runnable() {
            @Override
            public void run() {
                try {
                    take().execute();
                } catch (InterruptedException e) {
                    LOG.info("Command execution interrupted", e);
                } catch (Exception e) {
                    LOG.error("Command execution failed", e);
                }
            }
        }), 0, 5, TimeUnit.MILLISECONDS);
    }

    public interface Command {
        void execute();
    }
}
