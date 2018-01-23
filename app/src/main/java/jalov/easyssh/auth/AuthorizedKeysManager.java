package jalov.easyssh.auth;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import jalov.easyssh.RootManager;
import jalov.easyssh.settings.SshdConfig;

/**
 * Created by jalov on 2018-01-17.
 */

@Singleton
public class AuthorizedKeysManager {
    private final String TAG = this.getClass().getName();
    private SshdConfig sshdConfig;
    private List<AuthorizedKey> keys;

    public AuthorizedKeysManager(SshdConfig sshdConfig) {
        this.sshdConfig = sshdConfig;
        keys = loadAuthorizedKeys();
    }

    private List<AuthorizedKey> loadAuthorizedKeys() {
        String path = sshdConfig.get("AuthorizedKeysFile");
        File file = new File(path);
        Optional<InputStream> inputStream = RootManager.getFileInputStream(file);
        if (inputStream.isPresent()) {
            return readAuthorizedKeysFromInputStream(inputStream.get());
        }

        return null;
    }

    private void saveAuthorizedKeys() {
        String fileContent;
        fileContent = keys.stream().map(AuthorizedKey::toString).collect(Collectors.joining("\n"));
        RootManager.saveFile(sshdConfig.get("AuthorizedKeysFile"), fileContent);
    }

    public List<AuthorizedKey> getKeys() {
        return keys;
    }

    public void addAuthorizedKeysFromInputStream(InputStream inputStream) {
        keys.addAll(readAuthorizedKeysFromInputStream(inputStream));
        saveAuthorizedKeys();
    }

    public void removeAuthorizedKey(int position) {
        keys.remove(position);
        saveAuthorizedKeys();
    }

    private List<AuthorizedKey> readAuthorizedKeysFromInputStream(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().map(l -> l.split("\\s+"))
                    .filter(a -> a.length >= 3)
                    .map(a -> new AuthorizedKey(KeyType.getKey(a[0]), a[1], a[2]))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }
}