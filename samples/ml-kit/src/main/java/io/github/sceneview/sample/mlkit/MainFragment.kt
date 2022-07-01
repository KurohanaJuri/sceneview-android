package io.github.sceneview.sample.mlkit

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import io.github.sceneview.ar.ArSceneView


class MainFragment : Fragment(R.layout.fragment_main) {
    lateinit var sceneView: ArSceneView
    lateinit var loadingView: View


    var isLoading = false
        set(value) {
            field = value
            loadingView.isGone = !value
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingView = view.findViewById(R.id.loadingView)

        sceneView = view.findViewById<ArSceneView?>(R.id.sceneView).apply {
        }


    }

}