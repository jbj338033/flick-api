package com.flick.core.domain.user

import com.flick.core.support.error.CoreException
import com.flick.core.support.error.ErrorType
import com.flick.storage.db.core.entity.User
import com.flick.storage.db.core.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
) {
    fun getUser(userId: UUID): User =
        userRepository.findByIdOrNull(userId)
            ?: throw CoreException(ErrorType.USER_NOT_FOUND)

    fun getUserByStudentNumber(
        grade: Int,
        room: Int,
        number: Int,
    ): User =
        userRepository.findByGradeAndRoomAndNumber(grade, room, number)
            ?: throw CoreException(ErrorType.USER_NOT_FOUND)

    fun getAllUsers(): List<User> = userRepository.findAll()

    fun searchUsers(query: String): List<User> {
        val trimmed = query.trim()
        if (trimmed.length == 4 && trimmed.all { it.isDigit() }) {
            val grade = trimmed[0].digitToInt()
            val room = trimmed[1].digitToInt()
            val number = trimmed.substring(2).toInt()
            val user = userRepository.findByGradeAndRoomAndNumber(grade, room, number)
            return if (user != null) listOf(user) else emptyList()
        }
        return userRepository.findByNameContaining(trimmed)
    }
}
