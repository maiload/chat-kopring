//package org.example.chatkopring.member.controller
//
//import org.example.chatkopring.common.dto.CustomUser
//import org.example.chatkopring.util.logger
//import org.springframework.security.core.annotation.AuthenticationPrincipal
//import org.springframework.stereotype.Controller
//import org.springframework.web.bind.annotation.GetMapping
//
//@Controller
//class SampleController {
//    val log = logger()
//
//    @GetMapping("/login")
//    fun login(): String{
//        return "home";
//    }
//
//    @GetMapping("/api/member/chat")
//    fun chat(@AuthenticationPrincipal customUser: CustomUser): String{
//        log.info("customUser : $customUser")
//        return "chat";
//    }
//}