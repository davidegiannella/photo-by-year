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

import java.io.File;
import java.io.IOException;

public class Main {
    private static final OptionParser parser = new OptionParser();
    private static final OptionSpec<File> optionSource = parser
        .accepts("s", "Directory where the pictures are").withRequiredArg().ofType(File.class)
        .required();
    private static final OptionSpec<File> optionDestination = parser
        .accepts("d", "Directory where to put the parsed pictures").withRequiredArg()
        .ofType(File.class).required();
    private static final String optionDryRun = "dry-run";

    static {
        parser.accepts(optionDryRun, "Print planned actions without copying files");
    }

    public static void main(String[] args) throws IOException {
        OptionSet options = null;

        try {
            options = parser.parse(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            parser.printHelpOn(System.out);
        }

        if (options != null) {
            new PhotoByYearApplication(
                options.valueOf(optionSource).toPath(),
                options.valueOf(optionDestination).toPath(),
                new PhotoByYearOptions(options.has(optionDryRun))
            ).run();
        }
    }
}
