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

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

class SourceScanner {
    static final List<String> SUPPORTED_EXTENSIONS = Collections.unmodifiableList(
        Arrays.asList("jpg", "jpeg", "heic", "heif")
    );

    List<Path> scan(@Nonnull Path source) throws IOException {
        checkNotNull(source);

        List<Path> pictures = new ArrayList<>();
        try (var directoryStream = Files.newDirectoryStream(source, this::isSupportedImageFile)) {
            directoryStream.forEach(pictures::add);
        }
        return pictures;
    }

    private boolean isSupportedImageFile(Path entry) {
        if (!Files.isRegularFile(entry)) {
            return false;
        }

        String fileName = entry.getFileName().toString().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(extension -> fileName.endsWith("." + extension));
    }
}
