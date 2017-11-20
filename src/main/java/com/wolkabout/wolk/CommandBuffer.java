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

import java.util.concurrent.*;

class CommandBuffer extends LinkedBlockingQueue<CommandBuffer.Command> {
    public interface Command {
        void execute();
    }

    private static final Logger LOG = LoggerFactory.getLogger(CommandBuffer.class);

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    public CommandBuffer() {
        executorService.scheduleAtFixedRate((new Runnable() {
            @Override
            public void run() {
                try {
                    final CommandBuffer.Command command = take();
                    command.execute();
                } catch (InterruptedException e) {
                    LOG.info("Command execution interrupted", e);
                } catch (Exception e) {
                    LOG.error("Command execution failed", e);
                }
            }
        }), 0, 5, TimeUnit.MILLISECONDS);
    }
}
