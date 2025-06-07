package com.example.diet_gamification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var foodInfoLayout: LinearLayout
    private lateinit var textFoodName: TextView
    private lateinit var textCalories: TextView
    private lateinit var btnCancel: Button
    private lateinit var btnSave: Button

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

        checkCameraPermission()
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
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
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

    private fun processImage(imageProxy: ImageProxy) {
        val bitmap = binding.previewView.bitmap ?: run {
            imageProxy.close()
            return
        }

        imageProxy.close() // Close immediately to avoid blocking

        lifecycleScope.launch(Dispatchers.IO) {
            sendBitmapToServer(bitmap)
        }
    }

    private suspend fun sendBitmapToServer(bitmap: Bitmap) {
        try {
            // Convert bitmap to JPEG bytes
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val byteArray = stream.toByteArray()

            val requestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("image", "image.jpg", requestBody)

            val retrofit = Retrofit.Builder()
                .baseUrl("https://selected-jaguar-presently.ngrok-free.app") // Your Laravel backend URL
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val api = retrofit.create(ApiService::class.java)
            val response = api.uploadImageForCalories(multipartBody)

            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    val result = response.body()
                    val food = result?.get("food") as? String ?: "Unknown"
                    val calories = (result?.get("calories") as? Double) ?: 0.0
                    showDetectedFood(food, calories)
                } else {
                    Toast.makeText(requireContext(), "Prediction failed", Toast.LENGTH_SHORT).show()
                    Log.e("CameraFragment", "Error response: ${response.errorBody()?.string()}")
                }
            }
        } catch (e: Exception) {
            Log.e("CameraFragment", "Upload error", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
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
        // TODO: Save or pass data to ViewModel or other fragment
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
    }

    interface ApiService {
        @Multipart
        @POST("/api/calories/check")
        suspend fun uploadImageForCalories(
            @Part image: MultipartBody.Part
        ): retrofit2.Response<Map<String, Any>>
    }
}
