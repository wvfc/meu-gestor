package com.meugestor.app.data.repository

import com.meugestor.app.data.database.dao.GoalDao
import com.meugestor.app.data.database.entity.GoalEntity
import com.meugestor.app.data.database.entity.GoalPeriod
import com.meugestor.app.util.DateUtils
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class GoalProgress(
    val goal: GoalEntity,
    val spentAmount: Double,
    val percentUsed: Double,
    val periodStart: String = goal.startDate,
    val periodEnd: String = goal.endDate,
    val categoryName: String? = null
)

class GoalRepository(private val goalDao: GoalDao) {

    fun getAll(): Flow<List<GoalEntity>> = goalDao.getAll()

    fun getActive(): Flow<List<GoalEntity>> = goalDao.getActive()

    suspend fun getById(id: Long): GoalEntity? = goalDao.getById(id)

    suspend fun insert(goal: GoalEntity): Long = goalDao.insert(goal)

    suspend fun update(goal: GoalEntity) = goalDao.update(goal)

    suspend fun delete(goal: GoalEntity) = goalDao.delete(goal)

    /**
     * Checks how much of a goal's budget has been spent in the goal's *current* period.
     * Antes o intervalo gravado na criação era usado para sempre, então uma meta mensal
     * criada em abril nunca passava a acompanhar maio.
     */
    suspend fun checkGoalProgress(goal: GoalEntity, today: LocalDate = DateUtils.todayDate()): GoalProgress {
        val (start, end) = currentPeriod(goal.period, today)
        val spentAmount = goalDao.getSpentAmount(
            categoryId = goal.categoryId,
            startDate = start,
            endDate = end
        )
        val percentUsed = if (goal.targetAmount > 0) {
            (spentAmount / goal.targetAmount) * 100.0
        } else {
            0.0
        }
        return GoalProgress(
            goal = goal,
            spentAmount = spentAmount,
            percentUsed = percentUsed,
            periodStart = start,
            periodEnd = end
        )
    }

    companion object {
        /** Intervalo (início, fim) em ISO do período corrente de uma meta. */
        fun currentPeriod(period: GoalPeriod, today: LocalDate = DateUtils.todayDate()): Pair<String, String> {
            val (start, end) = when (period) {
                GoalPeriod.WEEKLY -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to
                    today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                GoalPeriod.MONTHLY -> today.withDayOfMonth(1) to today.with(TemporalAdjusters.lastDayOfMonth())
                GoalPeriod.YEARLY -> today.withDayOfYear(1) to today.with(TemporalAdjusters.lastDayOfYear())
            }
            return DateUtils.toIso(start) to DateUtils.toIso(end)
        }
    }
}
