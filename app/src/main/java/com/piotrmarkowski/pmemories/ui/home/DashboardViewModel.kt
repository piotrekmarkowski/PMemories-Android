package com.piotrmarkowski.pmemories.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.piotrmarkowski.pmemories.data.AppDatabase
import com.piotrmarkowski.pmemories.data.ProjectWithDetails
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val projectDao = AppDatabase.get(application).projectDao()

    /**
     * Most recently updated project that has NO export yet — Android
     * analog of iOS `HomeView.continuableProject`. A finished project (its
     * video already exported) shouldn't read as "pick up where you left
     * off" even if it happens to be the newest one on the list.
     */
    val continuableProject: StateFlow<ProjectWithDetails?> = projectDao.observeAllWithDetails()
        .map { projects -> projects.firstOrNull { it.project.exportedMediaUri == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
