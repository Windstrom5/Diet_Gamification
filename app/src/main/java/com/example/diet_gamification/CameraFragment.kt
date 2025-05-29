package com.example.diet_gamification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.diet_gamification.databinding.FragmentCameraBinding
import com.example.diet_gamification.utils.FoodClassifier
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var foodList: List<FoodItem>

    private lateinit var foodInfoLayout: LinearLayout
    private lateinit var textFoodName: TextView
    private lateinit var textCalories: TextView
    private lateinit var btnCancel: Button
    private lateinit var btnSave: Button

    data class FoodItem(val name: String, val calories: Double)

    companion object {
        const val CAMERA_PERMISSION_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize Food Info Panel
        foodInfoLayout = binding.root.findViewById(R.id.foodInfoLayout)
        textFoodName = binding.root.findViewById(R.id.textFoodName)
        textCalories = binding.root.findViewById(R.id.textCalories)
        btnCancel = binding.root.findViewById(R.id.btnCancel)
        btnSave = binding.root.findViewById(R.id.btnSave)

        foodInfoLayout.visibility = View.GONE

        btnCancel.setOnClickListener {
            foodInfoLayout.visibility = View.GONE
        }

        btnSave.setOnClickListener {
            val foodName = textFoodName.text.toString()
            val calories = textCalories.text.toString().removePrefix("Calories: ").toDoubleOrNull() ?: 0.0
            saveFoodToTodoList(foodName, calories)
            foodInfoLayout.visibility = View.GONE
        }

        lifecycleScope.launch(Dispatchers.IO) {
            foodList = readCsvFromAssets(requireContext())

            launch(Dispatchers.Main) {
                checkCameraPermission()
            }
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            startCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readCsvFromAssets(context: Context): List<FoodItem> {
        val list = mutableListOf<FoodItem>()
        val inputStream = context.assets.open("calories.csv")
        val reader = BufferedReader(InputStreamReader(inputStream))

        reader.readLine() // skip header
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val parts = line!!.split("\t") // Adjust tab or comma based on your CSV
            if (parts.size >= 4) {
                val name = parts[1].trim()
                val caloriesStr = parts[3].trim()
                val calories = caloriesStr.replace(" cal", "").toDoubleOrNull() ?: 0.0
                list.add(FoodItem(name, calories))
            }
        }
        return list
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processImage(imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val bitmap = binding.previewView.bitmap ?: run {
                imageProxy.close()
                return
            }

            val classifier = FoodClassifier(requireContext())

            val results = classifier.classify(bitmap)

            results?.let { classifications ->
                if (classifications.isNotEmpty()) {
                    val topClasses = classifications[0].categories
                    val detectedWords = topClasses.map { it.label.lowercase() }
                    Log.d("TFLITE_RESULTS", "Detected: ${detectedWords.joinToString()}")

                    val match = foodList.firstOrNull { item ->
                        detectedWords.any { label ->
                            item.name.lowercase().contains(label) || label.contains(item.name.lowercase())
                        }
                    }

                    if (match != null) {
                        Log.d("FOOD_MATCH", "${match.name} - ${match.calories} kcal")
                        showDetectedFood(match.name, match.calories)
                    } else {
                        Log.d("NO_MATCH", "Detected: ${detectedWords.joinToString()}, not found in CSV")
                    }
                }
            }

            imageProxy.close()
        } else {
            imageProxy.close()
        }
    }

    private fun showDetectedFood(name: String, calories: Double) {
        requireActivity().runOnUiThread {
            textFoodName.text = name
            textCalories.text = "Calories: $calories"
            foodInfoLayout.visibility = View.VISIBLE
        }
    }

    private fun saveFoodToTodoList(name: String, calories: Double) {
        Toast.makeText(requireContext(), "Saved: $name ($calories cal)", Toast.LENGTH_SHORT).show()
        // You can later send this to ViewModel or another Fragment
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
    }
}
