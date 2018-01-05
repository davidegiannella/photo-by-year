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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class MainTest {
    @Test(expected = NullPointerException.class)
    public void extractPathNullArg() {
        Main.extractPath(null);
    }

    @Test
    public void extractPathNoExif() throws URISyntaxException {
        URL resource = this.getClass().getClassLoader().getResource("no-exif2.jpg");
        File image = new File(resource.toURI());
        assertEquals(Main.NO_EXIF_PATH, Main.extractPath(image));
    }

    @Test
    public void extractPath() throws URISyntaxException {
        URL resource = this.getClass().getClassLoader().getResource("exif.jpg");
        File image = new File(resource.toURI());
        assertEquals("2009/12/31/", Main.extractPath(image));
    }

    @Test(expected = NullPointerException.class)
    public void ensureDirectoriesNullArgs() throws URISyntaxException {
        Main.ensureDirectories(null, null);
    }

    @Test
    public void ensureDirectories() {
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

        assertFalse("Should be false if source doesn't exists",
            Main.ensureDirectories(doesntExists, ok));
        assertFalse("Should be false if source is not a directory",
            Main.ensureDirectories(existsNoDir, ok));

        assertFalse("Should be false if destination doesn't exists",
            Main.ensureDirectories(ok, doesntExists));
        assertFalse("Should be false if destination is not a directory",
            Main.ensureDirectories(ok, existsNoDir));

        assertFalse("Should be false if both source and destination points to the same directory",
            Main.ensureDirectories(ok, ok));

        assertTrue("Everything should be ok in this case", Main.ensureDirectories(ok, ok2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void runWrongDirs() {
        Main m = new Main(mock(File.class), mock(File.class));
        Main mSpy = spy(m);
        when(mSpy.ensureDirectories(mock(File.class), mock(File.class))).thenReturn(false);

        // if the ensureDirectories gives a false, it should raise an IAE.
        mSpy.run();
    }
}
