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
import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

class DirectoryValidator {
    private DirectoryValidator() {
    }

    /**
     * Ensure that both provided directories {@code source} and {@code destination} exist, are
     * directories, and are not the same path.
     *
     * @param source      the source directory
     * @param destination the destination directory
     * @return the status of the provided directories
     */
    static boolean ensureDirectories(@Nonnull File source, @Nonnull File destination) {
        checkNotNull(source);
        checkNotNull(destination);

        return source.exists() && source.isDirectory() && destination.exists() && destination
            .isDirectory() && !source.getAbsolutePath().equals(destination.getAbsolutePath());
    }
}
