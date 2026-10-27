package com.recruiting.platform;

import com.recruiting.platform.contracts.FileStore;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * {@link FileStore} on a local volume ({@code app.files.store=local}, the default; root
 * {@code app.files.local-root}). The volume is encrypted at rest by the host (docs/hosting.md). Keys are validated
 * ({@code [a-z0-9/-]}, no dot segments) so a key can never leave the root.
 */
@Component
@ConditionalOnProperty(name = "app.files.store", havingValue = "local", matchIfMissing = true)
class LocalFileStore implements FileStore {

    private static final Pattern KEY = Pattern.compile("[a-z0-9/-]{1,300}");

    private final Path root;

    LocalFileStore(@Value("${app.files.local-root:${java.io.tmpdir}/recruiting-files}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public void put(String key, InputStream content, long size, String contentType) throws IOException {
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Path tmp = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
        try {
            Files.copy(content, tmp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Override
    public Optional<InputStream> get(String key) throws IOException {
        Path target = resolve(key);
        return Files.isRegularFile(target) ? Optional.of(Files.newInputStream(target)) : Optional.empty();
    }

    @Override
    public void delete(String key) throws IOException {
        Files.deleteIfExists(resolve(key));
    }

    private Path resolve(String key) {
        if (key == null || !KEY.matcher(key).matches() || key.startsWith("/") || key.contains("//")
                || key.contains("..")) {
            throw new IllegalArgumentException("invalid storage key");
        }
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("invalid storage key");
        }
        return p;
    }
}
