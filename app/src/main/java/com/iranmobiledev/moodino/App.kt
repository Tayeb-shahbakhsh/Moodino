package com.iranmobiledev.moodino

import android.app.Application
import android.content.SharedPreferences
import com.iranmobiledev.moodino.data.Activity
import com.iranmobiledev.moodino.database.AppDatabase
import com.iranmobiledev.moodino.database.EntryDao
import com.iranmobiledev.moodino.repository.activity.ActivityRepository
import com.iranmobiledev.moodino.repository.activity.ActivityRepositoryImpl
import com.iranmobiledev.moodino.repository.activity.source.ActivityLocalDataSource
import com.iranmobiledev.moodino.repository.entry.EntryRepository
import com.iranmobiledev.moodino.repository.entry.EntryRepositoryImpl
import com.iranmobiledev.moodino.ui.calendar.CalendarViewModel
import com.iranmobiledev.moodino.repository.more.MoreRepository
import com.iranmobiledev.moodino.repository.more.MoreRepositoryImpl
import com.iranmobiledev.moodino.repository.more.source.MoreLocalDataSource
import com.iranmobiledev.moodino.ui.MainActivity
import com.iranmobiledev.moodino.ui.entry.EntryDetailViewModel
import com.iranmobiledev.moodino.ui.entry.EntryViewModel
import com.iranmobiledev.moodino.ui.more.pinLock.PinLockViewModel
import com.iranmobiledev.moodino.ui.entry.adapter.EntryContainerAdapter
import com.iranmobiledev.moodino.ui.states.viewmodel.StatsFragmentViewModel
import com.iranmobiledev.moodino.utlis.GlideImageLoader
import com.iranmobiledev.moodino.utlis.ImageLoadingService
import com.iranmobiledev.moodino.utlis.MoodinoSharedPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.dsl.module

class App : Application() , KoinComponent{
    override fun onCreate() {
        super.onCreate()

        val database = AppDatabase.getAppDatabase(this)

        val modules = module {
            viewModel { EntryViewModel(get(), get()) }
            viewModel { EntryDetailViewModel(get(), get()) }
            viewModel { CalendarViewModel() }
            viewModel { PinLockViewModel(get()) }
            viewModel {StatsFragmentViewModel(get())}
            factory  <EntryRepository> {EntryRepositoryImpl(database.getEntryDao)}
            factory <ActivityRepository> { ActivityRepositoryImpl(ActivityLocalDataSource(database.getActivityDao)) }
            factory <MoreRepository> { MoreRepositoryImpl(MoreLocalDataSource(MoodinoSharedPreferences.create(this@App))) }
            single <ImageLoadingService>{ GlideImageLoader() }
            single { EntryContainerAdapter() }
        }

        startKoin {
            androidContext(this@App)
            modules(modules)
        }

        val sharedPref : SharedPreferences = MoodinoSharedPreferences.create(this)
        val firstEnter = sharedPref.getBoolean("first_enter", false)
        if(!firstEnter)
            makeDefaultActivities()
    }

    private fun makeDefaultActivities() {
        val viewModel : EntryViewModel = get()
        viewModel.addActivity(Activity(id = null, image = R.drawable.ic_arrow_bottom,title = "achievement", category = "CT"))
        viewModel.addActivity(Activity(id = null, image = R.drawable.ic_arrow_bottom,title = "achievement", category = "CT"))
        viewModel.addActivity(Activity(id = null, image = R.drawable.ic_arrow_bottom,title = "achievement", category = "CT"))
    }
}