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

import com.adobe.internal.xmp.XMPMeta;
import com.adobe.internal.xmp.XMPMetaFactory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.xmp.XmpDirectory;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class ExifPathExtractorTest {
    private static final String XMP_NS = "http://ns.adobe.com/xap/1.0/";

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
    void parseMetadataNullMetadata() {
        assertThrows(NullPointerException.class, () -> extractor.parseMetadata(null));
    }

    @Test
    void parseMetadataNoExif() {
        assertEquals(ExifPathExtractor.NO_EXIF_PATH, extractor.parseMetadata(new Metadata()));
    }

    @Test
    void parseMetadataUsesDateTimeOriginalFirst() throws Exception {
        Metadata metadata = new Metadata();
        ExifSubIFDDirectory exif = new ExifSubIFDDirectory();
        exif.setString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, "2009:12:31 10:11:12");
        exif.setString(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED, "2011:02:03 10:11:12");
        metadata.addDirectory(exif);
        metadata.addDirectory(createXmpDirectory("2010-01-02T10:11:12"));

        assertEquals("2009/12/31/", extractor.parseMetadata(metadata));
    }

    @Test
    void parseMetadataFallsBackToCreateDate() throws Exception {
        Metadata metadata = new Metadata();
        ExifSubIFDDirectory exif = new ExifSubIFDDirectory();
        exif.setString(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED, "2011:02:03 10:11:12");
        metadata.addDirectory(exif);
        metadata.addDirectory(createXmpDirectory("2010-01-02T10:11:12"));

        assertEquals("2010/01/02/", extractor.parseMetadata(metadata));
    }

    @Test
    void parseMetadataFallsBackToDateTimeDigitized() {
        Metadata metadata = new Metadata();
        ExifSubIFDDirectory exif = new ExifSubIFDDirectory();
        exif.setString(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED, "2011:02:03 10:11:12");
        metadata.addDirectory(exif);

        assertEquals("2011/02/03/", extractor.parseMetadata(metadata));
    }

    @Test
    void parseMetadataSupportsDashesAndColon() {
        Metadata metadata = new Metadata();
        ExifSubIFDDirectory exif = new ExifSubIFDDirectory();
        exif.setString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, "2018-06-01 13:53:00");
        metadata.addDirectory(exif);

        assertEquals("2018/06/01/", extractor.parseMetadata(metadata));
    }

    @Test
    void parseMetadataWrongFormat() {
        Metadata metadata = new Metadata();
        ExifSubIFDDirectory exif = new ExifSubIFDDirectory();
        exif.setString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL, "just a wrong fo:rm:at");
        metadata.addDirectory(exif);

        assertEquals(ExifPathExtractor.NO_EXIF_PATH, extractor.parseMetadata(metadata));
    }

    private XmpDirectory createXmpDirectory(String createDate) throws Exception {
        XMPMeta xmpMeta = XMPMetaFactory.create();
        xmpMeta.setProperty(XMP_NS, "CreateDate", createDate);

        XmpDirectory xmpDirectory = new XmpDirectory();
        xmpDirectory.setXMPMeta(xmpMeta);
        return xmpDirectory;
    }
}
