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

import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class MainTest {
    @TempDir
    Path tempDir;

    @Test
    public void extractPathNullArg() {
        assertThrows(NullPointerException.class, () -> Main.extractPath(null));
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

    @Test
    public void runCopiesJpgFilesToExpectedDirectoriesAndSkipsExistingFiles() throws IOException, URISyntaxException {
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

        new Main(source.toFile(), destination.toFile()).run();

        assertEquals("already here", Files.readString(existingTarget, StandardCharsets.UTF_8));
        assertTrue(Files.exists(destination.resolve("2009/12/31/fresh-exif.jpg")));
        assertTrue(Files.exists(destination.resolve("NoExif/no-exif2.jpg")));
        assertFalse(Files.exists(destination.resolve("ignored.txt")));
        assertFalse(Files.exists(destination.resolve("2009/12/31/nested.jpg")));
    }

    @Test
    public void runCopiesUnreadableMetadataJpgToNoExif() throws IOException {
        Path source = Files.createDirectory(tempDir.resolve("source"));
        Path destination = Files.createDirectory(tempDir.resolve("destination"));
        Files.writeString(source.resolve("invalid.jpg"), "not real jpeg data", StandardCharsets.UTF_8);

        assertDoesNotThrow(() -> new Main(source.toFile(), destination.toFile()).run());
        assertEquals("not real jpeg data", Files.readString(destination.resolve("NoExif/invalid.jpg"), StandardCharsets.UTF_8));
    }

    @Test
    public void ensureDirectoriesNullArgs() {
        assertThrows(NullPointerException.class, () -> Main.ensureDirectories(null, null));
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

        assertFalse(Main.ensureDirectories(doesntExists, ok),
            "Should be false if source doesn't exists");
        assertFalse(Main.ensureDirectories(existsNoDir, ok),
            "Should be false if source is not a directory");

        assertFalse(Main.ensureDirectories(ok, doesntExists),
            "Should be false if destination doesn't exists");
        assertFalse(Main.ensureDirectories(ok, existsNoDir),
            "Should be false if destination is not a directory");

        assertFalse(Main.ensureDirectories(ok, ok),
            "Should be false if both source and destination points to the same directory");

        assertTrue(Main.ensureDirectories(ok, ok2),
            "Everything should be ok in this case");
    }

    @Test
    public void runWrongDirs() {
        Main m = new Main(mock(File.class), mock(File.class));
        Main mSpy = spy(m);
        when(mSpy.ensureDirectories(mock(File.class), mock(File.class))).thenReturn(false);

        // if the ensureDirectories gives a false, it should raise an IAE.
        assertThrows(IllegalArgumentException.class, mSpy::run);
    }

    @Test
    public void parseMetaNullMeta() {
        assertThrows(NullPointerException.class, () -> Main.parseMeta(null));
    }

    @Test
    public void parseMetaNoExif() throws ImagingException {
        TiffImageMetadata exif = mock(TiffImageMetadata.class);
        when(exif.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL)).thenReturn(null);
        JpegImageMetadata meta = new JpegImageMetadata(null, exif);

        assertEquals(Main.NO_EXIF_PATH, Main.parseMeta(meta));
    }

    @Test
    public void parseMetaColon() throws ImagingException {
        TiffField dateTime = mock(TiffField.class);
        when(dateTime.getStringValue()).thenReturn("2009:12:31 10:11:12");

        TiffImageMetadata exif = mock(TiffImageMetadata.class);
        when(exif.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL)).thenReturn(dateTime);

        JpegImageMetadata meta = new JpegImageMetadata(null, exif);

        assertEquals("2009/12/31/", Main.parseMeta(meta));
    }

    @Test
    public void parseMetaDashesAndColon() throws ImagingException {
        TiffField dateTime = mock(TiffField.class);
        when(dateTime.getStringValue()).thenReturn("2018-06-01 13:53:00");

        TiffImageMetadata exif = mock(TiffImageMetadata.class);
        when(exif.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL)).thenReturn(dateTime);

        JpegImageMetadata meta = new JpegImageMetadata(null, exif);

        assertEquals("2018/06/01/", Main.parseMeta(meta));
    }

    @Test
    public void parseMetaWrongFormat() throws ImagingException {
        TiffField dateTime = mock(TiffField.class);
        when(dateTime.getStringValue()).thenReturn("just a wrong fo:rm:at");

        TiffImageMetadata exif = mock(TiffImageMetadata.class);
        when(exif.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL)).thenReturn(dateTime);

        JpegImageMetadata meta = new JpegImageMetadata(null, exif);

        assertEquals(Main.NO_EXIF_PATH, Main.parseMeta(meta));
    }

    private void copyResource(String resourceName, Path target) throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(resourceName);
        assertNotNull(resource, "Missing test resource: " + resourceName);
        Files.copy(Path.of(resource.toURI()), target);
    }
}
