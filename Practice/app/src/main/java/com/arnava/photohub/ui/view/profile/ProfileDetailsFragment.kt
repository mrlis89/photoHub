package com.arnava.photohub.ui.view.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arnava.photohub.databinding.FragmentProfileDetailsBinding
import com.arnava.photohub.data.local.UserInfoStorage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileDetailsFragment : Fragment() {

    private var _binding: FragmentProfileDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            userBio.text = UserInfoStorage.userInfo?.bio
            userLocation.text = UserInfoStorage.userInfo?.location.toString()
            userDownloads.text = UserInfoStorage.userInfo?.downloads.toString()
        }
    }
}