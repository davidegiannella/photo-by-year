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

import com.adobe.internal.xmp.XMPException;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.xmp.XmpDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

class ExifPathExtractor {
    private static final Logger LOG = LoggerFactory.getLogger(ExifPathExtractor.class);
    private static final String XMP_NS = "http://ns.adobe.com/xap/1.0/";

    static final String NO_EXIF_PATH = "NoExif/";
    static final List<DateTimeFormatter> DATE_FORMATTERS = Collections.unmodifiableList(
        Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        )
    );

    /**
     * Extract the relative path from the EXIF metadata of the provided image.
     *
     * @param image the image to parse. Cannot be null
     * @return the path, or {@link #NO_EXIF_PATH} in case of missing or unreadable metadata
     */
    @Nonnull
    String extractPath(@Nonnull File image) {
        checkNotNull(image);

        try {
            String path = parseMetadata(ImageMetadataReader.readMetadata(image));
            if (NO_EXIF_PATH.equals(path)) {
                LOG.error("Problems parsing Exif and formats. '{}' has been copied to '{}'. " +
                        "Check the logs for more details", image.getAbsolutePath(), NO_EXIF_PATH);
            }
            return path;
        } catch (ImageProcessingException e) {
            LOG.error("Error reading metadata on '{}'. {}", image.getAbsolutePath(), e.getMessage());
        } catch (IOException e) {
            LOG.error("Error reading metadata on '{}'. {}", image.getAbsolutePath(), e.getMessage());
        }

        return NO_EXIF_PATH;
    }

    String parseMetadata(@Nonnull Metadata metadata) {
        checkNotNull(metadata);

        return findOriginalDate(metadata)
            .or(() -> findCreateDate(metadata))
            .or(() -> findDigitizedDate(metadata))
            .map(ExifPathExtractor::formatPath)
            .orElse(NO_EXIF_PATH);
    }

    private Optional<LocalDateTime> findOriginalDate(Metadata metadata) {
        ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (exif == null) {
            return Optional.empty();
        }

        return parseExifDate(exif, ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
    }

    private Optional<LocalDateTime> findCreateDate(Metadata metadata) {
        XmpDirectory xmp = metadata.getFirstDirectoryOfType(XmpDirectory.class);
        if (xmp == null || xmp.getXMPMeta() == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(xmp.getXMPMeta().getPropertyCalendar(XMP_NS, "CreateDate"))
                .map(calendar -> LocalDateTime.ofInstant(calendar.toInstant(), calendar.getTimeZone().toZoneId()));
        } catch (XMPException e) {
            LOG.warn("Error parsing XMP CreateDate metadata. {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<LocalDateTime> findDigitizedDate(Metadata metadata) {
        ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (exif == null) {
            return Optional.empty();
        }

        return parseExifDate(exif, ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED);
    }

    private Optional<LocalDateTime> parseExifDate(ExifSubIFDDirectory exif, int tagType) {
        return parseDate(exif.getString(tagType))
            .or(() -> Optional.ofNullable(exif.getDate(tagType))
                .map(ExifPathExtractor::toLocalDateTime));
    }

    private static Optional<LocalDateTime> parseDate(String value) {
        if (value == null) {
            return Optional.empty();
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return Optional.of(LocalDateTime.parse(value, formatter));
            } catch (DateTimeParseException e) {
                LOG.warn("Error parsing meta through available formatters. Original value: '{}'", value);
            }
        }
        return Optional.empty();
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());
    }

    private static String formatPath(LocalDateTime date) {
        return String.format("%s/%02d/%02d/", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }
}
