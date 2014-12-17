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
package net.anthavio.rembedis;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * 
 * @author martin.vanek
 *
 */
public class RembedisTest {

    @Test
    public void testProcess() throws Exception {

        File binary = Rembedis.unpack();
        ProcessBuilder builder = new ProcessBuilder(binary.getAbsolutePath(), "--port", Integer.toString(9999));
        builder.directory(binary.getParentFile());
        Process process = builder.start();
        GuardedProcess consumer = new GuardedProcess(process, "The server is now ready to accept connections");
        consumer.start(3000);

        /*
        InputStream errorStream = process.getErrorStream();
        System.out.println("e>");
        String e = capture(errorStream);
        System.out.println("e<" + e);
        
        InputStream inputStream = process.getInputStream();
        System.out.println("i>");
        String i = capture(inputStream);
        System.out.println("i<" + i);
        */
        //process.destroy();
    }

    @Test
    public void testJavaIoTmpDirectory() throws IOException, InterruptedException {
        File binary = Rembedis.unpack();

        String javaIoTmpdir = System.getProperty("java.io.tmpdir");
        File expectedDir = new File(javaIoTmpdir, "redis-" + Rembedis.REDIS_VERSION);
        Assertions.assertThat(binary.getParentFile()).isEqualTo(expectedDir);
        assertProcessExecution(binary);

        long lastModified = binary.lastModified();

        File binary2 = Rembedis.unpack(); //existing file is returned
        Assertions.assertThat(binary2.lastModified()).isEqualTo(lastModified); // SAME
        assertProcessExecution(binary2);

        binary.delete(); //purge cached
        Assertions.assertThat(binary.exists()).isFalse();
        Thread.sleep(1000); //1 second at least!

        File binary3 = Rembedis.unpack(); //new file must be unpacked
        Assertions.assertThat(binary3.lastModified()).isNotEqualTo(lastModified); //DIFF
        assertProcessExecution(binary3);
    }

    @Test
    public void testLocalTargetDirectory() throws IOException, InterruptedException {
        String targetDir = "target/redis-test/unpack";
        String binaryPath = Rembedis.unpack(targetDir);
        File binary = new File(binaryPath);

        String javaUserDir = System.getProperty("user.dir");
        Assertions.assertThat(binary.getParentFile()).isEqualTo(new File(javaUserDir, targetDir));
        assertProcessExecution(binary);

        long lastModified = binary.lastModified();

        String binaryPath2 = Rembedis.unpack(targetDir); //existing file is returned
        File binary2 = new File(binaryPath2);
        Assertions.assertThat(binary2.lastModified()).isEqualTo(lastModified); // SAME
        assertProcessExecution(binary2);

        binary.delete(); //purge cached
        Assertions.assertThat(binary.exists()).isFalse();
        Thread.sleep(1000); //1 second at least!

        String binaryPath3 = Rembedis.unpack(targetDir); //new file must be unpacked
        File binary3 = new File(binaryPath3);
        Assertions.assertThat(binary3.lastModified()).isNotEqualTo(lastModified); //DIFF
        assertProcessExecution(binary3);
    }

    private void assertProcessExecution(File binary) throws IOException {
        Assertions.assertThat(binary).exists();
        Assertions.assertThat(binary).isFile();
        Assertions.assertThat(binary.canExecute()).isTrue();

        Process process = new ProcessBuilder(binary.getAbsolutePath(), "--version").start();
        String sysout = capture(process.getInputStream());
        String syserr = capture(process.getErrorStream());
        try {
            process.waitFor();
        } catch (InterruptedException ix) {
            System.out.println("Interrupted process.waitFor()");
        }
        Assertions.assertThat(process.exitValue()).isEqualTo(0);
        Assertions.assertThat(syserr).isEmpty();
        Assertions.assertThat(sysout).startsWith("Redis server v=" + Rembedis.REDIS_VERSION);
    }

    private String capture(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        String line = null;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }
}
