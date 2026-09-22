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
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class PhotoByYearApplicationTest {
    @TempDir
    Path tempDir;

    @Test
    void runCopiesJpgFilesToExpectedDirectoriesAndSkipsExistingFiles() throws IOException, URISyntaxException {
        Path source = Files.createDirectory(tempDir.resolve("source"));
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path nested = Files.createDirectory(source.resolve("nested"));

        copyResource("exif.jpg", source.resolve("exif.jpg"));
        copyResource("exif.jpg", source.resolve("fresh-exif.jpg"));
        copyResource("no-exif2.jpg", source.resolve("no-exif2.jpg"));
        copyResource("exif.jpg", nested.resolve("nested.jpg"));
        Files.writeString(source.resolve("ignored.txt"), "not a jpg", StandardCharsets.UTF_8);

        Path existingTarget = destination.resolve("2009/12/31/exif.jpg");
        Files.createDirectories(existingTarget.getParent());
        Files.writeString(existingTarget, "already here", StandardCharsets.UTF_8);

        CapturedOutput output = captureOutput(() -> new PhotoByYearApplication(source, destination).run());

        assertTrue(output.out().matches(
            Pattern.quote("Copying from '" + source.toAbsolutePath() + "' to '" + destination.toAbsolutePath() + "'")
                + lineSeparatorPattern()
                + "(?s:.*)"
        ));
        assertEquals("already here", Files.readString(existingTarget, StandardCharsets.UTF_8));
        assertTrue(Files.exists(destination.resolve("2009/12/31/fresh-exif.jpg")));
        assertTrue(Files.exists(destination.resolve("NoExif/no-exif2.jpg")));
        assertFalse(Files.exists(destination.resolve("ignored.txt")));
        assertFalse(Files.exists(destination.resolve("2009/12/31/nested.jpg")));
    }

    @Test
    void runCopiesUnreadableMetadataJpgToNoExif() throws IOException {
        Path source = Files.createDirectory(tempDir.resolve("source"));
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Files.writeString(source.resolve("invalid.jpg"), "not real jpeg data", StandardCharsets.UTF_8);

        assertDoesNotThrow(() -> new PhotoByYearApplication(source, destination).run());
        assertEquals("not real jpeg data", Files.readString(destination.resolve("NoExif/invalid.jpg"), StandardCharsets.UTF_8));
    }

    @Test
    void runDryRunReportsPlannedActionsWithoutChangingDestination() throws IOException, URISyntaxException {
        Path source = Files.createDirectory(tempDir.resolve("source"));
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Path nested = Files.createDirectory(source.resolve("nested"));

        copyResource("exif.jpg", source.resolve("exif.jpg"));
        copyResource("no-exif2.jpg", source.resolve("no-exif2.jpg"));
        copyResource("exif.jpg", nested.resolve("nested.jpg"));
        Files.writeString(source.resolve("ignored.txt"), "not a jpg", StandardCharsets.UTF_8);

        Path existingTarget = destination.resolve("2009/12/31/exif.jpg");
        Files.createDirectories(existingTarget.getParent());
        Files.writeString(existingTarget, "already here", StandardCharsets.UTF_8);

        CapturedOutput output = captureOutput(() -> new PhotoByYearApplication(
            source,
            destination,
            new PhotoByYearOptions(true)
        ).run());

        Path noExifTarget = destination.resolve("NoExif/no-exif2.jpg");
        assertEquals("already here", Files.readString(existingTarget, StandardCharsets.UTF_8));
        assertFalse(Files.exists(noExifTarget.getParent()));
        assertFalse(Files.exists(noExifTarget));
        assertFalse(Files.exists(destination.resolve("ignored.txt")));
        assertFalse(Files.exists(destination.resolve("2009/12/31/nested.jpg")));
        assertTrue(output.out().matches(
            Pattern.quote("Copying from '" + source.toAbsolutePath() + "' to '" + destination.toAbsolutePath() + "'")
                + lineSeparatorPattern()
                + Pattern.quote("[dry-run] Skipped '" + source.resolve("exif.jpg") + "' because destination already exists: '" + existingTarget + "'")
                + lineSeparatorPattern()
                + Pattern.quote("[dry-run] '" + source.resolve("no-exif2.jpg") + "' -> '" + noExifTarget + "' would be copied.")
                + lineSeparatorPattern()
        ));
        assertEquals("", output.err());
    }

    @Test
    void runThrowsWhenDirectoriesAreInvalid() {
        Path source = tempDir.resolve("missing-source");
        Path destination = tempDir.resolve("missing-destination");

        assertThrows(IllegalArgumentException.class, () -> new PhotoByYearApplication(source, destination).run());
    }

    @Test
    void runReportsScanFailuresWithoutStackTrace() throws IOException {
        Path source = Files.createDirectory(tempDir.resolve("source"));
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        SourceScanner scanner = new SourceScanner() {
            @Override
            java.util.List<Path> scan(Path source) throws IOException {
                throw new IOException("scan blocked");
            }
        };

        CapturedOutput output = captureOutput(() -> new PhotoByYearApplication(
            source,
            destination,
            scanner,
            new PhotoOrganizer(destination, new ExifPathExtractor())
        ).run());

        assertTrue(output.err().contains("Failed to scan source directory '" + source.toFile() + "'. scan blocked"));
        assertFalse(output.err().contains("Exception"));
    }

    private void copyResource(String resourceName, Path target) throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(resourceName);
        assertNotNull(resource, "Missing test resource: " + resourceName);
        Files.copy(Path.of(resource.toURI()), target);
    }

    private String lineSeparatorPattern() {
        return "\\R";
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
