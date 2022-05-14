package com.iranmobiledev.moodino.ui.states.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.iranmobiledev.moodino.R
import com.iranmobiledev.moodino.base.BaseViewModel
import com.iranmobiledev.moodino.data.EntryDate
import com.iranmobiledev.moodino.repository.entry.EntryRepository
import com.iranmobiledev.moodino.utlis.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import saman.zamani.persiandate.PersianDate
import java.time.LocalDate
import kotlin.collections.ArrayList


class StatsFragmentViewModel(
    private val entryRepository: EntryRepository
) : BaseViewModel() {

    val TAG = "viewmodelStats"

    private val _entries = MutableLiveData(listOf<com.iranmobiledev.moodino.data.Entry>())
    private val _lineChartEntries = MutableLiveData(listOf(Entry(1f, 2f)))
    private val _lineChartDates = MutableLiveData(listOf(10))
    private val pieChartEntries = mutableListOf<PieEntry>()
    private val _weekDays = MutableLiveData<ArrayList<Int>>()
    private val _longestChainLiveData: MutableLiveData<Int> = MutableLiveData(0)
    private val _latestChainLiveData: MutableLiveData<Int> = MutableLiveData(0)
    private val _lastFiveDaysStatus: MutableLiveData<List<Boolean>> =
        MutableLiveData(listOf(false, false, false, false, false))

    val lineChartEntries: LiveData<List<Entry>>
        get() = _lineChartEntries
    val longestChainLiveData: LiveData<Int>
        get() = _longestChainLiveData
    val latestChainLiveData: LiveData<Int>
        get() = _latestChainLiveData
    val lastFiveDaysStatus: LiveData<List<Boolean>>
        get() = _lastFiveDaysStatus
    val entries: LiveData<List<com.iranmobiledev.moodino.data.Entry>>
        get() = _entries
    val weekDays: LiveData<ArrayList<Int>>
        get() = _weekDays

    fun getEntries() {
        viewModelScope.launch {
            entryRepository.getAll().collectLatest { entries ->
                _entries.postValue(entries)
            }
        }
    }

    fun initDaysInRow() {
        viewModelScope.launch {
            _entries.asFlow().collectLatest{ entries ->

                //Adding week days name to daysInRow card
                launch {
                    getFiveDaysAsWeekDays()
                }

                val dates = withContext(Dispatchers.IO) {
                    getDatesFromEntries(entries)
                }

                launch {
                    getLongestChainFromDates(dates)
                }

                launch {
                    getLatestChain(dates)
                }

                launch {
                    getLastFiveDaysStatus(dates)
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getLongestChainFromDates(datesList: List<EntryDate>) {
        val reversedDates = datesList.distinct().reversed()
        var chainLengthMax = 0
        var chainLength = 1

        for (date in reversedDates) {
            val nextDateAsLocalDate = LocalDate.of(date.year, date.month, date.day).minusDays(1)
            if (date != reversedDates.last()) {
                val nextDateElement = reversedDates[reversedDates.indexOf(date) + 1]
                val nextDate =
                    LocalDate.of(nextDateElement.year, nextDateElement.month, nextDateElement.day)
                if (nextDateAsLocalDate == nextDate) {
                    if (reversedDates.size <= 2) {
                        chainLengthMax++
                    } else {
                        chainLength++
                    }
                } else {
                    if (chainLength >= chainLengthMax) {
                        chainLengthMax = chainLength
                    }
                    chainLength = 0
                }
            } else {
                if (LocalDate.of(date.year, date.month, date.day)
                        .minusDays(1) == nextDateAsLocalDate
                ) {
                    chainLengthMax++
                }
            }
        }
        _longestChainLiveData.postValue(chainLengthMax)
    }

    @SuppressLint("NewApi")
    private fun getLatestChain(dates: List<EntryDate>) {

        val reversedDate = dates.distinct().reversed()
        var latestChain = 0

        for (date in reversedDate) {

            val nextDateAsLocalDate =
                LocalDate.of(date.year, date.month, date.day).minusDays(1)

            if (date == reversedDate.first()) latestChain = 1

            if (date != reversedDate.last()) {
                val nextDateElement = reversedDate[reversedDate.indexOf(date) + 1]
                val nextDate =
                    LocalDate.of(nextDateElement.year, nextDateElement.month, nextDateElement.day)

                if (nextDateAsLocalDate == nextDate) latestChain++ else break
            }
        }
        _latestChainLiveData.postValue(latestChain)
    }

    fun initLineChart() {
        viewModelScope.launch {
            _entries.asFlow().collectLatest {
                launch {
                    getEntriesForLineChart(it)
                }
            }
        }
    }

    private fun getEntriesForLineChart(entries: List<com.iranmobiledev.moodino.data.Entry>) {
        var entriesLineChart: MutableList<Entry> = mutableListOf()
        val entriesDaysNumber: MutableList<Int> = mutableListOf()
        val optimizedLineChartEntries = mutableListOf<Entry>()

        for (entry in entries) {
            val y = getYFromEntry(entry)
            val x = getXFromEntry(entry)
            entriesDaysNumber.add(entry.date!!.day)
            entriesLineChart.add(Entry(x, y))
        }



        optimizeEntries(entriesLineChart, optimizedLineChartEntries)

        optimizedLineChartEntries.forEach {
            Log.d(TAG, "getEntriesForLineChart: $it")
        }

        _lineChartEntries.postValue(optimizedLineChartEntries)
        _lineChartDates.postValue(entriesDaysNumber.toList())
    }

    private fun optimizeEntries(
        entries: MutableList<Entry>,
        optimizedLineChartEntries: MutableList<Entry>
    ) {
        if (entries.size == 0) {
            return
        }

        val first = entries[0]
        val filtered = entries.filter { it.x == first.x }
        optimizedLineChartEntries.add(getNewChartEntry(filtered))
        val notFiltered = entries.filterNot { it.x == first.x }

        optimizeEntries(notFiltered as MutableList<Entry>, optimizedLineChartEntries)
    }

    private fun getNewChartEntry(filteredList: List<Entry>): Entry {
        var sum = 0f
        filteredList.forEach {
            sum += it.y
        }
        val averageY = sum / filteredList.size.toFloat()
        return Entry(filteredList[0].x, averageY)
    }

    private fun getXFromEntry(entry: com.iranmobiledev.moodino.data.Entry): Float {
        return entry.date!!.day.toFloat()
    }

    private fun getYFromEntry(entry: com.iranmobiledev.moodino.data.Entry): Float {
        return when (entry.title) {
            R.string.rad -> 5f
            R.string.good -> 4f
            R.string.meh -> 3f
            R.string.bad -> 2f
            R.string.awful -> 1f
            else -> {
                3f
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getFiveDaysAsWeekDays() {
        var days = arrayListOf<Int>()
        for (i in 0..4) {
            val date = PersianDate.today().addDay(-i.toLong())
            when (date.dayOfWeek()) {
                0 -> days.add(R.string.sat)
                1 -> days.add(R.string.sun)
                2 -> days.add(R.string.mon)
                3 -> days.add(R.string.tue)
                4 -> days.add(R.string.wed)
                5 -> days.add(R.string.thu)
                6 -> days.add(R.string.fri)
            }
        }

        days.reverse()
        _weekDays.postValue(days)
    }

    private fun getDatesFromEntries(entries: List<com.iranmobiledev.moodino.data.Entry>): List<EntryDate> {
        val dates = mutableListOf<EntryDate>()
        for (entry in entries) {
            dates.add(entry.date!!)
        }
        return dates
    }

    @SuppressLint("NewApi")
    fun getLastFiveDaysStatus(entryDates: List<EntryDate>) {
        val lastFiveDayStatus = mutableListOf<Boolean>()

        for (i in 0..4) {
            val date = PersianDate.today().addDay(-i.toLong())
            if (entryDates.contains(EntryDate(date.shYear, date.shMonth, date.shDay))) {
                lastFiveDayStatus.add(true)
            } else {
                lastFiveDayStatus.add(false)
            }
        }

        _lastFiveDaysStatus.postValue(lastFiveDayStatus)
    }

    fun initPieChart(pieChart: PieChart, context: Context) {
        val entries = getEntriesForPieChart()
        //mocked entries for chart
        if (pieChartEntries.isEmpty()) {

        }

        val colors = arrayListOf<Int>()
        for (color in ColorArray.colors) {
            colors.add(color)
        }

        val dataSet = PieDataSet(entries, null)
        dataSet.apply {
            setColors(colors)
        }


        val pieData = PieData(dataSet)
        pieData.apply {
            setDrawValues(true)
            setValueTextSize(0f)
            setValueTextColor(Color.TRANSPARENT)

        }

        pieChart.apply {
            data = pieData
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 65f
            setDrawEntryLabels(false)
            isRotationEnabled = false
            legend.isEnabled = false
            centerText = "16"
            setCenterTextColor(Color.GRAY)
            setCenterTextSize(24f)
            setDrawRoundedSlices(true)
        }

    }

    fun getEntriesForPieChart():
            MutableList<PieEntry> {
        return pieChartEntries
    }

    fun setEntriesForPieChart(entriesList: MutableList<PieEntry>) {
        entriesList.forEach {
            pieChartEntries.add(it)
        }
    }

    val datesMock = listOf<EntryDate>(
        EntryDate(1401, 2, 2),
        EntryDate(1401, 2, 3),
        EntryDate(1401, 2, 4),
        EntryDate(1401, 2, 5),
        EntryDate(1401, 2, 8),
        EntryDate(1401, 2, 9),
        EntryDate(1401, 2, 10),
        EntryDate(1401, 2, 11),
        EntryDate(1401, 2, 12),
        EntryDate(1401, 2, 13),
        EntryDate(1401, 2, 18),
        EntryDate(1401, 2, 19),
        EntryDate(1401, 2, 20),
        EntryDate(1401, 2, 21),
        EntryDate(1401, 2, 22),
        EntryDate(1401, 2, 23),
        EntryDate(1401, 2, 24),
        EntryDate(1401, 2, 25),
        EntryDate(1401, 2, 26),
        EntryDate(1401, 2, 27),
        EntryDate(1401, 2, 28),
    )

}