/*
 * Copyright (c) 2018 WolkAbout Technology s.r.o.
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
package com.wolkabout.wolk.file_transfer;

import com.wolkabout.wolk.filemanagement.UrlFileDownloadSession;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class UrlFileDownloadSessionTest {

    private static final Logger LOG = LoggerFactory.getLogger(FileDownloadSessionTest.class);

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();
    @Mock
    UrlFileDownloadSession.Callback callbackMock;
    private UrlFileDownloadSession session;

    @Test
    public void SampleTest() {
        LOG.info("Hello World!");
    }
}
