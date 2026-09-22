/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.photobyyear;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DirectoryValidatorTest {
    @Test
    void ensureDirectoriesNullArgs() {
        assertThrows(NullPointerException.class, () -> DirectoryValidator.ensureDirectories(null, null));
    }

    @Test
    void ensureDirectories() {
        File ok = mock(File.class);
        when(ok.isDirectory()).thenReturn(true);
        when(ok.exists()).thenReturn(true);
        when(ok.getAbsolutePath()).thenReturn("/path1");

        File ok2 = mock(File.class);
        when(ok2.isDirectory()).thenReturn(true);
        when(ok2.exists()).thenReturn(true);
        when(ok2.getAbsolutePath()).thenReturn("/path2");

        File existsNoDir = mock(File.class);
        when(existsNoDir.isDirectory()).thenReturn(false);
        when(existsNoDir.exists()).thenReturn(true);

        File doesntExists = mock(File.class);
        when(doesntExists.isDirectory()).thenReturn(false);
        when(doesntExists.exists()).thenReturn(false);

        assertFalse(DirectoryValidator.ensureDirectories(doesntExists, ok),
            "Should be false if source doesn't exists");
        assertFalse(DirectoryValidator.ensureDirectories(existsNoDir, ok),
            "Should be false if source is not a directory");

        assertFalse(DirectoryValidator.ensureDirectories(ok, doesntExists),
            "Should be false if destination doesn't exists");
        assertFalse(DirectoryValidator.ensureDirectories(ok, existsNoDir),
            "Should be false if destination is not a directory");

        assertFalse(DirectoryValidator.ensureDirectories(ok, ok),
            "Should be false if both source and destination points to the same directory");

        assertTrue(DirectoryValidator.ensureDirectories(ok, ok2),
            "Everything should be ok in this case");
    }
}
