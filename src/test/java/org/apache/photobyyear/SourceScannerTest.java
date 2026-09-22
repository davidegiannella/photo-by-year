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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SourceScannerTest {
    @TempDir
    Path tempDir;

    private final SourceScanner scanner = new SourceScanner();

    @Test
    void scanNullSource() {
        assertThrows(NullPointerException.class, () -> scanner.scan(null));
    }

    @Test
    void scanReturnsOnlyDirectJpgAndJpegFiles() throws IOException {
        Path source = Files.createDirectory(tempDir.resolve("source"));
        Path nested = Files.createDirectory(source.resolve("nested"));
        Path jpg = Files.writeString(source.resolve("photo.jpg"), "jpg", StandardCharsets.UTF_8);
        Path uppercaseJpg = Files.writeString(source.resolve("uppercase.JPG"), "jpg", StandardCharsets.UTF_8);
        Path jpeg = Files.writeString(source.resolve("photo.jpeg"), "jpeg", StandardCharsets.UTF_8);
        Path uppercaseJpeg = Files.writeString(source.resolve("uppercase.JPEG"), "jpeg", StandardCharsets.UTF_8);
        Files.writeString(source.resolve("ignored.jpe"), "jpe", StandardCharsets.UTF_8);
        Files.writeString(source.resolve("ignored.txt"), "txt", StandardCharsets.UTF_8);
        Files.writeString(nested.resolve("nested.jpg"), "jpg", StandardCharsets.UTF_8);
        Files.writeString(nested.resolve("nested.jpeg"), "jpeg", StandardCharsets.UTF_8);

        List<Path> pictures = scanner.scan(source);

        assertEquals(4, pictures.size());
        assertTrue(pictures.contains(jpg));
        assertTrue(pictures.contains(uppercaseJpg));
        assertTrue(pictures.contains(jpeg));
        assertTrue(pictures.contains(uppercaseJpeg));
        assertFalse(pictures.contains(nested.resolve("nested.jpg")));
        assertFalse(pictures.contains(nested.resolve("nested.jpeg")));
    }
}
