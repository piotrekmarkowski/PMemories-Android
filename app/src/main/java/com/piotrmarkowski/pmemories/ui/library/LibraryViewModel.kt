package com.piotrmarkowski.pmemories.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piotrmarkowski.pmemories.data.AppDatabase
import com.piotrmarkowski.pmemories.data.MediaDateResolver
import com.piotrmarkowski.pmemories.data.ProjectEntity
import com.piotrmarkowski.pmemories.data.ProjectWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/** One year section of the Library list — Android analog of iOS
 * `LibraryView.groupedProjects`. */
data class YearGroup(val year: Int, val projects: List<ProjectWithDetails>)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val projectDao = AppDatabase.get(application).projectDao()
    private val appContext = application.applicationContext

    private val rawProjects: StateFlow<List<ProjectWithDetails>> = projectDao.observeAllWithDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Real trip date per project (earliest photo/video capture date across
     * its items, via `MediaDateResolver`) — Android analog of iOS
     * `LibraryView.tripDates`. Resolving this means a `ContentResolver`
     * query per item, so it's cached here and only recomputed when the
     * underlying project/item list actually changes, not on every
     * recomposition (same reasoning as the iOS comment on `tripDates`).
     */
    private val tripDates = MutableStateFlow<Map<String, Long>>(emptyMap())

    init {
        viewModelScope.launch {
            rawProjects.collect { projects ->
                tripDates.value = withContext(Dispatchers.IO) {
                    projects.associate { details ->
                        val uris = details.items.map { it.mediaUri }
                        details.project.id to MediaDateResolver.earliestCreationDate(appContext, uris)
                    }.filterValues { it != null }.mapValues { it.value!! }
                }
            }
        }
    }

    val groupedProjects: StateFlow<List<YearGroup>> = combine(rawProjects, tripDates) { projects, dates ->
        groupByYear(projects, dates)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Date used for grouping/display — a manual correction
     * (`manualTripDate`) wins over the auto-detected one (photo metadata),
     * which wins over `updatedAt` (fallback until auto-detection catches up,
     * or when the photos carry no date metadata at all). Exposed for the
     * screen to pre-fill the "Edit Date" picker with the CURRENT effective
     * date, same as iOS `startEditingDate`.
     */
    fun effectiveTripDateMillis(details: ProjectWithDetails): Long =
        details.project.manualTripDate ?: tripDates.value[details.project.id] ?: details.project.updatedAt

    private fun groupByYear(projects: List<ProjectWithDetails>, dates: Map<String, Long>): List<YearGroup> {
        val calendar = Calendar.getInstance()
        fun effectiveDate(details: ProjectWithDetails): Long =
            details.project.manualTripDate ?: dates[details.project.id] ?: details.project.updatedAt
        fun yearOf(millis: Long): Int {
            calendar.timeInMillis = millis
            return calendar.get(Calendar.YEAR)
        }
        return projects
            .groupBy { yearOf(effectiveDate(it)) }
            .toSortedMap(compareByDescending { it })
            .map { (year, group) -> YearGroup(year, group.sortedByDescending { effectiveDate(it) }) }
    }

    fun updateManualTripDate(project: ProjectEntity, dateMillis: Long) {
        viewModelScope.launch {
            projectDao.update(project.copy(manualTripDate = dateMillis, updatedAt = System.currentTimeMillis()))
        }
    }

    fun rename(project: ProjectEntity, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            projectDao.update(project.copy(title = trimmed, updatedAt = System.currentTimeMillis()))
        }
    }

    fun delete(project: ProjectEntity) {
        viewModelScope.launch { projectDao.delete(project) }
    }
}
