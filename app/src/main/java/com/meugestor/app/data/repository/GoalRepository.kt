package com.meugestor.app.data.repository

import com.meugestor.app.data.database.dao.GoalDao
import com.meugestor.app.data.database.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

data class GoalProgress(
    val goal: GoalEntity,
    val spentAmount: Double,
    val percentUsed: Double
)

class GoalRepository(private val goalDao: GoalDao) {

    fun getAll(): Flow<List<GoalEntity>> = goalDao.getAll()

    fun getActive(): Flow<List<GoalEntity>> = goalDao.getActive()

    suspend fun getById(id: Long): GoalEntity? = goalDao.getById(id)

    suspend fun insert(goal: GoalEntity): Long = goalDao.insert(goal)

    suspend fun update(goal: GoalEntity) = goalDao.update(goal)

    suspend fun delete(goal: GoalEntity) = goalDao.delete(goal)

    /**
     * Checks how much of a goal's budget has been spent in the goal's date range.
     * Returns a [GoalProgress] with the spent amount and percentage used.
     */
    suspend fun checkGoalProgress(goal: GoalEntity): GoalProgress {
        val spentAmount = goalDao.getSpentAmount(
            categoryId = goal.categoryId,
            startDate = goal.startDate,
            endDate = goal.endDate
        )
        val percentUsed = if (goal.targetAmount > 0) {
            (spentAmount / goal.targetAmount) * 100.0
        } else {
            0.0
        }
        return GoalProgress(
            goal = goal,
            spentAmount = spentAmount,
            percentUsed = percentUsed
        )
    }
}
