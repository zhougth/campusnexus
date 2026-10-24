package com.campusnexus.service;

import com.campusnexus.dto.LoginDTO;
import com.campusnexus.dto.RegisterDTO;
import com.campusnexus.dto.UpdatePasswordDTO;
import com.campusnexus.dto.UpdateProfileDTO;
import com.campusnexus.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface AuthService {

    UserVO register(RegisterDTO dto);

    String login(LoginDTO dto);

    UserVO currentUser();

    void updateProfile(UpdateProfileDTO dto);

    void updatePassword(UpdatePasswordDTO dto);

    void logout(String token);

    String updateAvatar(MultipartFile file) throws IOException;
}
