package com.synapxnet.dataopsusrservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.*;

@Service
public class SMSCodeService {

    private static String SMSUrl;

    @Value("${Spug.url}")
    public void setSMSUrl(String url) {
        SMSCodeService.SMSUrl = url;
    }

    public static String SMSCodeSend(String[] args) throws Exception {
        String json = String.format(
                "{\"AppName\":\"%s\",\"SMSCode\":\"%s\",\"EffectiveTime\":\"%s\",\"targets\":\"%s\"}",
                args[0],
                args[1],
                args[2],
                args[3]
        );

        HttpURLConnection conn = (HttpURLConnection) new URL(SMSUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            return line;
        }
        return line;
    }
}
