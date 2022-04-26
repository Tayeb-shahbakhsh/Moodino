package com.iranmobiledev.moodino.ui.states

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.iranmobiledev.moodino.base.BaseFragment
import com.iranmobiledev.moodino.data.BottomNavState
import com.iranmobiledev.moodino.databinding.FragmentStatsBinding
import com.iranmobiledev.moodino.ui.states.customView.composable.DaysInYearComposable
import com.iranmobiledev.moodino.ui.states.viewmodel.StatsFragmentViewModel
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.koin.androidx.viewmodel.ext.android.viewModel


class StatsFragment : BaseFragment() {
    private lateinit var binding: FragmentStatsBinding
    private val model : StatsFragmentViewModel by viewModel()
    override fun onResume() {
        super.onResume()
        EventBus.getDefault().post(BottomNavState(true))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStatsBinding.inflate(inflater, container, false)

        binding.yearInPixels.apply{
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                DaysInYearComposable()
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val scope = CoroutineScope(Dispatchers.IO)

        val daysInARowCardBinding = binding.daysInRowCardInclude
        scope.launch {
            model.daysInRowManager(requireContext(),daysInARowCardBinding)
        }

        val lineChart = binding.moodChartCardInclude.moodsLineChart
            model.initializeLineChart(lineChart,requireContext())

        val pieChart = binding.moodCountCardInclude.moodCountPieChart
            model.initializePieChart(pieChart,requireContext())

        model.longestChainLiveData.observe(viewLifecycleOwner){
            binding.daysInRowCardInclude.longestChainTextView.text = it.toString()
        }

    }
}