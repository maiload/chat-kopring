package org.example.chatkopring.member.controller

import jakarta.validation.Valid
import org.example.chatkopring.common.authority.TokenInfo
import org.example.chatkopring.common.dto.BaseResponse
import org.example.chatkopring.common.dto.CustomUser
import org.example.chatkopring.common.exception.InvalidInputException
import org.example.chatkopring.common.status.Role
import org.example.chatkopring.member.dto.LoginDto
import org.example.chatkopring.member.dto.MemberDto
import org.example.chatkopring.member.dto.MemberResponse
import org.example.chatkopring.member.service.MemberService
import org.example.chatkopring.util.logger
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/member")
@RestController
class MemberController (
    private val memberService: MemberService,
){
    val log = logger()
    /**
     * 로그아웃
     */
    @GetMapping("/logout")
    fun logout(@RequestHeader("Refresh") refreshToken: String,
        @AuthenticationPrincipal customUser: CustomUser): BaseResponse<String> {
        val loginId = customUser.username
        memberService.logout(loginId, refreshToken)
        return BaseResponse(data = loginId, message = "Log Out Successfully")
    }

    /**
     * 토큰 재발급
     */
    @GetMapping("/reissue-token")
    fun reissueToken(@RequestHeader("Refresh") refreshToken: String): BaseResponse<TokenInfo> {
        val tokenInfo = memberService.reissueToken(refreshToken)
        return BaseResponse(data = tokenInfo)
    }

    /**
     * 일반 회원가입
     */
    @PostMapping("/signup")
    fun signUp(@RequestBody @Valid memberDto: MemberDto): BaseResponse<Unit> {
        val resultMsg: String = memberService.signUp(memberDto)
        return BaseResponse(message = resultMsg)
    }

    /**
     * 기업 회원가입
     */
    @PostMapping("/signup/admin")
    fun adminSignUp(@RequestBody @Valid memberDto: MemberDto): BaseResponse<Unit> {
        memberService.validateAdminSignUp(memberDto)
        val resultMsg: String = memberService.signUp(memberDto, Role.ADMIN)
        return BaseResponse(message = resultMsg)
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    fun login(@RequestBody @Valid loginDto: LoginDto): BaseResponse<Map<String, Any?>> {
        val memberInfo = memberService.login(loginDto)
        return BaseResponse(data = memberInfo)
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
        require(memberDto.id == customUser.userId) { "Token의 id와 입력된 id가 일치하지 않습니다." }
        memberDto.companyCode?.let { memberService.validateCompanyCode(it) }
        val authorityRole = customUser.authorities.first().authority    // = SimpleGrantedAuthority.authority
        val resultMsg: String = memberService.saveMyInfo(memberDto, authorityRole.substring("ROLE_".length))
        return BaseResponse(message = resultMsg)
    }

    /**
     * 사원 리스트 조회
     */
    @GetMapping("/admin/info")
    fun searchEmployeeInfo(@AuthenticationPrincipal customUser: CustomUser): BaseResponse<List<MemberResponse>> {
        val companyCode = memberService.searchMyInfo(customUser.userId).companyCode
            ?: throw InvalidInputException("companyCode", "Admin 의 companyCode 가 등록되어있지 않습니다.")
        val response = memberService.searchEmployeeInfo(companyCode)
        return BaseResponse(data = response)
    }

    /**
     * 사원 state 변경
     */
    @PutMapping("/admin/info")
    fun updateState(@RequestBody @Valid memberDto: MemberDto,
                    @AuthenticationPrincipal customUser: CustomUser): BaseResponse<Unit> {
        val adminCompanyCode = memberService.searchMyInfo(customUser.userId).companyCode
        require(adminCompanyCode == memberDto.companyCode) { "Admin의 companyCode와 Member의 companyCode가 일치하지 않습니다." }
        val resultMsg: String = memberService.updateState(memberDto)
        return BaseResponse(message = resultMsg)
    }

    @GetMapping("/colleague")
    fun findColleague(@AuthenticationPrincipal customUser: CustomUser): BaseResponse<List<MemberResponse>> {
        val response = memberService.findColleague(customUser.userId)
        return BaseResponse(data = response)
    }

    @GetMapping("/colleague/notJoin")
    fun findNotJoinedColleague(@RequestParam roomId: String,
                               @AuthenticationPrincipal customUser: CustomUser): BaseResponse<List<MemberResponse>> {
        val response = memberService.findColleague(customUser.userId, roomId)
        return BaseResponse(data = response,
            message = if (response.isEmpty()) "모든 사용자가 참여중입니다." else "${response.size}명의 미참여 유저가 있습니다.")
    }
}