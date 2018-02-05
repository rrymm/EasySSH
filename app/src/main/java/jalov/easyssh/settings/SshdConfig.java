package jalov.easyssh.settings;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import jalov.easyssh.server.SshServer;
import jalov.easyssh.utils.RootManager;
import jalov.easyssh.utils.Scripts;

/**
 * Created by jalov on 2018-01-16.
 */

@Singleton
public class SshdConfig {
    public static final String TAG = "SettingsManager";
    public static final String SSHD_CONFIG_PATH = "/data/ssh/sshd_config";
    public static final String RSA_HOSTKEY_PATH = "/data/ssh/ssh_host_rsa_key";
    public static final String DSA_HOSTKEY_PATH = "/data/ssh/ssh_host_dsa_key";
    public static final String AUTHORIZED_KEYS_PATH = "/data/ssh/authorized_keys";
    private Map<String,String> settings;
    private List<String> hostKeys;
    private SshServer server;

    public SshdConfig(SshServer server) {
        this.server = server;
        load();
    }

    public void load() {
        settings = new HashMap<>();
        hostKeys = new ArrayList<>();
        Optional<InputStream> inputStream = RootManager.su(Scripts.BEGIN +
                Scripts.CREATE_SSHD_CONFIG_IF_NOT_EXIST +
                Scripts.READ_FILE + SSHD_CONFIG_PATH +
                Scripts.END);
        if(inputStream.isPresent()) {
            // Load config from file
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream.get()))) {
                settings = reader.lines().filter(l -> !l.isEmpty())
                        .filter(l -> l.charAt(0) != '#') // Ignore comments
                        .map(l -> l.split("\\s+"))
                        .filter(a -> a.length >= 2)
                        .filter(a -> {
                            if(a[0].compareTo("HostKey") != 0) return true;
                            hostKeys.add(a[1]);
                            return false;
                        }) // Store HostKeys separately
                        .collect(Collectors.toMap(a -> a[0], a -> Arrays.stream(a).skip(1L).collect(Collectors.joining(" "))));

                settings.entrySet().forEach(es -> Log.d(TAG, "load: " + es.getKey() + "=" + es.getValue()));
                addMissingConfiguration(settings);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Sshd config loading Error");
        }
    }

    public String get(String key) {
        return settings.get(key);
    }

    public void addOrUpdate(String key, String value) {
        if(settings.containsKey(key)) {
            if (settings.get(key).compareTo(value) != 0) {
                settings.replace(key, value);
                save();
            }
        } else {
            settings.put(key, value);
            save();
        }
    }

    public void remove(String key) {
        if(settings.containsKey(key)) {
            settings.remove(key);
            save();
        }
    }

    private void save() {
        StringBuilder fileContent = new StringBuilder();
        hostKeys.forEach(hk -> fileContent.append("HostKey " + hk + "\n"));
        settings.entrySet().forEach(es -> fileContent.append(es.getKey() + " " + es.getValue() + "\n"));
        RootManager.su("echo '" + fileContent.toString() + "' > " + SSHD_CONFIG_PATH);

        if(server.isRunning()) {
            server.restart();
        }
    }

    private void addMissingConfiguration(Map<String,String> config) {
        Map<String,String> defaultConfig = DefaultSshdConfig.INSTANCE.getConfig();
        defaultConfig.entrySet().forEach( es -> config.putIfAbsent(es.getKey(), es.getValue()));
        if(hostKeys.isEmpty()) {
            hostKeys.add(RSA_HOSTKEY_PATH);
            hostKeys.add(DSA_HOSTKEY_PATH);
        }
    }
}
