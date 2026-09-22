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

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL;

class ExifPathExtractor {
    private static final Logger LOG = LoggerFactory.getLogger(ExifPathExtractor.class);

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
            ImageMetadata meta = Imaging.getMetadata(image);
            if (meta == null) {
                return NO_EXIF_PATH;
            }

            if (meta instanceof JpegImageMetadata) {
                String path = parseMeta((JpegImageMetadata) meta);
                if (NO_EXIF_PATH.equals(path)) {
                    LOG.error("Problems parsing Exif and formats. '{}' has been copied to '{}'. " +
                            "Check the logs for more details", image.getAbsolutePath(), NO_EXIF_PATH);
                }
                return path;
            } else {
                LOG.error("Not a valid metadata class. Expected '{}' but was '{}'",
                    JpegImageMetadata.class.getName(), meta.getClass().getName());
            }
        } catch (ImagingException e) {
            LOG.error("Error reading metadata on '{}'. {}", image.getAbsolutePath(), e.getMessage());
        } catch (IOException e) {
            LOG.error("Error reading metadata on '{}'. {}", image.getAbsolutePath(), e.getMessage());
        } catch (DateTimeParseException e) {
            LOG.error("Error parsing Date/Time metadata on '{}'. {}", image.getAbsolutePath(), e.getMessage());
        }

        return NO_EXIF_PATH;
    }

    String parseMeta(@Nonnull JpegImageMetadata meta) throws ImagingException {
        checkNotNull(meta);
        TiffField dateTimeOriginal = meta.findExifValue(EXIF_TAG_DATE_TIME_ORIGINAL);
        if (dateTimeOriginal == null) {
            return NO_EXIF_PATH;
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDateTime date = LocalDateTime.parse(dateTimeOriginal.getStringValue(), formatter);
                return String.format("%s/%02d/%02d/", date.getYear(), date.getMonthValue(),
                    date.getDayOfMonth());
            } catch (DateTimeParseException e) {
                LOG.warn("Error parsing meta through available formatters. Original value: '{}'", dateTimeOriginal.getStringValue());
            }
        }
        return NO_EXIF_PATH;
    }
}
