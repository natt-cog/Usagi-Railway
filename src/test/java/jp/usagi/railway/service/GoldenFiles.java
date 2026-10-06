package jp.usagi.railway.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;

final class GoldenFiles {

    private GoldenFiles() {
    }

    static List<String> read(String name) throws IOException {
        InputStream in = GoldenFiles.class.getResourceAsStream("/golden/" + name);
        try {
            return IOUtils.readLines(in, "UTF-8");
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
