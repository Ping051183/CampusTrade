package com.campustrade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.constant.RoleConstant;
import com.campustrade.constant.StatusConstant;
import com.campustrade.dto.LoginDTO;
import com.campustrade.dto.RegisterDTO;
import com.campustrade.dto.UpdatePasswordDTO;
import com.campustrade.dto.UserUpdateDTO;
import com.campustrade.entity.User;
import com.campustrade.exception.BusinessException;
import com.campustrade.exception.ErrorCode;
import com.campustrade.mapper.UserMapper;
import com.campustrade.security.JwtTokenProvider;
import com.campustrade.service.UserService;
import com.campustrade.vo.LoginVO;
import com.campustrade.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Year;
import java.util.regex.Pattern;

/**
 * 用户Service实现类
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /** 支持的学校（校园二手平台仅限本校学生使用） */
    private static final String SUPPORTED_SCHOOL = "广州工商学院";

    /**
     * 学号规则：12 位数字 = 4 位入学年份 + 6 位专业和班级 + 2 位班内学号
     * 例如 202310610320：2023 年入学，106103 为专业班级，20 为班内序号
     */
    private static final Pattern STUDENT_NO_PATTERN = Pattern.compile("^20\\d{10}$");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO registerDTO) {
        // 校验校园身份：仅限广州工商学院学生，学号需符合学校编码规则
        validateCampusIdentity(registerDTO.getStudentNo(), registerDTO.getSchool());

        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, registerDTO.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        // 检查学号是否已被注册
        LambdaQueryWrapper<User> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(User::getStudentNo, registerDTO.getStudentNo().trim());
        if (userMapper.selectCount(studentWrapper) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该学号已被注册");
        }

        // 创建新用户
        User user = new User();
        BeanUtils.copyProperties(registerDTO, user);
        user.setStudentNo(registerDTO.getStudentNo().trim());
        user.setSchool(SUPPORTED_SCHOOL);
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRole(RoleConstant.USER);
        user.setStatus(StatusConstant.USER_ENABLED);

        userMapper.insert(user);
    }

    /**
     * 校验学号 + 学校：学校必须是广州工商学院，学号必须为 12 位且入学年份合理
     */
    private void validateCampusIdentity(String studentNo, String school) {
        if (!StringUtils.hasText(school) || !SUPPORTED_SCHOOL.equals(school.trim())) {
            throw new BusinessException(ErrorCode.SCHOOL_NOT_SUPPORTED,
                    "暂仅支持广州工商学院学生注册，学校请填写：广州工商学院");
        }
        if (!StringUtils.hasText(studentNo) || !STUDENT_NO_PATTERN.matcher(studentNo.trim()).matches()) {
            throw new BusinessException(ErrorCode.STUDENT_NO_INVALID,
                    "学号格式不正确：应为12位数字（4位入学年份+6位专业班级+2位班内序号），如 202310610320");
        }
        int admissionYear = Integer.parseInt(studentNo.trim().substring(0, 4));
        int currentYear = Year.now().getValue();
        if (admissionYear < 2010 || admissionYear > currentYear + 1) {
            throw new BusinessException(ErrorCode.STUDENT_NO_INVALID,
                    "学号中的入学年份不合理：" + admissionYear);
        }
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 根据用户名查询用户
        User user = userMapper.findByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 检查用户状态
        if (StatusConstant.USER_DISABLED.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // 验证密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }

        // 生成JWT Token
        String token = jwtTokenProvider.generateToken(user.getId(), user.getRole());

        // 构建返回结果
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setNickname(user.getNickname());
        loginVO.setAvatar(user.getAvatar());
        loginVO.setRole(user.getRole());

        return loginVO;
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(Long userId, UserUpdateDTO updateDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 如果修改了学号或学校，需要重新校验校园身份
        if (StringUtils.hasText(updateDTO.getStudentNo()) || StringUtils.hasText(updateDTO.getSchool())) {
            String studentNo = StringUtils.hasText(updateDTO.getStudentNo())
                    ? updateDTO.getStudentNo().trim() : user.getStudentNo();
            String school = StringUtils.hasText(updateDTO.getSchool())
                    ? updateDTO.getSchool().trim() : user.getSchool();
            validateCampusIdentity(studentNo, school);
        }

        // 更新用户信息
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setNickname(updateDTO.getNickname());
        updateUser.setPhone(updateDTO.getPhone());
        updateUser.setEmail(updateDTO.getEmail());
        updateUser.setStudentNo(StringUtils.hasText(updateDTO.getStudentNo()) ? updateDTO.getStudentNo().trim() : null);
        updateUser.setSchool(StringUtils.hasText(updateDTO.getSchool()) ? SUPPORTED_SCHOOL : null);

        userMapper.updateById(updateUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, UpdatePasswordDTO updatePasswordDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 验证原密码
        if (!passwordEncoder.matches(updatePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_ERROR);
        }

        // 更新密码
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPassword(passwordEncoder.encode(updatePasswordDTO.getNewPassword()));
        userMapper.updateById(updateUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(Long userId, String avatarUrl) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setAvatar(avatarUrl);
        userMapper.updateById(updateUser);

        return avatarUrl;
    }
}
