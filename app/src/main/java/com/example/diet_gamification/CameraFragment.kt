package com.example.diet_gamification

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import com.example.diet_gamification.utils.ApiService
import com.example.diet_gamifikasi.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalTime
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
    private var mainActivity: MainActivity? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = activity as? MainActivity
    }
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
//            saveFoodToTodoList(foodName, calories)
            foodInfoLayout.visibility = View.GONE
        }

        // Handle capture button
        binding.btnCapture.setOnClickListener {
            mainActivity?.showLoadingDialog()
            captureImageAndSend()
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

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview
            )

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun captureImageAndSend() {
        val bitmap = binding.previewView.bitmap
        if (bitmap != null) {
            // Disable button immediately on main thread
            binding.btnCapture.isEnabled = false
            Toast.makeText(requireContext(), "Analyzing image...", Toast.LENGTH_SHORT).show()

            lifecycleScope.launch(Dispatchers.IO) {
                sendBitmapToServer(bitmap)
            }
        } else {
            Toast.makeText(requireContext(), "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun sendBitmapToServer(bitmap: Bitmap) {
        try {
            // Compress bitmap to JPEG byte array
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val byteArray = stream.toByteArray()

            // Prepare multipart body for image upload
            val requestBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData(
                "image", "image.jpg", requestBody
            )

            // Build Retrofit instance
            val retrofit = Retrofit.Builder()
                .baseUrl("http://192.168.1.4:8000/") // replace with your real backend IP/domain
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val api = retrofit.create(ApiService::class.java)

            val response = api.checkCalories(multipartBody)

            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body is List<*>) {
                        val list = body.filterIsInstance<Map<String, Any>>() // safely cast

                        if (list.isNotEmpty()) {
                            val topResult = list.maxByOrNull { (it["confidence"] as? Double) ?: 0.0 }

                            if (topResult != null) {
                                val label = topResult["label"] as? String ?: "Unknown"
                                val nutrition = topResult["nutrition"] as? Map<*, *>
                                val calories = (nutrition?.get("calories") as? Number)?.toInt() ?: -1

                                Log.d("CameraFragment", "Detected: $label ($calories kcal)")

                                mainActivity?.hideLoadingDialog()
                                showFoodDialog(label, calories)
                            } else {
                                Log.w("CameraFragment", "No top result found")
                                Toast.makeText(requireContext(), "No valid result found", Toast.LENGTH_SHORT).show()
                                mainActivity?.hideLoadingDialog()
                            }
                        } else {
                            Log.w("CameraFragment", "Empty response list from server")
                            Toast.makeText(requireContext(), "Empty response from server", Toast.LENGTH_SHORT).show()
                            mainActivity?.hideLoadingDialog()
                        }
                    } else {
                        Log.e("CameraFragment", "Unexpected response body: $body")
                        Toast.makeText(requireContext(), "Invalid response format", Toast.LENGTH_SHORT).show()
                        mainActivity?.hideLoadingDialog()
                    }
                } else {
                    Log.e("CameraFragment", "Unsuccessful response: code=${response.code()}, error=${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Server error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    mainActivity?.hideLoadingDialog()
                }

                binding.btnCapture.isEnabled = true
            }
        } catch (e: Exception) {
            Log.e("CameraFragment", "Exception during upload", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Exception: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                binding.btnCapture.isEnabled = true
                mainActivity?.hideLoadingDialog()
            }
        }
    }

    private fun showFoodDialog(foodName: String, calories: Int) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_food_detected, null)

        val tvFood = dialogView.findViewById<TextView>(R.id.tvDetectedFood)
        val tvCalories = dialogView.findViewById<TextView>(R.id.tvDetectedCalories)
        val etCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.etCategory)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        tvFood.text = "🍽 Food: $foodName"
        tvCalories.text = "🔥 Calories: $calories kcal"

        val categories = listOf("Breakfast", "Lunch", "Dinner")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        etCategory.setAdapter(adapter)
        val gifView = dialogView.findViewById<GifImageView>(R.id.ivGif)
        val gifDrawable = GifDrawable(resources, R.drawable.eat)
        gifView.setImageResource(R.drawable.eat)
        gifDrawable.start() // Optional: triggers the animation
        val hour = LocalTime.now().hour
        etCategory.setText(
            when {
                hour in 5..10 -> "Breakfast"
                hour in 11..15 -> "Lunch"
                else -> "Dinner"
            }, false
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()


        btnSave.setOnClickListener {
            val selectedCategory = etCategory.text.toString()
            val requestData = mapOf<String, Any?>(
                "id_account" to mainActivity?.currentAccountModel?.id,
                "category" to selectedCategory,
                "name" to foodName,
                "calories" to calories,
                "date" to LocalDate.now().toString()
            ).filterValues { it != null } as Map<String, Any>

            val retrofit = Retrofit.Builder()
                .baseUrl("http://192.168.1.4:8000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val api = retrofit.create(ApiService::class.java)

            lifecycleScope.launch {
                try {
                    val response = api.createCalorie(requestData)
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "✅ Saved!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(requireContext(), "❌ Failed to save", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "⚠ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }




    private fun showDetectedFood(name: String, calories: Double) {
        textFoodName.text = name
        textCalories.text = "Calories: $calories"
        foodInfoLayout.visibility = View.VISIBLE
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
}

