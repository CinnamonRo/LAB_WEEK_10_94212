package com.example.lab_week_10

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.lab_week_10.viewmodels.TotalViewModel

class FirstFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel = ViewModelProvider(requireActivity()).get(TotalViewModel::class.java)
        val textTotal = view.findViewById<TextView>(R.id.text_total_first)
        val textDate = view.findViewById<TextView>(R.id.text_date_first)

        // Observe total and date LiveData from the shared ViewModel
        viewModel.total.observe(viewLifecycleOwner, Observer<Int?> { value ->
            val v = value ?: 0
            textTotal.text = getString(R.string.text_total, v)
        })

        viewModel.date.observe(viewLifecycleOwner, Observer<String?> { d ->
            // For easier debugging, always show the date field.
            // If date is empty, show a placeholder so it's clear the value is not set.
            textDate.visibility = View.VISIBLE
            if (d.isNullOrEmpty()) {
                textDate.text = getString(R.string.text_last_updated, "(not set)")
            } else {
                textDate.text = getString(R.string.text_last_updated, d)
            }
        })
    }


    companion object {
        @JvmStatic
        fun newInstance(): FirstFragment {
            return FirstFragment()
        }
    }
}
