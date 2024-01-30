package nl.moj.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class WorkerService {

    private final String identification;

    public WorkerService() {
        identification = resolveHostname();
    }

    private String resolveHostname() {
        String hostname = "";
        try {
            hostname = new BufferedReader(
                    new InputStreamReader(Runtime.getRuntime().exec("hostname").getInputStream()))
                    .readLine() + "-" + UUID.randomUUID();
        } catch (IOException e) {
            try {
                return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID();
            } catch (UnknownHostException ex) {
                hostname = UUID.randomUUID().toString();
            }
        }
        return hostname;
    }

    public String getWorkerIdentification() {
        return identification;
    }
}
