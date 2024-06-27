package org.example.chatkopring.member.service

import jakarta.transaction.Transactional
import org.example.chatkopring.chat.repository.ChatRoomRepository
import org.example.chatkopring.chat.repository.ParticipantRepository
import org.example.chatkopring.common.authority.JwtTokenProvider
import org.example.chatkopring.common.authority.TokenInfo
import org.example.chatkopring.common.exception.InvalidInputException
import org.example.chatkopring.common.exception.UnAuthorizationException
import org.example.chatkopring.common.status.Role
import org.example.chatkopring.common.status.RoomType
import org.example.chatkopring.common.status.State
import org.example.chatkopring.member.dto.LoginDto
import org.example.chatkopring.member.dto.MemberDto
import org.example.chatkopring.member.dto.MemberResponse
import org.example.chatkopring.member.entity.BlackList
import org.example.chatkopring.member.entity.Member
import org.example.chatkopring.member.entity.MemberRole
import org.example.chatkopring.member.repository.BlackListRepository
import org.example.chatkopring.member.repository.CompanyRepository
import org.example.chatkopring.member.repository.MemberRepository
import org.example.chatkopring.member.repository.MemberRoleRepository
import org.example.chatkopring.util.logger
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Transactional
@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val memberRoleRepository: MemberRoleRepository,
    private val blackListRepository: BlackListRepository,
    private val companyRepository: CompanyRepository,
    private val participantRepository: ParticipantRepository,
    private val chatRoomRepository: ChatRoomRepository,
    private val authenticationManagerBuilder: AuthenticationManagerBuilder,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    val log = logger()
    val passwordEncoder: PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
    /**
     * 회원가입
     */
    fun signUp(memberDto: MemberDto, role: Role = Role.MEMBER): String {
        // companyCode 확인
        memberDto.companyCode?.let { validateCompanyCode(it) }

        // ID 중복 검사
        var member: Member? = memberRepository.findByLoginId(memberDto.loginId)
        if (member != null) {
            throw InvalidInputException(memberDto.loginId, "이미 등록된 ID 입니다.")
        }

        val originPw = memberDto.password
        val encodedPw = passwordEncoder.encode(originPw)
        member = memberDto.toEntity(encodedPw, role.name)
        memberRepository.save(member)

        val memberRole: MemberRole = MemberRole(null, role, member)
        memberRoleRepository.save(memberRole)

        return "회원가입이 완료되었습니다."
    }

    fun validateCompanyCode(companyCode: String) {
//        requireNotNull(companyCode) { throw InvalidInputException("companyCode", "값이 null 입니다.") }
        require(companyRepository.existsByCompanyCode(companyCode)) { throw InvalidInputException("companyCode", "등록되지 않은 코드입니다.") }
    }

    /**
     * 기업 회원가입 검증
     */
    fun validateAdminSignUp(memberDto: MemberDto) {
        requireNotNull(memberDto.ceoName) { throw InvalidInputException("ceoName", "값이 null 입니다.") }
        requireNotNull(memberDto.businessId) { throw InvalidInputException("businessId", "값이 null 입니다.") }
        require(!memberRepository.existsByBusinessId(memberDto.businessId!!)) { throw InvalidInputException("businessId", "이미 가입된 사업자 번호 입니다.") }
        requireNotNull(memberDto.companyCertificateNumber) { throw InvalidInputException("companyCertificateNumber", "값이 null 입니다.") }
        requireNotNull(memberDto.companyName) { throw InvalidInputException("companyName", "값이 null 입니다.") }
    }

    /**
     * refreshToken 검증
     */
    fun validateRefreshToken(refreshToken: String) {
        require(jwtTokenProvider.validateToken(refreshToken)) { throw UnAuthorizationException(message = "유효하지 않은 Refresh Token 입니다.") }
        require(!blackListRepository.existsByInvalidRefreshToken(refreshToken)) { throw UnAuthorizationException(message = "이미 로그아웃된 사용자입니다!") }
    }


    /**
     * 로그아웃 -> blackList 등록
     */
    fun logout(loginId: String, refreshToken: String) {
        validateRefreshToken(refreshToken)
        if(jwtTokenProvider.getUserLoginId(refreshToken) != loginId) throw UnAuthorizationException(loginId, "로그인한 사용자의 Refresh Token이 아닙니다.")
        blackListRepository.save(BlackList(refreshToken))
    }

    /**
     * 토큰 검증 후 재발급
     */
    fun reissueToken(refreshToken: String): TokenInfo {
        validateRefreshToken(refreshToken)
        val authentication = jwtTokenProvider.getAuthentication(refreshToken)
        blackListRepository.save(BlackList(refreshToken))
        return jwtTokenProvider.createToken(authentication)
    }


    /**
     * 로그인 -> 토큰 발행
     */
    fun login(loginDto: LoginDto): Map<String, Any?> {
        val member = memberRepository.findByLoginId(loginDto.loginId)
            ?: throw InvalidInputException(message = "아이디 혹은 비밀번호를 다시 확인하세요.")
        require(passwordEncoder.matches(loginDto.password, member.password)) { "아이디 혹은 비밀번호를 다시 확인하세요." }
        val authenticationToken = UsernamePasswordAuthenticationToken(loginDto.loginId, member.password)
        val authentication = authenticationManagerBuilder.`object`.authenticate(authenticationToken)
        val tokenInfo = jwtTokenProvider.createToken(authentication)
        return mapOf("tokenInfo" to tokenInfo, "state" to member.state, "companyCode" to member.companyCode)
    }

    /**
     * 내 정보 조회
     */
    fun searchMyInfo(id: Long): MemberResponse {
        val member: Member = memberRepository.findByIdOrNull(id)
            ?: throw InvalidInputException("id", "회원번호(${id})가 존재하지 않는 유저입니다.")
        return member.toResponseDto()
    }

    fun searchMyInfo(loginId: String): MemberResponse {
        val member: Member = memberRepository.findByLoginId(loginId)
            ?: throw InvalidInputException("id", "회원 아이디(${loginId})가 존재하지 않는 유저입니다.")
        return member.toResponseDto()
    }

    fun searchEmployeeInfo(companyCode: String): List<MemberResponse> {
        val members: List<Member> = memberRepository.findByCompanyCode(companyCode)
            ?: throw InvalidInputException("companyCode", "companyCode(${companyCode})로 등록된 회원이 존재하지 않습니다.")
        return members.map { it.toResponseDto() }
    }

    /**
     * 내 정보 수정
     */
    fun saveMyInfo(memberDto: MemberDto, role: String): String {
        val savedPw = memberRepository.findByLoginId(memberDto.loginId)?.password
            ?: throw InvalidInputException("loginId", "존재하지 않는 ID(${memberDto.loginId}) 입니다")
//        if(role == Role.MEMBER.name) require(passwordEncoder.matches(memberDto.password, savedPw)) { "비밀번호를 다시 확인하세요." }
        val member: Member = memberDto.toEntity(savedPw, role)
        memberRepository.save(member)
        return "수정 완료되었습니다."
    }

    /**
     * state 변경
     */
    fun updateState(memberDto: MemberDto): String {
        val member = memberRepository.findById(memberDto.id!!).get()
        member.state = memberDto.state
        memberRepository.save(member)
        return if (memberDto.state == State.APPROVED) "승인되었습니다." else "거절되었습니다."
    }

    /**
     * 같은 CompanyCode 인 사용자 조회
     */
    fun findColleague(userId : Long, roomId: String? = null): List<MemberResponse> {
        val member = memberRepository.findById(userId).get()
        requireNotNull(member.companyCode) { "CompanyCode 가 등록되지 않은 유저 입니다" }
        require(member.state == State.APPROVED) { throw UnAuthorizationException(member.loginId, "[${member.state}] 승인되지 않은 사용자입니다.") }
        val allColleagues = memberRepository.findByCompanyCodeAndState(member.companyCode!!, member.state)
        return if (roomId == null) {   // 모든 회사 사용자
            allColleagues.map { it.toResponseDto() }
        }else{  // 특정 방에 없는 회사 사용자
            val chatRoom = chatRoomRepository.findById(roomId).get()
            require(chatRoom.roomType != RoomType.PRIVATE) { throw InvalidInputException(roomId, "PRIVATE 채팅방은 사용자를 초대할 수 없습니다.") }
            allColleagues.filter { !participantRepository.existsByChatRoomAndLoginId(chatRoom, it.loginId) }
                .map { it.toResponseDto() }
        }
    }
}