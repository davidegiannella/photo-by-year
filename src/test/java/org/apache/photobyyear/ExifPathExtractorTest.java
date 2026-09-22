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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExifPathExtractorTest {
    private final ExifPathExtractor extractor = new ExifPathExtractor();

    @Test
    void extractPathNullArg() {
        assertThrows(NullPointerException.class, () -> extractor.extractPath(null));
    }

    @Test
    void extractPathNoExif() throws URISyntaxException {
        URL resource = this.getClass().getClassLoader().getResource("no-exif2.jpg");
        assertNotNull(resource, "Missing test resource: no-exif2.jpg");
        File image = new File(resource.toURI());

        assertEquals(ExifPathExtractor.NO_EXIF_PATH, extractor.extractPath(image));
    }

    @Test
    void extractPath() throws URISyntaxException {
        URL resource = this.getClass().getClassLoader().getResource("exif.jpg");
        assertNotNull(resource, "Missing test resource: exif.jpg");
        File image = new File(resource.toURI());

        assertEquals("2009/12/31/", extractor.extractPath(image));
    }

    @Test
    void parseMetaNullMeta() {
        assertThrows(NullPointerException.class, () -> extractor.parseMeta(null));
    }

    @Test
    void parseMetaNoExif() throws ImagingException {
        TiffImageMetadata exif = mock(TiffImageMetadata.class);
        when(exif.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL)).thenReturn(null);
        JpegImageMetadata meta = new JpegImageMetadata(null, exif);

        assertEquals(ExifPathExtractor.NO_EXIF_PATH, extractor.parseMeta(meta));
    }

    @Test
    void parseMetaColon() throws ImagingException {
        TiffField dateTime = mock(TiffField.class);
        when(dateTime.getStringValue()).thenReturn("2009:12:31 10:11:12");

        TiffImageMetadata exif = mock(TiffImageMetadata.class);
        when(exif.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL)).thenReturn(dateTime);

        JpegImageMetadata meta = new JpegImageMetadata(null, exif);

        assertEquals("2009/12/31/", extractor.parseMeta(meta));
    }

    @Test
    void parseMetaDashesAndColon() throws ImagingException {
        TiffField dateTime = mock(TiffField.class);
        when(dateTime.getStringValue()).thenReturn("2018-06-01 13:53:00");

        TiffImageMetadata exif = mock(TiffImageMetadata.class);
        when(exif.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL)).thenReturn(dateTime);

        JpegImageMetadata meta = new JpegImageMetadata(null, exif);

        assertEquals("2018/06/01/", extractor.parseMeta(meta));
    }

    @Test
    void parseMetaWrongFormat() throws ImagingException {
        TiffField dateTime = mock(TiffField.class);
        when(dateTime.getStringValue()).thenReturn("just a wrong fo:rm:at");

        TiffImageMetadata exif = mock(TiffImageMetadata.class);
        when(exif.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL)).thenReturn(dateTime);

        JpegImageMetadata meta = new JpegImageMetadata(null, exif);

        assertEquals(ExifPathExtractor.NO_EXIF_PATH, extractor.parseMeta(meta));
    }
}
