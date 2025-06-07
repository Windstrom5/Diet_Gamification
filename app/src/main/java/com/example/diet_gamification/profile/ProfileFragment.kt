package com.example.diet_gamifikasi.profile

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.diet_gamification.R
import com.example.diet_gamification.databinding.FragmentProfileBinding
import com.example.diet_gamification.model.AccountModel
import com.example.diet_gamification.profile.UserViewModel
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.diet_gamification.firebase.AccountFirebaseRepository
import com.example.diet_gamification.room.AccountItemEntity
import com.example.diet_gamification.room.AppDatabase
import com.example.diet_gamification.shop.ShopAdapter
import com.example.diet_gamification.shop.ShopItem
import com.example.diet_gamification.shop.ShopRepository
import com.example.diet_gamification.utils.ApiService
import com.example.diet_gamifikasi.MainActivity
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.w3c.dom.Text
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest

class ProfileFragment : Fragment() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var cardView: CardView
    private lateinit var edit: Button
    private lateinit var  logout: Button
    private lateinit var cvshop: CardView
    private lateinit var xp: TextView
    private lateinit var userViewModel: UserViewModel
    private lateinit var auth: FirebaseAuth
    private lateinit var name: TextView
    private var accountModel: AccountModel? = null
    private lateinit var username: TextView
    private lateinit var weightuser: TextView
    private lateinit var heightuser: TextView
    val mainActivity = context as? MainActivity
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        drawerLayout = binding.drawerLayout
        navView = binding.navView
        auth = FirebaseAuth.getInstance()
        cardView = binding.Cardview
        edit = binding.edit
        xp = binding.tvexp
        name = binding.Name
        logout = binding.logout
        cvshop = binding.Cardviewshop
        username = binding.nameuser
        weightuser = binding.beratuser
        heightuser = binding.tinggiuser
        getAccountFromActivity()
//       getAccountFromBundle()  Nanti Aktifin klo udah bisa login pake firebase
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]
        setupDrawerNavigation()
//        observeUserData()
//        setupLogoutButton()
        edit.setOnClickListener {
            if(accountModel != null){
                showSettingDialog(requireContext(),accountModel,)
            }else{
                showLoginDialog(requireContext())
            }
        }
        cvshop.setOnClickListener {
            showShopDialog()
        }
        return binding.root
    }
    private fun showPickImageDialog(
        context: Context,
        accountModel: AccountModel?,
        onImageSelected: (String) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.imagedialog, null)

        val image1 = view.findViewById<CircleImageView>(R.id.image1)
        val image2 = view.findViewById<CircleImageView>(R.id.image2)
        val image3 = view.findViewById<CircleImageView>(R.id.image3)

        val lock1 = view.findViewById<ImageView>(R.id.lock1)
        val lock2 = view.findViewById<ImageView>(R.id.lock2)
        val lock3 = view.findViewById<ImageView>(R.id.lock3)

        val unlocked = ShopRepository.getUnlockedItems(accountModel?.inventory)

        fun setupImage(id: String, image: CircleImageView, lock: ImageView, drawableName: String) {
            if (unlocked.contains(id)) {
                lock.visibility = View.GONE

                // 👉 Only call onImageSelected when the user clicks the image
                image.setOnClickListener {
                    onImageSelected(drawableName)
                }
            } else {
                lock.visibility = View.VISIBLE

                // Optional: prevent interaction with locked images
                image.setOnClickListener(null)

                // Optional: grey out locked images
                val matrix = ColorMatrix().apply { setSaturation(0f) }
                image.colorFilter = ColorMatrixColorFilter(matrix)
            }
        }

        setupImage("PP-1", image1, lock1, "isla")
        setupImage("PP-2", image2, lock2, "sigma")
        setupImage("PP-3", image3, lock3, "plank")

        AlertDialog.Builder(context)
            .setView(view)
            .setTitle("Choose Profile Picture")
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showShopDialog() {
        // Inflate your “shop” dialog layout
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.fragment_shop, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val btnClose    = dialogView.findViewById<Button>(R.id.btnCloseShop)

        // Build the parent AlertDialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // 1) Get your shop items
        val shopItems = ShopRepository.shopItems

        // 2) Wire up the adapter
        val adapter = ShopAdapter(shopItems,accountModel?.inventory) { item ->
            // Parse price
            val price = item.price.toIntOrNull() ?: 0
            // Read user XP from your accountModel
            val userXp = accountModel?.exp ?: 0

            if (userXp < price) {
                // Not enough XP → show a warning
                AlertDialog.Builder(requireContext())
                    .setTitle("Not enough exp")
                    .setMessage("You need $price exp to buy “${item.nama}”, but you only have $userXp exp.")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://selected-jaguar-presently.ngrok-free.app") // Replace with actual base URL
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(ApiService::class.java)
                AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Purchase")
                    .setMessage("Spend $price exp to buy “${item.nama}”?")
                    .setPositiveButton("Buy") { _, _ ->
                        accountModel?.exp = userXp - price
                        val currentInventory = accountModel?.inventory ?: ""
                        val itemId = item.id
                        val updatedInventory = if (currentInventory.isBlank()) {
                            itemId
                        } else {
                            "$currentInventory,$itemId"
                        }
                        accountModel?.inventory = updatedInventory
                        val newXp = userXp - price
                        val newInventory = if ((accountModel?.inventory ?: "").isBlank()) {
                            item.id
                        } else {
                            "${accountModel?.inventory},${item.id}"
                        }

                        val updateRequest = mapOf(
                            "exp" to newXp,
                            "inventory" to newInventory
                        )

                        val accountId = accountModel?.id ?: return@setPositiveButton

                        lifecycleScope.launch {
                            val response = api.updateAccount(accountId, updateRequest)
                            if (response.isSuccessful) {
                                val updated = response.body()
                                accountModel?.exp = (updated?.get("exp") as? Double)?.toInt() ?: newXp
                                accountModel?.inventory = updated?.get("inventory") as? String ?: newInventory

                                val mainActivity = activity as? MainActivity
                                mainActivity?.currentAccountModel = accountModel
                                mainActivity?.updateUsername()
                                mainActivity?.hideLoadingDialog()
                                openFragment(ProfileFragment())
                                dialog.dismiss()
                            } else {
                                Toast.makeText(requireContext(), "Failed to update account", Toast.LENGTH_SHORT).show()
                            }
                        }

                        val mainActivity = activity as? MainActivity
                        mainActivity?.currentAccountModel = accountModel
                        mainActivity?.updateUsername()

                        val profileFragment = ProfileFragment()
                        openFragment(profileFragment)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter       = adapter

        // Close button just dismisses the shop dialog
        btnClose.setOnClickListener { dialog.dismiss() }

        // Optional: make the dialog’s background transparent
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    // Somewhere in your Fragment/Activity, update the on‑screen XP counter:
    private fun updatexpDisplay(newXp: Int) {
        accountModel?.exp = newXp
    }
    private fun showSettingDialog(context: Context, accountModel: AccountModel?) {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_settings, null)

        val etName = view.findViewById<TextInputEditText>(R.id.etName)
        val etWeight = view.findViewById<TextInputEditText>(R.id.etWeight)
        val etHeight = view.findViewById<TextInputEditText>(R.id.etHeight)
        val etFont = view.findViewById<AutoCompleteTextView>(R.id.etFont)
        val etTitle = view.findViewById<AutoCompleteTextView>(R.id.etTitle)
        val etgender = view.findViewById<AutoCompleteTextView>(R.id.etgender)
        val profile = view.findViewById<CircleImageView>(R.id.circleImageView)
        // Prefill existing data
        val genderOptions = listOf("Male", "Female")
        val genderAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, genderOptions)
        etgender.setAdapter(genderAdapter)
        etgender.setText(accountModel?.gender, false)
        etName.setText(accountModel?.name)
        etWeight.setText(accountModel?.berat?.toString())
        etHeight.setText(accountModel?.tinggi?.toString())
//        etFont.setText(accountModel?.setting) // if storing font in setting
//        etTitle.setText(accountModel?.se) // if using gender as a title input here
        val unlocked = ShopRepository.getUnlockedItems(accountModel?.inventory)

// Filter fonts from unlocked inventory
        val unlockedFonts = ShopRepository.shopItems
            .filter { it.id.startsWith("FT-") && unlocked.contains(it.id) }
            .map { it.nama }

// Filter titles from unlocked inventory
        val unlockedTitles = ShopRepository.shopItems
            .filter { it.id.startsWith("TL-") && unlocked.contains(it.id) }
            .map { it.nama }

// Apply font dropdown
        val fontAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, unlockedFonts)
        etFont.setAdapter(fontAdapter)

// Apply title dropdown
        val titleAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, unlockedTitles)
        etTitle.setAdapter(titleAdapter)
        val selectedFont = ShopRepository.shopItems.find { it.id == accountModel?.setting && it.id.startsWith("FT-") }?.nama
        val selectedTitle = ShopRepository.shopItems.find { it.id == accountModel?.gender && it.id.startsWith("TL-") }?.nama

        etFont.setText(selectedFont ?: "", false)
        etTitle.setText(selectedTitle ?: "", false)
        var selectedProfile = accountModel?.setting?.takeIf { it.startsWith("PP-") }
        val imagePath = ShopRepository.shopItems.find { it.id == selectedProfile }?.dirimag
        imagePath?.let {
            val resId = context.resources.getIdentifier(it, "drawable", context.packageName)
            if (resId != 0) profile.setImageResource(resId)
        }
        profile.setOnClickListener {
            showPickImageDialog(context, accountModel) { selectedDrawableName ->
                val selectedItem = ShopRepository.shopItems.find { it.dirimag == selectedDrawableName && it.id.startsWith("PP-") }
                selectedItem?.let {
                    val resId = context.resources.getIdentifier(it.dirimag, "drawable", context.packageName)
                    if (resId != 0) profile.setImageResource(resId)
                    selectedProfile = it.id // save selected profile ID
                }
            }
        }
        AlertDialog.Builder(context)
            .setView(view)
            .setTitle("Edit Settings")
            .setPositiveButton("Save") { _, _ ->
                val selectedFontName = etFont.text.toString()
                val selectedFontId = ShopRepository.shopItems.find { it.nama == selectedFontName && it.id.startsWith("FT-") }?.id

                val selectedTitleName = etTitle.text.toString()
                val selectedTitleId = ShopRepository.shopItems.find { it.nama == selectedTitleName && it.id.startsWith("TL-") }?.id

                val allSelected = listOfNotNull(selectedProfile, selectedFontId, selectedTitleId)
                val newSetting = allSelected.joinToString(",")

                // Create or update the account model
                var accountModel = accountModel?.apply {
                    name = etName.text.toString()
                    berat = etWeight.text.toString().toIntOrNull() ?: berat
                    tinggi = etHeight.text.toString().toIntOrNull() ?: tinggi
                    gender = etgender.text.toString()
                    setting = newSetting
                }

                val mainActivity = context as? MainActivity
                mainActivity?.currentAccountModel = accountModel
                mainActivity?.updateUsername()
                val profileFragment = ProfileFragment()
                openFragment(profileFragment)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRegisterDialog(context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.fragment_register, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val registerButton = dialogView.findViewById<Button>(R.id.btnRegister)
        val loginText = dialogView.findViewById<TextView>(R.id.tvLogin)
        val nameEditText = dialogView.findViewById<EditText>(R.id.etFullName)
        val emailEditText = dialogView.findViewById<EditText>(R.id.etEmail)
        val passwordEditText = dialogView.findViewById<EditText>(R.id.etPassword)
        val confirmPasswordEditText = dialogView.findViewById<EditText>(R.id.etConfirmPassword)
        val beratEditText = dialogView.findViewById<EditText>(R.id.etWeight)
        val genderDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.etgender)
        val tinggiEditText = dialogView.findViewById<EditText>(R.id.etHeight)

        val genderOptions = listOf("Male", "Female", "Other")
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, genderOptions)
        genderDropdown.setAdapter(adapter)

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()
            val berat = beratEditText.text.toString().trim()
            val gender = genderDropdown.text.toString().trim()
            val tinggi = tinggiEditText.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || berat.isEmpty() || gender.isEmpty() || tinggi.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                // Build AccountModel object
                val accountModel = AccountModel(
                    id = 0,
                    email = email,
                    name = name,
                    password = password,
                    gender = gender,     // ✅ lowercase
                    exp = 0,             // ✅ lowercase
                    berat = berat.toIntOrNull() ?: 0,
                    tinggi = tinggi.toIntOrNull() ?: 0,
                    inventory = "",
                    setting = "",
                    is_verify = false,
                    created_at = "",
                    updated_at = ""
                )


                // Retrofit setup
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://selected-jaguar-presently.ngrok-free.app")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(ApiService::class.java)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = api.register(accountModel)
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                val body = response.body()
                                if (body != null) {
                                    // 'body' is Map<String, Any>, so to access 'success' or 'message':
                                    val success = body["success"] as? Boolean ?: false
                                    val message = body["message"] as? String ?: "No message"

                                    if (success) {
                                        Toast.makeText(context, "Registration successful: $message", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Registration failed: $message", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Response body is null", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // response.errorBody() contains Laravel validation errors in JSON
                                val errorJson = response.errorBody()?.string()
                                Log.e("RegisterError", "Raw error response: $errorJson")  // <-- log raw error
                                if (errorJson != null) {
                                    try {
                                        val jsonObj = JSONObject(errorJson)
                                        val errors = jsonObj.optJSONObject("errors")
                                        val emailErrors = errors?.optJSONArray("email")
                                        val errorMessage = emailErrors?.getString(0) ?: "Registration failed"
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                    } catch (e: JSONException) {
                                        Toast.makeText(context, "Unknown error occurred", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Unknown error occurred", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        loginText.setOnClickListener {
            dialog.dismiss()
            showLoginDialog(context)
        }

        dialog.show()
    }



    private fun hashPassword(password: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(password.toByteArray())

        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun showLoginDialog(context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.fragment_login, null)

        val emailEditText = dialogView.findViewById<EditText>(R.id.etEmail)
        val passwordEditText = dialogView.findViewById<EditText>(R.id.etPassword)
        val loginButton = dialogView.findViewById<Button>(R.id.btnLogin)
        val registerText = dialogView.findViewById<TextView>(R.id.tvRegister)
        val forgotPasswordText = dialogView.findViewById<TextView>(R.id.tvForgotPassword)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://selected-jaguar-presently.ngrok-free.app")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)

        loginButton.setOnClickListener {
            mainActivity?.showLoadingDialog()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                mainActivity?.hideLoadingDialog()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val loginRequest = mapOf(
                        "email" to email,
                        "password" to password
                    )
                    val response = api.login(loginRequest)

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            val body = response.body()
                            if (body != null && body.containsKey("account")) {
                                val accountMap = body["account"] as Map<*, *>

                                val account = AccountModel(
                                    id = (accountMap["id"] as Double).toInt(),  // Retrofit converts JSON numbers to Double
                                    email = accountMap["email"] as String,
                                    name = accountMap["name"] as String,
                                    password = accountMap["password"] as String,
                                    gender = accountMap["gender"] as String,
                                    exp = (accountMap["exp"] as Double).toInt(),
                                    berat = (accountMap["berat"] as Double).toInt(),
                                    tinggi = (accountMap["tinggi"] as Double).toInt(),
                                    inventory = accountMap["inventory"] as? String,
                                    setting = accountMap["setting"] as? String,
                                    is_verify = accountMap["is_verify"] as Boolean,
                                    created_at = accountMap["created_at"] as String,
                                    updated_at = accountMap["updated_at"] as String
                                )

                                Toast.makeText(context, "Login Successful", Toast.LENGTH_SHORT).show()
                                mainActivity?.currentAccountModel = account
                                mainActivity?.updateUsername()
                                mainActivity?.openFragment(ProfileFragment())
                                mainActivity?.hideLoadingDialog()
                                dialog.dismiss()
                            } else {
                                Toast.makeText(context, "Login failed: Invalid response", Toast.LENGTH_SHORT).show()
                                mainActivity?.hideLoadingDialog()
                            }
                        } else {
                            val errorJson = response.errorBody()?.string()
                            val errorMessage = parseLaravelError(errorJson)
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            mainActivity?.hideLoadingDialog()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Login error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        mainActivity?.hideLoadingDialog()
                    }
                }
            }
        }

        registerText.setOnClickListener {
            dialog.dismiss()
            showRegisterDialog(context)
        }

        forgotPasswordText.setOnClickListener {
            Toast.makeText(context, "Handle Forgot Password", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun mapToAccountModel(data: Map<String, Any>): AccountModel {
        return AccountModel(
            id = (data["id"] as? Number)?.toInt() ?: 0,
            email = data["email"] as? String ?: "",
            name = data["name"] as? String ?: "",
            password = data["password"] as? String ?: "",
            gender = data["gender"] as? String ?: "",      // FIXED
            exp = (data["exp"] as? Number)?.toInt() ?: 0,  // FIXED
            berat = (data["berat"] as? Number)?.toInt() ?: 0,
            tinggi = (data["tinggi"] as? Number)?.toInt() ?: 0,
            inventory = data["inventory"] as? String,
            setting = data["setting"] as? String
        )
    }

    private fun parseLaravelError(errorJson: String?): String {
        if (errorJson == null) return "Unknown error occurred"
        return try {
            val jsonObj = JSONObject(errorJson)
            val errors = jsonObj.optJSONObject("errors")
            if (errors != null && errors.length() > 0) {
                val firstKey = errors.keys().next()
                val errorArray = errors.optJSONArray(firstKey)
                errorArray?.getString(0) ?: "Validation error"
            } else {
                jsonObj.optString("message", "Error occurred")
            }
        } catch (e: JSONException) {
            "Error parsing server response"
        }
    }


    // Extension function to map AccountItemEntity to AccountModel
    private fun AccountItemEntity.toAccountModel(): AccountModel {
        return AccountModel(
            id = this.id,
            email = this.email,
            name = this.name,
            password = this.password,
            gender = this.gender,
            exp = this.exp,
            berat = this.berat,
            tinggi = this.tinggi,
            inventory = this.inventory,
            setting = this.setting
        )
    }

    // Inside ProfileFragment
    private fun openFragment(fragment: Fragment) {
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null) // Optional, if you want to keep the back stack
        transaction.commit()
    }


    private fun showEditPasswordDialog() {
        val context = requireContext()

        // Create TextInputLayout
        val tilPassword = TextInputLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isHintEnabled = true
            hint = "Password"
            setPadding(24, 24, 24, 0)
            setBoxBackgroundColorResource(android.R.color.transparent)
            endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        }

        // Create TextInputEditText inside it
        val etPassword = TextInputEditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setHintTextColor(Color.parseColor("#AAAAAA"))
            setTextColor(Color.parseColor("#FFFFFF"))
            backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            setPadding(60, 40, 60, 40)
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock, 0, 0, 0)
            compoundDrawablePadding = 20
            hint = "Password"
        }

        tilPassword.addView(etPassword)

        // Build AlertDialog
        AlertDialog.Builder(context)
            .setTitle("Edit Password")
            .setView(tilPassword)
            .setPositiveButton("Save") { _, _ ->
                val newPassword = etPassword.text.toString()
                if (newPassword.isNotEmpty()) {
                    // Do your password save logic here
                    Toast.makeText(context, "Password updated!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun showLoggedInState() {
        cardView.visibility=View.VISIBLE
        logout.visibility= View.VISIBLE
        name.setText(accountModel?.name)
        edit.text = "edit"
        xp.setText(accountModel?.exp.toString() + " XP")
        weightuser.setText(accountModel?.berat.toString() + " KG")
        username.setText(accountModel?.name)
        heightuser.setText(accountModel?.tinggi.toString() + " CM")
        val mainActivity = activity as? MainActivity
        mainActivity?.applyFontIfAvailable(requireContext(), mainActivity.currentAccountModel?.setting, binding.root)
    }

    private fun showLoggedOutState() {
        cardView.visibility=View.GONE
        logout.visibility= View.GONE
        edit.text = "login"
        name.text = "Welcome Guest"
        logout.visibility=View.GONE
        xp.visibility=View.GONE
    }
    private fun getAccountFromActivity() {
        val mainActivity = activity as? MainActivity
        accountModel = mainActivity?.currentAccountModel

        if (accountModel != null) {
            Log.d("ProfileFragment2", "Received Account: ${accountModel!!.name}")
            showLoggedInState()

        } else {
            Log.e("ProfileFragment2", "AccountModel is null in MainActivity")
            showLoggedOutState()
        }
    }


    private fun setupDrawerNavigation() {
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_login -> {
                    findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
                }
                R.id.nav_bmi_calculator -> {
                    findNavController().navigate(R.id.action_profileFragment_to_bmiCalculatorFragment)
                }
                R.id.nav_shop -> {
                    findNavController().navigate(R.id.action_profileFragment_to_shopFragment)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun observeUserData() {
        userViewModel.username.observe(viewLifecycleOwner) { name ->
            binding.Name.text = name
        }

        userViewModel.exp.observe(viewLifecycleOwner) { exp ->
            binding.tvexp.text = "exp: $exp"
        }
    }

    private fun setupLogoutButton() {
        binding.logout.setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
