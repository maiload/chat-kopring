package org.example.chatkopring.member.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/api/member")
@Controller
class OauthMemberController {

    // Google
    @GetMapping("/login/google")
    fun googleLogin(): String = "redirect:/oauth2/authorization/google"
}