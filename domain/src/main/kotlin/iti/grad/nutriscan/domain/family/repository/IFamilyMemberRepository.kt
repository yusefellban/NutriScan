package iti.grad.nutriscan.domain.family.repository

import iti.grad.nutriscan.domain.family.model.FamilyMember
import iti.grad.nutriscan.domain.family.model.FamilyMemberInput
import kotlinx.coroutines.flow.Flow
import java.io.File

interface IFamilyMemberRepository {
    /** Single source of truth — reads from Room, kept in sync with the backend. */
    fun getFamilyMembers(): Flow<List<FamilyMember>>

    /** Adds a member: optimistic local insert, then syncs the full list to the backend. */
    suspend fun addFamilyMember(input: FamilyMemberInput): Result<Unit>

    /** Removes a member: optimistic local delete, then syncs the full list to the backend. */
    suspend fun removeFamilyMember(memberId: String): Result<Unit>

    /** Updates a member: optimistic local update, then syncs the full list to the backend. */
    suspend fun updateFamilyMember(memberId: String, input: FamilyMemberInput): Result<Unit>

    /** Uploads or replaces a family member photo by member id. */
    suspend fun uploadFamilyMemberImage(memberId: String, imageFile: File): Result<Unit>
}
