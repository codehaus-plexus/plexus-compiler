package org.codehaus.plexus.compiler.csharp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarUtil {
    public static void extract(Path destDir, File jarFile) throws IOException {
        Path toPath = destDir.normalize();
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> enumEntries = jar.entries();
            while (enumEntries.hasMoreElements()) {
                JarEntry file = enumEntries.nextElement();
                Path f = destDir.resolve(file.getName());
                if (!f.startsWith(toPath)) {
                    throw new IOException("Bad zip entry");
                }
                if (file.isDirectory()) {
                    Files.createDirectories(f);
                    continue;
                }
                try (InputStream is = jar.getInputStream(file);
                     OutputStream fos = Files.newOutputStream(f)) {
                    while (is.available() > 0) {
                        fos.write(is.read());
                    }
                }
            }
        }
    }
}
