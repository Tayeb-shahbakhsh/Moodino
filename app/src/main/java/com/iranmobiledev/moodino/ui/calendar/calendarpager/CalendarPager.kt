package com.iranmobiledev.moodino.ui.calendar.calendarpager

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.iranmobiledev.moodino.R
import com.iranmobiledev.moodino.data.Entry
import com.iranmobiledev.moodino.databinding.FragmentMonthBinding
import io.github.persiancalendar.calendar.AbstractDate
import java.lang.ref.WeakReference

class CalendarPager(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    // Public API
    var onDayClicked = fun(_: Jdn) {}
    var onDayLongClicked = fun(_: Jdn) {}

    // Selected month is visible current month of the pager, maybe a day is not selected on it yet
    var onMonthSelected = fun() {}

    private val baseJdn = Jdn.today()
    val selectedMonth: AbstractDate
        get() = mainCalendar.getMonthStartFromMonthsDistance(
            baseJdn, -applyOffset(viewPager.currentItem)
        )

    private var pagerAdapter: PagerAdapter? = null

    fun setSelectedDay(
        jdn: Jdn, highlight: Boolean = true, monthChange: Boolean = true,
        smoothScroll: Boolean = true
    ) {
        selectedJdn = if (highlight) jdn else null
        jdn.dayOfWeekName
        if (monthChange) {
            viewPager.setCurrentItem(
                applyOffset(position = -mainCalendar.getMonthsDistance(baseJdn, jdn)), smoothScroll
            )
        }
        refresh()
    }

    fun setEntries(entries: List<Entry>?) {
        pagerAdapter?.setEntries(entries)
    }

    // Public API, to be reviewed
    fun refresh(isEventsModified: Boolean = false) = pagesViewHolders
        .mapNotNull { it.get() }.forEach { it.pageRefresh(isEventsModified, selectedJdn) }

    private val pagesViewHolders = ArrayList<WeakReference<PagerAdapter.ViewHolder>>()

    // Package API, to be rewritten with viewPager.adapter.notifyItemChanged()
    private fun addViewHolder(vh: PagerAdapter.ViewHolder) = pagesViewHolders.add(WeakReference(vh))

    //   private val monthsLimit = 5000 // this should be an even number

    // private fun applyOffset(position: Int) = monthsLimit / 2 - position
    private fun applyOffset(position: Int) = monthLimit / 2 - position

    private val viewPager = ViewPager2(context)
    private var selectedJdn: Jdn? = null

    init {
        pagerAdapter = PagerAdapter()
        viewPager.adapter = pagerAdapter
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = refresh()

        })
        addView(viewPager)
        //    viewPager.setCurrentItem(applyOffset(0), false)
        viewPager.setCurrentItem(monthPositionGlobal, true)
        viewPager.isUserInputEnabled = false
    }

    inner class PagerAdapter : RecyclerView.Adapter<PagerAdapter.ViewHolder>() {

        private var entries: List<Entry>? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = FragmentMonthBinding.inflate(parent.context.layoutInflater, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(position)

        override fun getItemCount() = monthLimit

        private val sharedDayViewData = SharedDayViewData(
            context, resources.getDimension(R.dimen.grid_calendar_height) / 7 - 4.5.sp
        )

        @SuppressLint("NotifyDataSetChanged")
        fun setEntries(entries: List<Entry>?) {
            this.entries = entries
            notifyDataSetChanged()
        }

        inner class ViewHolder(val binding: FragmentMonthBinding) :
            RecyclerView.ViewHolder(binding.root) {

            var pageRefresh = fun(_: Boolean, _: Jdn?) {}

            init {
                binding.monthView.initialize(sharedDayViewData, this@CalendarPager)

//                binding.previous.let {
//                    it.contentDescription = it.context.getString(
//                        R.string.previous_x, it.context.getString(R.string.month)
//                    )
//                    it.rotateTo(ArrowView.Direction.START)
//                    it.setOnClickListener {
//                        viewPager.setCurrentItem(viewPager.currentItem - 1, true)
//                    }
//                    it.setOnLongClickListener {
//                        viewPager.setCurrentItem(viewPager.currentItem - 12, false)
//                        true
//                    }
//                }

//                binding.next.let {
//                    it.contentDescription = it.context.getString(
//                        R.string.next_x, it.context.getString(R.string.month)
//                    )
//                    it.rotateTo(ArrowView.Direction.END)
//                    it.setOnClickListener {
//                        viewPager.setCurrentItem(viewPager.currentItem + 1, true)
//                    }
//                    it.setOnLongClickListener {
//                        viewPager.setCurrentItem(viewPager.currentItem + 12, false)
//                        true
//                    }
//                }
                addViewHolder(this)
            }

            fun bind(position: Int) {
                val monthStartDate = mainCalendar.getMonthStartFromMonthsDistance(
                    baseJdn, -applyOffset(position)
                )
                val monthStartJdn = Jdn(monthStartDate)
                val monthLength =
                    mainCalendar.getMonthLength(monthStartDate.year, monthStartDate.month)
                binding.monthView.bind(monthStartJdn, monthStartDate)

                binding.monthView.setEntries(entries)
                entries = null

                pageRefresh = { isEventsModification: Boolean, jdn: Jdn? ->
                    if (viewPager.currentItem == position) {
                        if (isEventsModification && jdn != null) {
                            // binding.monthView.initializeMonthEvents()
                            onDayClicked(jdn)
                        } else {
                            onMonthSelected()
                        }

                        binding.monthView.selectDay(
                            if (jdn != null && jdn >= monthStartJdn && jdn - monthStartJdn + 1 <= monthLength)
                                jdn - monthStartJdn + 1
                            else -1
                        )
                    } else binding.monthView.selectDay(-1)
                }

                pageRefresh(false, selectedJdn)
            }
        }
    }

    fun clickOnNextMonth() {
        viewPager.setCurrentItem(viewPager.currentItem + 1, true)
    }

    fun clickOnPreviousMonth() {
        viewPager.setCurrentItem(viewPager.currentItem - 1, true)
    }


}
