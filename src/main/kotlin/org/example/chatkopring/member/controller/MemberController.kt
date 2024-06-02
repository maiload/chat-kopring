package org.example.chatkopring.member.controller

import jakarta.validation.Valid
import org.example.chatkopring.common.authority.TokenInfo
import org.example.chatkopring.common.dto.BaseResponse
import org.example.chatkopring.common.dto.CustomUser
import org.example.chatkopring.member.dto.LoginDto
import org.example.chatkopring.member.dto.MemberDto
import org.example.chatkopring.member.dto.MemberResponse
import org.example.chatkopring.member.service.MemberService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/member")
@RestController
class MemberController (
    private val memberService: MemberService,
){
    /**
     * 로그아웃
     */
    @GetMapping("/logout")
    fun logout(@AuthenticationPrincipal customUser: CustomUser): BaseResponse<Long> {
        return BaseResponse(data = customUser.userId, message = "Log Out Successfully")
    }

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    fun signUp(@RequestBody @Valid memberDto: MemberDto): BaseResponse<Unit> {
        val resultMsg: String = memberService.signUp(memberDto)
        return BaseResponse(message = resultMsg)
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    fun login(@RequestBody @Valid loginDto: LoginDto): BaseResponse<TokenInfo> {
        val tokenInfo = memberService.login(loginDto)
        return BaseResponse(data = tokenInfo)
    }

    /**
     * 내 정보 조회
     */
    @GetMapping("/info")
    fun searchMyInfo(@AuthenticationPrincipal customUser: CustomUser): BaseResponse<MemberResponse> {
        val response = memberService.searchMyInfo(customUser.userId)
        return BaseResponse(data = response)
    }

    /**
     * 내 정보 수정
     */
    @PutMapping("/info")
    fun saveMyInfo(@RequestBody @Valid memberDto: MemberDto,
                   @AuthenticationPrincipal customUser: CustomUser): BaseResponse<Unit> {
        requireNotNull(memberDto.id) { "id가 null 입니다." }
        require(memberDto.id == customUser.userId) { "Token의 id와 dto의 id가 일치하지 않습니다." }
        val resultMsg: String = memberService.saveMyInfo(memberDto)
        return BaseResponse(message = resultMsg)
    }
}