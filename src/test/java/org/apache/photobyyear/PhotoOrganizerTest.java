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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PhotoOrganizerTest {
    @TempDir
    Path tempDir;

    @Test
    void copyPictureNullPicture() {
        PhotoOrganizer organizer = new PhotoOrganizer(tempDir, new ExifPathExtractor());

        assertThrows(NullPointerException.class, () -> organizer.copyPicture(null));
    }

    @Test
    void copyPictureCopiesFileToExtractedPathAndReportsDestination() throws IOException {
        Path source = Files.writeString(tempDir.resolve("photo.jpg"), "content", StandardCharsets.UTF_8);
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        ExifPathExtractor extractor = mock(ExifPathExtractor.class);
        when(extractor.extractPath(source.toFile())).thenReturn("2009/12/31/");

        CapturedOutput output = captureOutput(() -> new PhotoOrganizer(destination, extractor).copyPicture(source));

        Path target = destination.resolve("2009/12/31/photo.jpg");
        assertEquals("content", Files.readString(target, StandardCharsets.UTF_8));
        assertTrue(output.out().contains("'" + source + "' -> '" + target + "' done."));
    }

    @Test
    void copyPictureReportsSkippedExistingFileAsNormalOutput() throws IOException {
        Path source = Files.writeString(tempDir.resolve("photo.jpg"), "new content", StandardCharsets.UTF_8);
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path existingTarget = destination.resolve("2009/12/31/photo.jpg");
        Files.createDirectories(existingTarget.getParent());
        Files.writeString(existingTarget, "already here", StandardCharsets.UTF_8);
        ExifPathExtractor extractor = mock(ExifPathExtractor.class);
        when(extractor.extractPath(source.toFile())).thenReturn("2009/12/31/");

        CapturedOutput output = captureOutput(() -> new PhotoOrganizer(destination, extractor).copyPicture(source));

        assertEquals("already here", Files.readString(existingTarget, StandardCharsets.UTF_8));
        assertTrue(output.out().contains("Skipped '" + source + "' because destination already exists: '" + existingTarget + "'"));
        assertFalse(output.err().contains("Skipping"));
        assertFalse(output.err().contains("Exception"));
    }

    @Test
    void copyPictureDryRunReportsPlannedCopyWithoutWriting() throws IOException {
        Path source = Files.writeString(tempDir.resolve("photo.jpg"), "content", StandardCharsets.UTF_8);
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path target = destination.resolve("2009/12/31/photo.jpg");
        ExifPathExtractor extractor = mock(ExifPathExtractor.class);
        when(extractor.extractPath(source.toFile())).thenReturn("2009/12/31/");

        CapturedOutput output = captureOutput(() -> new PhotoOrganizer(
            destination,
            extractor,
            new PhotoByYearOptions(true)
        ).copyPicture(source));

        assertFalse(Files.exists(target.getParent()));
        assertFalse(Files.exists(target));
        assertTrue(output.out().contains("[dry-run] '" + source + "' -> '" + target + "' would be copied."));
        assertEquals("", output.err());
    }

    @Test
    void copyPictureDryRunReportsSkippedExistingFileWithoutOverwriting() throws IOException {
        Path source = Files.writeString(tempDir.resolve("photo.jpg"), "new content", StandardCharsets.UTF_8);
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path existingTarget = destination.resolve("2009/12/31/photo.jpg");
        Files.createDirectories(existingTarget.getParent());
        Files.writeString(existingTarget, "already here", StandardCharsets.UTF_8);
        ExifPathExtractor extractor = mock(ExifPathExtractor.class);
        when(extractor.extractPath(source.toFile())).thenReturn("2009/12/31/");

        CapturedOutput output = captureOutput(() -> new PhotoOrganizer(
            destination,
            extractor,
            new PhotoByYearOptions(true)
        ).copyPicture(source));

        assertEquals("already here", Files.readString(existingTarget, StandardCharsets.UTF_8));
        assertTrue(output.out().contains("[dry-run] Skipped '" + source + "' because destination already exists: '" + existingTarget + "'"));
        assertEquals("", output.err());
    }

    @Test
    void copyPictureReportsCopyFailuresWithoutStackTrace() throws IOException {
        Path source = Files.writeString(tempDir.resolve("photo.jpg"), "content", StandardCharsets.UTF_8);
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path blockedDirectory = destination.resolve("2009");
        Files.writeString(blockedDirectory, "not a directory", StandardCharsets.UTF_8);
        Path blockedTarget = destination.resolve("2009/12/31/photo.jpg");
        ExifPathExtractor extractor = mock(ExifPathExtractor.class);
        when(extractor.extractPath(source.toFile())).thenReturn("2009/12/31/");

        CapturedOutput output = captureOutput(() -> new PhotoOrganizer(destination, extractor).copyPicture(source));

        assertTrue(output.err().contains("Failed to copy '" + source + "' to '" + blockedTarget + "'"));
        assertFalse(output.err().contains("Exception"));
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
