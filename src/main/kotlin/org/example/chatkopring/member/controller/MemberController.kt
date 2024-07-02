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
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.util.StringUtils
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

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
        log.info("[$loginId] Log Out Successfully")
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
     * 회원가입
     */
    @PostMapping("/signup")
    fun signUp(@RequestBody @Valid memberDto: MemberDto): BaseResponse<Unit> {
        val resultMsg: String
        val role: String
        if (StringUtils.hasText(memberDto.businessId)) {
            memberService.validateAdminSignUp(memberDto)
            resultMsg = memberService.signUp(memberDto, Role.ADMIN)
            role = Role.ADMIN.name
        }else{
            resultMsg = memberService.signUp(memberDto)
            role = Role.MEMBER.name
        }
        log.info("[${memberDto.loginId} / $role] Sign Up Successfully")
        return BaseResponse(message = resultMsg)
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    fun login(@RequestBody @Valid loginDto: LoginDto): BaseResponse<Map<String, Any?>> {
        val memberInfo = memberService.login(loginDto)
        val role = memberInfo["role"]
        log.info("[${loginDto.loginId} / $role] Log In Successfully")
        return BaseResponse(data = memberInfo)
    }


    /**
     * 내 정보 조회
     */
    @GetMapping("/info")
    fun searchMyInfo(@AuthenticationPrincipal customUser: CustomUser): BaseResponse<MemberResponse> {
        val response = memberService.searchMyInfo(customUser.userId)
        log.info("[${customUser.username}] Request Member Info")
        return BaseResponse(data = response)
    }

    /**
     * 프로필 사진 요청
     */
    @GetMapping("/info/image")
    fun searchMyInfoWithImage(@AuthenticationPrincipal customUser: CustomUser): ResponseEntity<Any> {
        return memberService.searchMyInfoWithImage(customUser.userId)
    }

    /**
     * 내 정보 수정
     */
    @PutMapping("/info")
    fun saveMyInfo(@RequestPart(name = "image", required = false) image: MultipartFile?,
                   @RequestPart("member") @Valid memberDto: MemberDto,
                   @AuthenticationPrincipal customUser: CustomUser): BaseResponse<Unit> {
        requireNotNull(memberDto.id) { "id가 null 입니다." }
        require(memberDto.id == customUser.userId) { "AccessToken의 id와 입력된 id가 일치하지 않습니다." }
        memberDto.companyCode?.let { memberService.validateCompanyCode(it) }
        val authorityRole = customUser.authorities.first().authority    // = SimpleGrantedAuthority.authority
        val resultMsg: String = memberService.saveMyInfo(memberDto, authorityRole.substring("ROLE_".length), image)
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
    fun updateState(@RequestBody memberDto: MemberDto,
                    @AuthenticationPrincipal customUser: CustomUser): BaseResponse<Unit> {
        requireNotNull(memberDto.id) { "회원 id(PK) 값이 null 입니다." }
        val adminCompanyCode = memberService.searchMyInfo(customUser.userId).companyCode
        require(adminCompanyCode == memberDto.companyCode) { "Admin의 companyCode와 Member의 companyCode가 일치하지 않습니다." }
        val resultMsg: String = memberService.updateState(memberDto)
        return BaseResponse(message = resultMsg)
    }

    @GetMapping("/colleague")
    fun findColleague(@AuthenticationPrincipal customUser: CustomUser): BaseResponse<List<MemberResponse>> {
        val response = memberService.findColleague(customUser.userId)
        log.info("[${customUser.username}] Request Colleague list : ${response.size}")
        return BaseResponse(data = response)
    }

    @GetMapping("/colleague/notJoin")
    fun findNotJoinedColleague(@RequestParam roomId: String,
                               @AuthenticationPrincipal customUser: CustomUser): BaseResponse<List<MemberResponse>> {
        val response = memberService.findColleague(customUser.userId, roomId)
        log.info("[${customUser.username}] Request Not Joined Colleague list : ${response.size}")
        return BaseResponse(data = response,
            message = if (response.isEmpty()) "모든 사용자가 참여중입니다." else "${response.size}명의 미참여 유저가 있습니다.")
    }

    @GetMapping("/users")
    fun getConnectedUsers(): BaseResponse<MutableSet<String>> = BaseResponse(data = memberService.getConnectedUsers())

}