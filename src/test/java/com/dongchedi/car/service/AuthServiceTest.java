package com.dongchedi.car.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.dongchedi.car.config.AuthProperties;
import com.dongchedi.car.dto.RegisterRequest;
import com.dongchedi.car.exception.BizException;
import com.dongchedi.car.model.UserAccount;
import com.dongchedi.car.repository.InMemoryUserRepository;
import com.dongchedi.car.repository.UserRepository;
import com.dongchedi.car.support.SessionKeys;

/**
 * 认证服务单元测试：密码哈希、注册校验、登录与失败锁定。
 */
class AuthServiceTest {

    private UserRepository userRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        userRepository = new InMemoryUserRepository();
        AuthProperties properties = new AuthProperties();
        properties.setMaxFailedAttempts(3);
        authService = new AuthService(userRepository, encoder, properties);
    }

    private RegisterRequest registerRequest(String username, String password, String nickname) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setNickname(nickname);
        return request;
    }

    @Test
    @DisplayName("注册成功后密码以 BCrypt 哈希保存，且注册即登录")
    void shouldRegisterWithHashedPassword() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        UserAccount user = authService.register(request, registerRequest("tom_2026", "tom12345", "汤姆"));

        assertThat(user.username()).isEqualTo("tom_2026");
        assertThat(user.nickname()).isEqualTo("汤姆");
        assertThat(user.passwordHash()).isNotEqualTo("tom12345");
        assertThat(user.passwordHash()).startsWith("$2");
        assertThat(user.isAdmin()).isFalse();

        MockHttpSession session = (MockHttpSession) request.getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute(SessionKeys.LOGIN_USER_ID)).isEqualTo(user.id());
    }

    @Test
    @DisplayName("用户名重复时返回 409")
    void shouldRejectDuplicateUsername() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        authService.register(request, registerRequest("tom_2026", "tom12345", null));

        assertThatThrownBy(() -> authService.register(new MockHttpServletRequest(),
                registerRequest("TOM_2026", "tom12345", null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("用户名已被注册")
                .extracting(error -> ((BizException) error).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("用户名格式不合法时返回 400")
    void shouldRejectInvalidUsername() {
        assertThatThrownBy(() -> authService.register(new MockHttpServletRequest(),
                registerRequest("ab", "tom12345", null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("用户名需为 4~20 位");
    }

    @Test
    @DisplayName("密码强度不足时返回 400")
    void shouldRejectWeakPassword() {
        assertThatThrownBy(() -> authService.register(new MockHttpServletRequest(),
                registerRequest("tom_2026", "abcdefgh", null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("需同时包含字母和数字");
    }

    @Test
    @DisplayName("登录成功后写入 Session，且用户名大小写不敏感")
    void shouldLoginAndBindSession() {
        authService.register(new MockHttpServletRequest(), registerRequest("tom_2026", "tom12345", null));

        MockHttpServletRequest request = new MockHttpServletRequest();
        UserAccount user = authService.login(request, "Tom_2026", "tom12345");

        assertThat(user.username()).isEqualTo("tom_2026");
        assertThat(request.getSession(false).getAttribute(SessionKeys.LOGIN_USER_ID)).isEqualTo(user.id());
    }

    @Test
    @DisplayName("密码错误返回 401，且不泄露账号是否存在")
    void shouldRejectWrongPassword() {
        authService.register(new MockHttpServletRequest(), registerRequest("tom_2026", "tom12345", null));

        assertThatThrownBy(() -> authService.login(new MockHttpServletRequest(), "tom_2026", "wrong12345"))
                .isInstanceOf(BizException.class)
                .hasMessage("用户名或密码错误");
        assertThatThrownBy(() -> authService.login(new MockHttpServletRequest(), "not_exists", "wrong12345"))
                .isInstanceOf(BizException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    @DisplayName("连续登录失败达到阈值后锁定账号")
    void shouldLockAfterTooManyFailures() {
        authService.register(new MockHttpServletRequest(), registerRequest("tom_2026", "tom12345", null));

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> authService.login(new MockHttpServletRequest(), "tom_2026", "wrong12345"))
                    .isInstanceOf(BizException.class);
        }

        assertThatThrownBy(() -> authService.login(new MockHttpServletRequest(), "tom_2026", "tom12345"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("登录失败次数过多")
                .extracting(error -> ((BizException) error).getStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("退出登录后当前用户为空")
    void shouldClearSessionOnLogout() {
        authService.register(new MockHttpServletRequest(), registerRequest("tom_2026", "tom12345", null));
        MockHttpServletRequest request = new MockHttpServletRequest();
        authService.login(request, "tom_2026", "tom12345");
        assertThat(authService.currentUser(request)).isPresent();

        authService.logout(request);

        assertThat(authService.currentUser(request)).isEmpty();
    }
}
