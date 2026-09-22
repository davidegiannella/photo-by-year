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
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    @TempDir
    Path tempDir;

    @Test
    void mainSupportsDryRunOption() throws IOException, URISyntaxException {
        Path source = Files.createDirectory(tempDir.resolve("source"));
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        copyResource("exif.jpg", source.resolve("exif.jpg"));

        CapturedOutput output = captureOutput(() -> assertDoesNotThrow(() -> Main.main(new String[] {
            "--dry-run",
            "-s", source.toString(),
            "-d", destination.toString()
        })));

        Path target = destination.resolve("2009/12/31/exif.jpg");
        assertFalse(Files.exists(target.getParent()));
        assertFalse(Files.exists(target));
        assertTrue(output.out().contains("[dry-run] '" + source.resolve("exif.jpg") + "' -> '" + target + "' would be copied."));
        assertEquals("", output.err());
    }

    private void copyResource(String resourceName, Path target) throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(resourceName);
        assertNotNull(resource, "Missing test resource: " + resourceName);
        Files.copy(Path.of(resource.toURI()), target);
    }

    private CapturedOutput captureOutput(Runnable runnable) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(out));
            System.setErr(new PrintStream(err));
            runnable.run();
            return new CapturedOutput(out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private record CapturedOutput(String out, String err) {
    }
}
