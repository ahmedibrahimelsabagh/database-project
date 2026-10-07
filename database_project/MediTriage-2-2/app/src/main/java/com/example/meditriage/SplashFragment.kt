package com.example.meditriage

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class SplashFragment : Fragment(R.layout.fragment_splash) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.getStartedBtn).setOnClickListener {
            findNavController().navigate(R.id.action_splash_to_register)
        }

        view.findViewById<View>(R.id.loginBtn).setOnClickListener {
            findNavController().navigate(R.id.action_splash_to_login)
        }
    }
}
