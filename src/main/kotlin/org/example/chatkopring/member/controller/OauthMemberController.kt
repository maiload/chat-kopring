package org.example.chatkopring.member.controller

import org.example.chatkopring.util.logger
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class OauthMemberController {
    val log = logger()

    // Google
    @GetMapping("/api/member/login/google")
    fun googleLogin(): String {
        return "redirect:/oauth2/authorization/google"
    }

}