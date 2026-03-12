package com.synapxnet.dataopsusrservice.service;

import com.synapxnet.dataopsusrservice.entity.User;
import java.util.List;

public interface UserService {

    List<User> listAll();

    User getById(Long id);

    User create(User user);

    User update(User user);

    void delete(Long id);

    void changePassword(Long id, String oldPassword, String newPassword);
}
