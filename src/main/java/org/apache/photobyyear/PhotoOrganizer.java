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
import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.common.base.Preconditions.checkNotNull;

class PhotoOrganizer {
    private static final Logger LOG = LoggerFactory.getLogger(PhotoOrganizer.class);

    private final Path destination;
    private final ExifPathExtractor pathExtractor;

    PhotoOrganizer(@Nonnull Path destination, @Nonnull ExifPathExtractor pathExtractor) {
        this.destination = checkNotNull(destination);
        this.pathExtractor = checkNotNull(pathExtractor);
    }

    void copyPicture(@Nonnull Path picture) {
        checkNotNull(picture);

        Path targetDirectory = destination.resolve(pathExtractor.extractPath(picture.toFile()));
        Path target = targetDirectory.resolve(picture.getFileName());

        try {
            Files.createDirectories(targetDirectory);

            if (target.toFile().exists()) {
                System.out.printf("Skipped '%s' because destination already exists: '%s'%n", picture, target);
                return;
            }

            Files.copy(picture, target);
            System.out.printf("'%s' -> '%s' done.%n", picture, target);
        } catch (IOException e) {
            System.err.printf("Failed to copy '%s' to '%s'. %s%n", picture, target, e.getMessage());
            LOG.error("Failed to copy '{}' to '{}'. {}", picture, target, e.getMessage());
        }
    }
}
