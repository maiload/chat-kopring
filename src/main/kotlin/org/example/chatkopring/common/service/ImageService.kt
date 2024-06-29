package org.example.chatkopring.common.service

import org.example.chatkopring.chat.dto.ChatMessageDto
import org.example.chatkopring.chat.entity.ChatImage
import org.example.chatkopring.chat.entity.ChatMessage
import org.example.chatkopring.chat.repository.ChatImageRepository
import org.example.chatkopring.member.entity.Member
import org.example.chatkopring.member.entity.MemberImage
import org.example.chatkopring.member.repository.MemberImageRepository
import org.example.chatkopring.util.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.*

@Service
class ImageService(
    private val memberImageRepository: MemberImageRepository,
    private val chatImageRepository: ChatImageRepository,
) {
    val log = logger()

    @Value("\${image.path.profile}")
    lateinit var profileImageOutputPath: String

    @Value("\${image.path.chat}")
    lateinit var chatImageOutputPath: String

    fun uploadImage(image: MultipartFile, member: Member): MemberImage {
        val originFilename = image.originalFilename ?: "Untitled.jpg"
        val storageFilename = originFilename.generateStorageFileName()
        val outputPath = profileImageOutputPath + storageFilename
        val fileSize = image.size
        val memberImage = compareProfileImage(originFilename, fileSize, image.bytes, member)
        return if(memberImage == null){
            saveImage(image, outputPath)
            MemberImage(originFilename, storageFilename, image.size)
        }else{
            log.info("[${member.loginId}] $originFilename is already saved in images.profile")
            memberImage
        }
    }

    fun uploadChatImage(chatMessageDto: ChatMessageDto, chatMessage: ChatMessage): ChatImage {
        val base64Image = chatMessageDto.image
        requireNotNull(base64Image)
        val originFilename = chatMessageDto.content
        val storageFilename = originFilename.generateStorageFileName()
        val outputPath = chatImageOutputPath + storageFilename
        val fileSize = base64Image.length.toLong()
        val chatImage = compareChatImage(originFilename, fileSize, Base64.getDecoder().decode(base64Image))
        return if(chatImage == null){
            saveBase64Image(base64Image, outputPath)
            ChatImage(originFilename, storageFilename, fileSize, chatMessage)
        }else{
            log.info("[${chatMessageDto.sender}] $originFilename is already saved in images.chat")
            chatImage
        }
    }

    fun deleteImage(member: Member) {
        val savedProfileImage = memberImageRepository.findByMember(member)
        if(savedProfileImage != null){
            val path = Paths.get(profileImageOutputPath + savedProfileImage.storageFileName)
            Files.delete(path)
        }
    }

    private fun compareProfileImage(originFileName: String, fileSize: Long, requestImage: ByteArray, member: Member): MemberImage? {
        val memberImageList = memberImageRepository.findByOriginFileNameAndFileSizeAndMember(originFileName, fileSize, member)
        return memberImageList.firstOrNull {
            val requestImageSHA256 = calculateSHA256(requestImage)
            val file = File(profileImageOutputPath + it.storageFileName)
            val savedImageSHA256 = calculateSHA256(file)
            requestImageSHA256 == savedImageSHA256
        }
    }

    private fun compareChatImage(originFileName: String, fileSize: Long, requestImage: ByteArray): ChatImage? {
        val chatImageList = chatImageRepository.findByOriginFileNameAndFileSize(originFileName, fileSize)
        return chatImageList.firstOrNull {
            val requestImageSHA256 = calculateSHA256(requestImage)
            val file = File(chatImageOutputPath + it.storageFileName)
            val savedImageSHA256 = calculateSHA256(file)
            requestImageSHA256 == savedImageSHA256
        }
    }

    private fun calculateSHA256(imageFile: ByteArray): String {
        val sha256Digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = sha256Digest.digest(imageFile)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun calculateSHA256(file: File): String {
        val sha256Digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                sha256Digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = sha256Digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun saveBase64Image(base64Image: String, outputPath: String) {
        val imageBytes = Base64.getDecoder().decode(base64Image)

        val path: Path = Paths.get(outputPath)
        if (path.parent != null && !Files.exists(path.parent)) {
            Files.createDirectories(path.parent)
        }
        Files.write(path, imageBytes)
//        FileOutputStream(outputPath).use { outputStream ->
//            outputStream.write(imageBytes)
//            outputStream.flush()
//        }
    }

    private fun saveImage(image: MultipartFile, outputPath: String) {
        val path: Path = Paths.get(outputPath)
        if (path.parent != null && !Files.exists(path.parent)) {
            Files.createDirectories(path.parent)
        }
        Files.write(path, image.bytes)
    }

    fun String.generateStorageFileName(): String {
        val currentTimeMillisString = System.currentTimeMillis().toString()
        val parts = this.split('.')
        val fileName = parts.firstOrNull() ?: ""
        val extension = parts.lastOrNull() ?: ""
        return "$fileName-${currentTimeMillisString}.$extension"
    }

    fun getExtension(filename: String): String {
        val parts = filename.split('.')
        return parts.lastOrNull() ?: ""
    }

    fun encodeImageToBase64(storageFileName: String): String? {
        val file = File(chatImageOutputPath + storageFileName)
        file.inputStream().use { inputStream ->
            val bytes = inputStream.readBytes()

            return Base64.getEncoder().encodeToString(bytes)
        }
    }
}