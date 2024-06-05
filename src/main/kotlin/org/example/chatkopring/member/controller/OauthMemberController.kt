package org.example.chatkopring.member.controller

import org.example.chatkopring.util.logger
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/api/member")
@Controller
class OauthMemberController {
    val log = logger()

    // Google
    @GetMapping("/login/google")
    fun googleLogin(): String {
        return "redirect:/oauth2/authorization/google"
    }
}