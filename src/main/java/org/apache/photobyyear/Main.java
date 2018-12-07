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

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final OptionParser parser = new OptionParser();
    private static final OptionSpec<File> optionSource = parser
        .accepts("s", "Directory where the pictures are").withRequiredArg().ofType(File.class)
        .required();
    private static final OptionSpec<File> optionDestination = parser
        .accepts("d", "Directory where to put the parsed pictures").withRequiredArg()
        .ofType(File.class).required();

    private final File source;
    private final File destination;

    static final String NO_EXIF_PATH = "NoExif/";

    public static void main(String[] args) throws IOException {
        OptionSet options = null;

        try {
            options = parser.parse(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            parser.printHelpOn(System.out);
        }

        if (options != null) {
            new Main(options.valueOf(optionSource), options.valueOf(optionDestination)).run();
        }
    }

    Main(@Nonnull File source, @Nonnull File destination) {
        this.source = checkNotNull(source);
        this.destination = checkNotNull(destination);
    }

    public void run() {
        System.out.printf("Copying from '%s' to '%s'%n", source.getAbsolutePath(),
            destination.getAbsolutePath());
        if (!ensureDirectories(source, destination)) {
            throw new IllegalArgumentException(
                "Either source and/or destination are not valid directories or don't exist");
        }

        try {
            Path destPath = destination.toPath();
            Files.newDirectoryStream(Paths.get(source.toURI()), entry -> {
                return Files.isRegularFile(entry) && entry.getFileName().toString().toLowerCase()
                    .endsWith(".jpg");
            }).forEach(pic -> {

                try {
                    // resolve and create, if not there, the directory stucture
                    Path d = destPath.resolve(extractPath(pic.toFile()));
                    Files.createDirectories(d);

                    // resolving the final destinaton and name for the copy
                    d = d.resolve(d.toFile().getAbsolutePath() + "/" + pic.getFileName());

                    if (d.toFile().exists()) {
                        System.err.printf(
                            "Error. File '%s' already exists on destination ('%s'). Skipping%n",
                            pic, d);
                        return;
                    }

                    Files.copy(pic, d);
                    System.out.printf("'%s' -> '%s' done.%n", pic, d);
                } catch (IOException e) {
                    System.err
                        .printf("Error copying '%s' to destination. %s%n", pic, e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ensure that both the provided directories {@code source} and {@code destination} exists and
     * are directories and they're not the same
     *
     * @param source      the source directory
     * @param destination the destination directory
     * @return the status of the provied directories
     */
    static boolean ensureDirectories(@Nonnull File source, @Nonnull File destination) {
        checkNotNull(source);
        checkNotNull(destination);

        return source.exists() && source.isDirectory() && destination.exists() && destination
            .isDirectory() && !source.getAbsolutePath().equals(destination.getAbsolutePath());
    }

    /**
     * extract the relative path off the exif of thep provided image
     *
     * @param image the image to parse. Cannot be null
     * @return the path or null in case of errors
     */
    @CheckForNull
    static String extractPath(@Nonnull File image) {
        checkNotNull(image);

        try {
            ImageMetadata meta = Imaging.getMetadata(image);
            if (meta == null) {
                // no exif there
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
        } catch (ImageReadException e) {
            LOG.error("Error reading metadata on '{}'", image.getAbsolutePath() ,e);
        } catch (IOException e) {
            LOG.error("Error reading metadata on '{}'", image.getAbsolutePath() ,e);
        } catch (DateTimeParseException e) {
            LOG.error("Error parsing Date/Time metadata on '{}'", image.getAbsolutePath(), e);
        }

        return null;
    }

    static final List<DateTimeFormatter> DATE_FORMATTERS = Collections.unmodifiableList(
            Arrays.asList(
                    DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            )
    );

    static String parseMeta(JpegImageMetadata meta) throws ImageReadException {
        checkNotNull(meta);
        JpegImageMetadata jpegMeta = (JpegImageMetadata) meta;
        TiffField dateTimeOriginal = jpegMeta.findEXIFValue(EXIF_TAG_DATE_TIME_ORIGINAL);
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
