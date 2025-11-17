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

        // create an explicit Observer with generic type
        val observer = Observer<Int?> { value ->
            val v = value ?: 0
            textTotal.text = getString(R.string.text_total, v)
        }

        // attach observer
        viewModel.total.observe(viewLifecycleOwner, observer)
    }


    private fun updateText(total: Int) {
        // gunakan view?.findViewById dari fragment view untuk menghindari NPE
        view?.findViewById<TextView>(R.id.text_total_first)?.text =
            getString(R.string.text_total, total)
    }

    private fun prepareViewModel() {
        val viewModel =
            ViewModelProvider(requireActivity()).get(TotalViewModel::class.java)

        // pakai Observer eksplisit supaya Kotlin tidak kebingungan
        viewModel.total.observe(viewLifecycleOwner, Observer<Int?> { total ->
            val safeTotal = total ?: 0
            updateText(safeTotal)
        })
    }

    companion object {
        @JvmStatic
        fun newInstance(): FirstFragment {
            return FirstFragment()
        }
    }
}
