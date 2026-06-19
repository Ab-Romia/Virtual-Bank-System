package com.virtualbank.user.web;

import com.virtualbank.common.security.CurrentUser;
import com.virtualbank.common.web.ApiException;
import com.virtualbank.user.UserService;
import com.virtualbank.user.web.dto.UserProfile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserProfile me() {
        return userService.profile(CurrentUser.requireId());
    }

    /**
     * A caller may only read its own profile. The id always comes from the validated
     * token, never the path, so requesting another user's id is rejected rather than
     * silently honored, which is what closes the IDOR.
     */
    @GetMapping("/{id}")
    public UserProfile byId(@PathVariable String id) {
        if (!id.equals(CurrentUser.requireId())) {
            throw ApiException.forbidden("Cannot access another user's profile");
        }
        return userService.profile(id);
    }
}
