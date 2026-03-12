package com.synapxnet.dataopsusrservice.service;

import java.util.List;
import java.util.Map;

public interface AuthService {

    Map<String, Object> sendSmsCode(String userPhone);

    Map<String, Object> login(String userPhone, String code);

    void logout(String token);

    Map<String, Object> getUserInfo(String token);

    List<String> getAccessCodes(String token);
}
