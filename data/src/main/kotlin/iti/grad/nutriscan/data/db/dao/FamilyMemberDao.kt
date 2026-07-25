package iti.grad.nutriscan.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import iti.grad.nutriscan.data.db.entity.FamilyMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members WHERE ownerUserId = :ownerUserId")
    fun getFamilyMembersFlow(ownerUserId: String): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE ownerUserId = :ownerUserId")
    suspend fun getFamilyMembersOnce(ownerUserId: String): List<FamilyMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMember(member: FamilyMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMembers(members: List<FamilyMemberEntity>)

    /** Full replace of the cached list for this user — used after a successful sync. */
    @Query("DELETE FROM family_members WHERE ownerUserId = :ownerUserId")
    suspend fun clearForUser(ownerUserId: String)

    @Query("DELETE FROM family_members WHERE id = :memberId")
    suspend fun deleteById(memberId: String)

    @Transaction
    suspend fun replaceAllForUser(ownerUserId: String, members: List<FamilyMemberEntity>) {
        clearForUser(ownerUserId)
        insertOrUpdateMembers(members)
    }
}
