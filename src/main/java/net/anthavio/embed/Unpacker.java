/**
 * Copyright © 2014, Anthavio
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those
 * of the authors and should not be interpreted as representing official policies,
 * either expressed or implied, of the FreeBSD Project.
 */
package net.anthavio.embed;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import net.anthavio.process.Arch;
import net.anthavio.process.Bit;
import net.anthavio.process.Os;
import net.anthavio.process.Platform;
import net.anthavio.process.Platform.DetectedPlatform;

/**
 * @author mvanek
 */
public class Unpacker {

    public static UnpackerBuilder Builder() {
        return new UnpackerBuilder();
    }

    private final Collection<Binary> binaries;
    private final String name;
    private final String version;
    private final String destination;

    public Unpacker(String name, String version, String destination, Collection<Binary> binaries) {
        this.name = name;
        this.version = version;
        this.destination = destination;
        this.binaries = binaries;
    }

    public File unpack() {
        Binary binary = detect();
        if (binary == null) {
            throw new IllegalArgumentException("Binary not found for platform: " + Platform.detect());
        }

        StringBuilder subdir = new StringBuilder();
        if (name != null) {
            subdir.append(name);
        } else {
            subdir.append("unpacker");
        }
        if (version != null) {
            subdir.append('-');
            subdir.append(version);
        }

        File directory = new File(destination, subdir.toString());

        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IllegalArgumentException("Failed to make target directory: " + directory);
            }
        }

        String resource = binary.getResource();
        String filename = resource.substring(resource.lastIndexOf('/') + 1);
        File target = new File(directory, filename);

        unpack(resource, target);

        if (!target.canExecute()) {
            target.setExecutable(true); //check return value...
        }
        return target;
    }

    protected Binary detect() {
        DetectedPlatform platform = Platform.detect();
        for (Binary binary : binaries) {
            boolean arch = binary.getArch() == platform.getArch() || binary.getArch() == Arch.UNSET;
            boolean os = binary.getOs() == platform.getOs() || binary.getOs() == Os.UNSET;
            boolean bits = binary.getOsBits() == platform.getOsBits() || platform.getOsBits() == Bit.B64 || binary.getOsBits() == Bit.UNSET;
            if (arch && os && bits) {
                return binary;
            }
        }
        return null;
    }

    private static void unpack(String resource, File target) {
        if (target.exists() && target.isFile() && target.canExecute()) {
            return; //keep existing
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = Unpacker.class.getClassLoader();
        }
        InputStream stream = classLoader.getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalStateException("Resource not found " + resource + " using ClassLoader " + classLoader);
        }
        BufferedInputStream input = new BufferedInputStream(stream);
        BufferedOutputStream output = null;
        try {
            output = new BufferedOutputStream(new FileOutputStream(target));
            while (input.available() > 0) {
                byte[] buffer = new byte[input.available()];
                input.read(buffer);
                output.write(buffer);
            }
            output.flush();

        } catch (Exception x) {
            throw new IllegalStateException("Failed to unpack resource: " + resource + " into: " + target, x);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException iox) {
                    //ignore
                }
            }
            try {
                input.close();
            } catch (IOException iox) {
                //ignore
            }
        }
    }

    public static class UnpackerBuilder {

        private Collection<Binary> binaries = new HashSet<Binary>();
        private String name;
        private String version;
        private String destination = System.getProperty("unpacker.dir", System.getProperty("java.io.tmpdir"));

        public UnpackerBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public UnpackerBuilder setVersion(String version) {
            this.version = version;
            return this;
        }

        public UnpackerBuilder addBinary(Os os, Bit bits, String resource) {
            binaries.add(new Binary(os, bits, resource));
            return this;
        }

        public UnpackerBuilder setDestination(String destination) {
            this.destination = destination;
            return this;
        }

        public Unpacker build() {
            return new Unpacker(name, version, destination, binaries);
        }
    }
}
