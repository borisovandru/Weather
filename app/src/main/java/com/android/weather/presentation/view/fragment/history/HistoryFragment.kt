package com.android.weather.presentation.view.fragment.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.weather.R
import com.android.weather.data.utils.makeGone
import com.android.weather.data.utils.makeVisible
import com.android.weather.data.utils.showSnackBar
import com.android.weather.databinding.FragmentHistoryBinding
import com.android.weather.presentation.state.AppState
import com.android.weather.presentation.viewmodel.history.HistoryViewModel

class HistoryFragment : Fragment() {

    private val historyViewModel: HistoryViewModel by lazy {
        ViewModelProvider(this).get(HistoryViewModel::class.java)
    }

    private val adapter: HistoryFragmentAdapter by lazy {
        HistoryFragmentAdapter()
    }

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.historyRecyclerView.adapter = adapter
        historyViewModel.historyLiveData.observe(viewLifecycleOwner) { renderData(it) }
        historyViewModel.getAllHistory()
    }

    private fun renderData(appState: AppState) {
        when (appState) {
            is AppState.Success -> {
                binding.historyRecyclerView.makeVisible()
                binding.includeProgressBarLayout.progressBarLayout.makeGone()
                adapter.setData(appState.weatherData)
            }
            is AppState.Loading -> {
                binding.historyRecyclerView.makeGone()
                binding.includeProgressBarLayout.progressBarLayout.makeVisible()
            }
            is AppState.Error -> {
                binding.historyRecyclerView.makeVisible()
                binding.includeProgressBarLayout.progressBarLayout.makeGone()
                binding.historyRecyclerView.showSnackBar(
                    getString(R.string.text_error),
                    getString(R.string.text_reload)
                ) {
                    historyViewModel.getAllHistory()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

