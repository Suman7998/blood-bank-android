package com.example.bloodbank.ui.donors

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.bloodbank.databinding.FragmentDonorsBinding
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloodbank.ui.donors.DonorAdapter
import android.app.AlertDialog
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.bloodbank.R
import com.example.bloodbank.data.entity.Donor
import com.example.bloodbank.data.repository.DonorRepository
import com.example.bloodbank.databinding.DialogAddDonorBinding
import kotlinx.coroutines.launch
import com.example.bloodbank.util.NetworkUtils

class DonorsFragment : Fragment() {
    private var _binding: FragmentDonorsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: DonorRepository
    private val adapter = DonorAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDonorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = DonorRepository(requireContext())

        binding.rvDonors.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDonors.adapter = adapter

        repository.donors.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }

        binding.btnLoadSamples.setOnClickListener {
            lifecycleScope.launch {
                repository.loadSamples()
                Toast.makeText(requireContext(), "Loaded 100 sample donors", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSyncPush.setOnClickListener {
            lifecycleScope.launch {
                if (!NetworkUtils.isOnline(requireContext())) {
                    Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                repository.pushToCloudSafe()
                Toast.makeText(requireContext(), "Pushed donors to cloud", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSyncPull.setOnClickListener {
            lifecycleScope.launch {
                if (!NetworkUtils.isOnline(requireContext())) {
                    Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                repository.pullFromCloudSafe()
                Toast.makeText(requireContext(), "Pulled donors from cloud", Toast.LENGTH_SHORT).show()
            }
        }

        binding.fabAddDonor.setOnClickListener { showAddDialog() }
    }

    private fun showAddDialog() {
        val dBinding = DialogAddDonorBinding.inflate(layoutInflater)
        val groups = resources.getStringArray(R.array.blood_types)
        val groupsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, groups)
        dBinding.etBloodGroup.setAdapter(groupsAdapter)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add Donor")
            .setView(dBinding.root)
            .setPositiveButton(getString(R.string.submit), null)
            .setNegativeButton(getString(R.string.cancel)) { d, _ -> d.dismiss() }
            .create()

        dialog.setOnShowListener {
            val btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btn.setOnClickListener {
                val name = dBinding.etName.text?.toString()?.trim().orEmpty()
                val bg = dBinding.etBloodGroup.text?.toString()?.trim().orEmpty()
                val phone = dBinding.etPhone.text?.toString()?.trim().orEmpty()
                val city = dBinding.etCity.text?.toString()?.trim().orEmpty()

                if (name.isEmpty() || bg.isEmpty() || phone.isEmpty() || city.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.error_field_required), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    repository.addDonor(Donor(name = name, bloodGroup = bg, phone = phone, city = city))
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
