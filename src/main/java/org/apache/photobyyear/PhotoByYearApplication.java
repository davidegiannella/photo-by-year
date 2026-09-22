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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;

import static com.google.common.base.Preconditions.checkNotNull;

class PhotoByYearApplication {
    private static final Logger LOG = LoggerFactory.getLogger(PhotoByYearApplication.class);

    private final Path source;
    private final Path destination;
    private final SourceScanner sourceScanner;
    private final PhotoOrganizer photoOrganizer;

    PhotoByYearApplication(@Nonnull Path source, @Nonnull Path destination) {
        this(source, destination, PhotoByYearOptions.defaults());
    }

    PhotoByYearApplication(
        @Nonnull Path source,
        @Nonnull Path destination,
        @Nonnull PhotoByYearOptions options
    ) {
        this(
            source,
            destination,
            new SourceScanner(),
            new PhotoOrganizer(destination, new ExifPathExtractor(), options)
        );
    }

    PhotoByYearApplication(
        @Nonnull Path source,
        @Nonnull Path destination,
        @Nonnull SourceScanner sourceScanner,
        @Nonnull PhotoOrganizer photoOrganizer
    ) {
        this.source = checkNotNull(source);
        this.destination = checkNotNull(destination);
        this.sourceScanner = checkNotNull(sourceScanner);
        this.photoOrganizer = checkNotNull(photoOrganizer);
    }

    public void run() {
        System.out.printf("Copying from '%s' to '%s'%n", source.toAbsolutePath(), destination.toAbsolutePath());
        if (!DirectoryValidator.ensureDirectories(source.toFile(), destination.toFile())) {
            throw new IllegalArgumentException(
                "Either source and/or destination are not valid directories or don't exist");
        }

        try {
            sourceScanner.scan(source).forEach(photoOrganizer::copyPicture);
        } catch (IOException e) {
            System.err.printf("Failed to scan source directory '%s'. %s%n", source.toFile(), e.getMessage());
            LOG.error("Failed to scan source directory '{}'. {}", source.toFile(), e.getMessage());
        }
    }
}
